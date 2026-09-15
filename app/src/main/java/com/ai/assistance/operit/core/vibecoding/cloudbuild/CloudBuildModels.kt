package com.ai.assistance.operit.core.vibecoding.cloudbuild

import com.ai.assistance.operit.core.vibecoding.domain.BuildBackend

/** Phase E 云端构建只读能力的数据模型（纯 Kotlin，可 JVM 单测）。 */

/** 技术栈识别结果。 */
data class StackDetection(
    val primary: String,
    val detectedMarkers: List<String>,
    val suggestedBackend: BuildBackend,
    val rationale: String,
    val confidence: Confidence,
) {
    enum class Confidence { HIGH, MEDIUM, UNKNOWN }
}

/** CI 模板声明（Phase E 只做预览与选择，不写远端）。 */
data class CloudBuildTemplate(
    val id: String,
    val displayName: String,
    val detectionMarkers: List<String>,
    val runner: String,
    val toolchain: List<String>,
    val testCommand: String?,
    val buildCommand: String,
    val artifactGlobs: List<String>,
    val signatureRequired: Boolean,
    val version: Int,
    val source: String,
)

/** 权限预检结果（只读，不输出 token）。 */
data class PermissionProbe(
    val hasToken: Boolean,
    val requiredScopes: List<String>,
    val missingScopes: List<String>,
    val reason: String,
) {
    fun isReady(): Boolean = hasToken && missingScopes.isEmpty()
}

/** 远端运行/Release 查询结果（status action 的输出）。 */
data class CloudRunSummary(
    val runId: Long,
    val workflowName: String,
    val headSha: String,
    val status: String,
    val conclusion: String?,
    val runUrl: String,
    val createdAt: String,
)

/** cloud_build_release 只读动作统一结果。 */
data class CloudBuildInspectionResult(
    val action: String,
    val ok: Boolean,
    val summary: String,
    val stack: StackDetection? = null,
    val matchedTemplate: CloudBuildTemplate? = null,
    val permission: PermissionProbe? = null,
    val runs: List<CloudRunSummary> = emptyList(),
    val warnings: List<String> = emptyList(),
)