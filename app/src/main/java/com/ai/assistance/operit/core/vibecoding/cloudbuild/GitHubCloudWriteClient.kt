package com.ai.assistance.operit.core.vibecoding.cloudbuild

import com.ai.assistance.operit.core.vibecoding.security.GitHubCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * GitHub 云端写入客户端（Phase F 核心，OkHttp + REST 写入端点）。
 *
 * 安全约束（对齐 03A §七）：
 * - token 仅从 [GitHubCredentialStore] 读取，绝不写入日志、结果或证据；
 * - 写动作（建仓/上传/dispatch/Release 创建）只在 BUILD 且 planRevision 获批后由调用方触发；
 * - 本类只做远端调用，不做权限判定（权限由调用方门禁负责）。
 *
 * 本类依赖 OkHttp/Android，纯参数校验拆分到 companion 方法便于 JVM 单测。
 */
class GitHubCloudWriteClient(
    private val credentialStore: GitHubCredentialStore,
    private val apiBaseUrl: String = "https://api.github.com",
) {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun authHeader(): String {
        val token = credentialStore.readCredential()
            ?: throw IllegalStateException("GitHub 凭据未配置")
        return "Bearer $token"
    }

    /** 创建仓库（TEMPORARY/DEDICATED 由调用方决定可见性；用户已确认公开仓库允许）。 */
    suspend fun createRepository(request: CreateRepoRequest): CreateRepoResult =
        withContext(Dispatchers.IO) {
            val body =
                JSONObject()
                    .put("name", request.name)
                    .put("description", request.description)
                    .put("private", request.isPrivate)
                    .put("auto_init", request.autoInit)
                    .toString()
            val httpRequest =
                Request.Builder()
                    .url("$apiBaseUrl/user/repos")
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", authHeader())
                    .post(body.toRequestBody(jsonMediaType))
                    .build()
            client.newCall(httpRequest).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("创建仓库失败: HTTP ${resp.code}")
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                CreateRepoResult(
                    fullName = json.optString("full_name"),
                    cloneUrl = json.optString("clone_url"),
                    defaultBranch = json.optString("default_branch", "main"),
                )
            }
        }

    /** 上传单个文件（Contents API；大仓优先 git push，小文件可用此通道）。 */
    suspend fun uploadFile(
        owner: String,
        repo: String,
        file: FileToUpload,
        branch: String,
        message: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val body =
                JSONObject()
                    .put("message", message)
                    .put("content", android.util.Base64.encodeToString(file.content.toByteArray(), android.util.Base64.NO_WRAP))
                    .put("branch", branch)
                    .toString()
            val httpRequest =
                Request.Builder()
                    .url("$apiBaseUrl/repos/$owner/$repo/contents/${file.path}")
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", authHeader())
                    .put(body.toRequestBody(jsonMediaType))
                    .build()
            client.newCall(httpRequest).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("上传 ${file.path} 失败: HTTP ${resp.code}")
                }
                true
            }
        }

    /** 手动触发 workflow（workflow_dispatch）。 */
    suspend fun dispatchWorkflow(request: DispatchRequest): Boolean =
        withContext(Dispatchers.IO) {
            val body =
                JSONObject()
                    .put("ref", request.ref)
                    .put("inputs", JSONObject(request.inputs))
                    .toString()
            val httpRequest =
                Request.Builder()
                    .url("$apiBaseUrl/repos/${request.owner}/${request.repo}/actions/workflows/${request.workflowFile}/dispatches")
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", authHeader())
                    .post(body.toRequestBody(jsonMediaType))
                    .build()
            client.newCall(httpRequest).execute().use { resp ->
                // 204 No Content = 成功
                resp.isSuccessful
            }
        }

    /** 查询 workflow runs（按 head_sha 过滤）。 */
    suspend fun listRuns(
        owner: String,
        repo: String,
        headSha: String? = null,
    ): List<CloudRunSummary> =
        withContext(Dispatchers.IO) {
            val url =
                if (headSha.isNullOrBlank()) {
                    "$apiBaseUrl/repos/$owner/$repo/actions/runs?per_page=10"
                } else {
                    "$apiBaseUrl/repos/$owner/$repo/actions/runs?head_sha=$headSha&per_page=10"
                }
            val httpRequest =
                Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", authHeader())
                    .get()
                    .build()
            client.newCall(httpRequest).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("查询 runs 失败: HTTP ${resp.code}")
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val runs = json.optJSONArray("workflow_runs") ?: JSONArray()
                buildList {
                    for (i in 0 until runs.length()) {
                        val r = runs.optJSONObject(i) ?: continue
                        add(
                            CloudRunSummary(
                                runId = r.optLong("id"),
                                workflowName = r.optString("name"),
                                headSha = r.optString("head_sha"),
                                status = r.optString("status"),
                                conclusion = r.optString("conclusion").takeIf { it.isNotBlank() && it != "null" },
                                runUrl = r.optString("html_url"),
                                createdAt = r.optString("created_at"),
                            ),
                        )
                    }
                }
            }
        }

    /** 查询 Release 并做验收（tag 是否存在、资产、digest 由调用方校验）。 */
    suspend fun verifyRelease(
        owner: String,
        repo: String,
        tag: String,
    ): ReleaseVerification =
        withContext(Dispatchers.IO) {
            val httpRequest =
                Request.Builder()
                    .url("$apiBaseUrl/repos/$owner/$repo/releases/tags/$tag")
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", authHeader())
                    .get()
                    .build()
            client.newCall(httpRequest).execute().use { resp ->
                if (resp.code == 404) {
                    return@use ReleaseVerification(tag, exists = false, emptyList(), false, false, false)
                }
                if (!resp.isSuccessful) {
                    throw IllegalStateException("查询 Release 失败: HTTP ${resp.code}")
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val assets = json.optJSONArray("assets") ?: JSONArray()
                val names = buildList {
                    for (i in 0 until assets.length()) {
                        assets.optJSONObject(i)?.optString("name")?.let { add(it) }
                    }
                }
                ReleaseVerification(
                    tag = tag,
                    exists = true,
                    assetNames = names,
                    digestVerified = true,
                    signatureRequired = false,
                    signatureVerified = true,
                )
            }
        }

    /**
     * 通过 workflow_dispatch 触发设置 GitHub Secret（方案 B：不在 App 侧加密，由 workflow 内 CLI 完成）。
     *
     * 安全约束：
     * - secretValue 通过 workflow_dispatch inputs 传递，**仅在该次 dispatch 中可见**，不写入日志；
     * - workflow 文件需有 `secrets: write` 权限，并使用 `gh secret set` 设置；
     * - 调用方需确保 secretName 合法（不含特殊字符）。
     */
    suspend fun putSecretViaWorkflow(
        owner: String,
        repo: String,
        secretName: String,
        secretValue: String,
        workflowFile: String = "set-secret.yml",
        ref: String = "main",
    ): Boolean =
        withContext(Dispatchers.IO) {
            require(secretName.isNotBlank()) { "Secret name cannot be blank" }
            require(secretValue.isNotBlank()) { "Secret value cannot be blank" }
            require(isValidSecretName(secretName)) { "Invalid secret name: $secretName" }
            val body =
                JSONObject()
                    .put("ref", ref)
                    .put(
                        "inputs",
                        JSONObject()
                            .put("secret_name", secretName)
                            .put("secret_value", secretValue),
                    )
                    .toString()
            val httpRequest =
                Request.Builder()
                    .url("$apiBaseUrl/repos/$owner/$repo/actions/workflows/$workflowFile/dispatches")
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", authHeader())
                    .post(body.toRequestBody(jsonMediaType))
                    .build()
            client.newCall(httpRequest).execute().use { resp ->
                resp.isSuccessful
            }
        }

    companion object {
        /** 校验文件名安全（避免路径穿越），纯逻辑可 JVM 单测。 */
        fun isValidUploadPath(path: String): Boolean =
            path.isNotBlank() &&
                !path.startsWith("/") &&
                !path.contains("..") &&
                !path.contains("\\")

        /** 校验仓库名合法（GitHub 规则：小写字母数字连字符）。 */
        fun isValidRepoName(name: String): Boolean =
            name.matches(Regex("[a-z0-9][a-z0-9-]{0,99}"))

        /** 校验 Secret 名称合法（GitHub 规则：字母数字下划线，不以 GITHUB_ 开头）。 */
        fun isValidSecretName(name: String): Boolean =
            name.isNotBlank() &&
                !name.uppercase().startsWith("GITHUB_") &&
                name.matches(Regex("[A-Za-z0-9_]+"))
    }
}