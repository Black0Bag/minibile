package com.ai.assistance.operit.core.vibecoding.cloudbuild

/** Phase F 云端写入闭环的数据模型（纯 Kotlin，可 JVM 单测）。 */

/** 仓库创建请求（TEMPORARY 默认 private；用户已确认本仓库为公开，测试样例可显式 public）。 */
data class CreateRepoRequest(
    val name: String,
    val description: String,
    val isPrivate: Boolean = false,
    val autoInit: Boolean = true,
)

/** 仓库创建结果。 */
data class CreateRepoResult(
    val fullName: String,
    val cloneUrl: String,
    val defaultBranch: String,
)

/** 文件上传条目（Contents API 小文件 / git push 大文件由调用方选择）。 */
data class FileToUpload(
    val path: String,
    val content: String,
)

/** workflow 手动触发请求。 */
data class DispatchRequest(
    val owner: String,
    val repo: String,
    val workflowFile: String,
    val ref: String,
    val inputs: Map<String, String> = emptyMap(),
)

/** Release 验收结果。 */
data class ReleaseVerification(
    val tag: String,
    val exists: Boolean,
    val assetNames: List<String>,
    val digestVerified: Boolean,
    val signatureRequired: Boolean,
    val signatureVerified: Boolean,
) {
    fun isComplete(): Boolean =
        exists &&
            assetNames.isNotEmpty() &&
            digestVerified &&
            (!signatureRequired || signatureVerified)
}

/** 云端写入动作统一结果。 */
data class CloudWriteResult(
    val action: String,
    val ok: Boolean,
    val summary: String,
    val createdRepo: CreateRepoResult? = null,
    val release: ReleaseVerification? = null,
    val runId: Long? = null,
    val warnings: List<String> = emptyList(),
)