package com.ai.assistance.operit.core.vibecoding.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubCredentialStoreTest {

    @Test
    fun `envelope json round trip preserves all fields`() {
        val envelope =
            GitHubCredentialStore.Envelope(
                version = 1,
                ivBase64 = "aGVsbG8=",
                cipherTextBase64 = "c2VjcmV0",
                accountName = "Black0Bag",
            )
        val restored = GitHubCredentialStore.Envelope.fromJson(envelope.toJson())
        assertEquals(1, restored.version)
        assertEquals("aGVsbG8=", restored.ivBase64)
        assertEquals("c2VjcmV0", restored.cipherTextBase64)
        assertEquals("Black0Bag", restored.accountName)
        assertEquals(envelope, restored)
    }

    @Test
    fun `envelope json escapes quotes in account name`() {
        val envelope =
            GitHubCredentialStore.Envelope(
                version = 1,
                ivBase64 = "aXY=",
                cipherTextBase64 = "Y3Q=",
                accountName = "say \"hi\"",
            )
        val restored = GitHubCredentialStore.Envelope.fromJson(envelope.toJson())
        assertEquals("say \"hi\"", restored.accountName)
    }

    @Test
    fun `invalid envelope json is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            GitHubCredentialStore.Envelope.fromJson("not-a-json")
        }
    }

    @Test
    fun `envelope never contains plaintext token`() {
        val envelope =
            GitHubCredentialStore.Envelope(
                version = 1,
                ivBase64 = "aXY=",
                cipherTextBase64 = "ZW5jcnlwdGVk",
                accountName = "Black0Bag",
            )
        val json = envelope.toJson()
        // 只有密文/IV/account，绝不出现明文 token 内容
        assertTrue(!json.contains("ghp_"))
        assertTrue(!json.contains("token"))
        assertTrue(!json.contains("c2VjcmV0"))
    }
}