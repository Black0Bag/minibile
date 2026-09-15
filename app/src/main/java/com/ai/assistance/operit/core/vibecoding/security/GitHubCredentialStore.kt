package com.ai.assistance.operit.core.vibecoding.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * GitHub 凭据安全存储（Phase F）。
 *
 * 安全模型（对齐 03A 计划 §七 与 Android 官方 Keystore 指南）：
 * - 主密钥由 Android Keystore 生成，AES-256-GCM，密钥材料不可导出；
 * - 凭据（Fine-grained PAT）用信封加密：密文 + IV + 版本存 app-private SharedPreferences；
 * - 不复用已废弃的 [androidx.security.crypto.EncryptedSharedPreferences]；
 * - Token 永不进入日志、证据、TODO、Prompt 或 Git。
 *
 * 本类依赖 Android API，纯逻辑（编解码）拆分到 companion 方法以便 JVM 单测。
 */
class GitHubCredentialStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /** 保存凭据：信封加密后写入 SharedPreferences。 */
    fun saveCredential(token: String, accountName: String): Boolean {
        require(token.isNotBlank()) { "GitHub token cannot be blank" }
        require(accountName.isNotBlank()) { "Account name cannot be blank" }
        return runCatching {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            val envelope = Envelope(
                version = 1,
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
                cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP),
                accountName = accountName,
            )
            prefs.edit()
                .putString(KEY_ENVELOPE, envelope.toJson())
                .apply()
            true
        }.getOrElse {
            false
        }
    }

    /** 读取凭据：解密信封；缺失或解密失败返回 null（绝不回显 token 到日志）。 */
    fun readCredential(): String? {
        val envelopeJson = prefs.getString(KEY_ENVELOPE, null) ?: return null
        return runCatching {
            val envelope = Envelope.fromJson(envelopeJson)
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(128, Base64.decode(envelope.ivBase64, Base64.NO_WRAP)),
            )
            val plain =
                cipher.doFinal(Base64.decode(envelope.cipherTextBase64, Base64.NO_WRAP))
            String(plain, Charsets.UTF_8)
        }.getOrNull()
    }

    fun clearCredential() {
        prefs.edit().remove(KEY_ENVELOPE).apply()
    }

    /** 是否已保存凭据（不泄露内容）。 */
    fun hasCredential(): Boolean = prefs.contains(KEY_ENVELOPE)

    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        generator.init(spec)
        return generator.generateKey()
    }

    /** 信封模型：与 Android API 隔离，可 JVM 单测编解码逻辑。 */
    data class Envelope(
        val version: Int,
        val ivBase64: String,
        val cipherTextBase64: String,
        val accountName: String,
    ) {
        fun toJson(): String =
            "{\"version\":$version," +
                "\"iv\":\"$ivBase64\"," +
                "\"ct\":\"$cipherTextBase64\"," +
                "\"account\":\"${accountName.replace("\"", "\\\"")}\"}"

        companion object {
            fun fromJson(json: String): Envelope {
                val regex = Regex("\"version\":(\\d+),\"iv\":\"([^\"]*)\",\"ct\":\"([^\"]*)\",\"account\":\"([^\"]*)\"")
                val match = regex.find(json) ?: throw IllegalArgumentException("Invalid envelope")
                return Envelope(
                    version = match.groupValues[1].toInt(),
                    ivBase64 = match.groupValues[2],
                    cipherTextBase64 = match.groupValues[3],
                    accountName = match.groupValues[4],
                )
            }
        }
    }

    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "vibecoding_github_master_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS_NAME = "vibecoding_github_credentials"
        private const val KEY_ENVELOPE = "credential_envelope"
    }
}