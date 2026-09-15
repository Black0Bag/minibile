package com.ai.assistance.operit.core.vibecoding.cloudbuild

import com.ai.assistance.operit.core.vibecoding.domain.BuildBackend

/**
 * 云端构建只读检查器（Phase E 核心，纯 Kotlin 可单测）。
 *
 * 负责 cloud_build_release 的只读动作：
 * - `inspect`：识别技术栈、选择 CI 模板、给出构建后端建议；
 * - `prepare`：生成 CI/发布计划预览（不修改远端）；
 * - `status`：解析远端运行/Release 查询结果（HTTP 由调用方注入）。
 *
 * 本类不做任何远端写入；写动作由 Phase F 实现。
 */
class CloudBuildInspector(
    private val registry: CloudBuildTemplateRegistry = CloudBuildTemplateRegistry,
) {

    fun inspect(projectFiles: Collection<String>): CloudBuildInspectionResult {
        val stack = detectStack(projectFiles)
        val template = registry.match(projectFiles)
        val warnings = buildWarnings(stack, template)
        return CloudBuildInspectionResult(
            action = "inspect",
            ok = stack.confidence != StackDetection.Confidence.UNKNOWN,
            summary = "识别技术栈 ${stack.primary}，建议后端 ${stack.suggestedBackend}，匹配模板 ${template.id}",
            stack = stack,
            matchedTemplate = template,
            warnings = warnings,
        )
    }

    fun prepare(projectFiles: Collection<String>): CloudBuildInspectionResult {
        val inspected = inspect(projectFiles)
        val template = inspected.matchedTemplate ?: return inspected
        val summary =
            buildString {
                appendLine("云端构建计划预览（仅只读，不写远端）：")
                appendLine("- 仓库模式：TEMPORARY private（命名 vc-build-{task}-{timestamp}）")
                appendLine("- CI 模板：${template.id}（v${template.version}，来源 ${template.source}）")
                appendLine("- Runner：${template.runner}；工具链：${template.toolchain.joinToString(", ")}")
                appendLine("- 测试命令：${template.testCommand ?: "无"}；构建命令：${template.buildCommand}")
                appendLine("- 产物：${template.artifactGlobs.joinToString(", ") { it }}")
                appendLine("- 签名需求：${if (template.signatureRequired) "需要（首次须用户授权固定密钥）" else "不需要"}")
                appendLine("- 产物语义：TEST_ARTIFACT（临时验证，不保证覆盖升级）")
            }
        return inspected.copy(action = "prepare", summary = summary.trimEnd())
    }

    /** 权限预检（不输出 token 本身，只报告缺失权限）。 */
    fun probePermissions(hasToken: Boolean, availableScopes: Set<String>): PermissionProbe {
        val required = listOf("Actions: read", "Contents: read")
        if (!hasToken) {
            return PermissionProbe(
                hasToken = false,
                requiredScopes = required,
                missingScopes = required,
                reason = "未配置 GitHub 凭据（公开仓库只读可匿名，但私有仓库需 token）",
            )
        }
        val missing = required.filterNot { it in availableScopes }
        return PermissionProbe(
            hasToken = true,
            requiredScopes = required,
            missingScopes = missing,
            reason = if (missing.isEmpty()) "只读权限齐备" else "缺少权限: ${missing.joinToString(", ")}",
        )
    }

    /** status 动作：解析远端查询结果（由调用方提供已拉取的数据）。 */
    fun status(parsedRuns: List<CloudRunSummary>): CloudBuildInspectionResult {
        val summary =
            if (parsedRuns.isEmpty()) {
                "未找到运行记录"
            } else {
                buildString {
                    appendLine("最近运行：")
                    parsedRuns.take(5).forEach { run ->
                        appendLine("- #${run.runId} ${run.workflowName} [${run.status}${run.conclusion?.let { "/$it" } ?: ""}] sha=${run.headSha.take(8)} ${run.createdAt}")
                    }
                }.trimEnd()
            }
        return CloudBuildInspectionResult(
            action = "status",
            ok = true,
            summary = summary,
            runs = parsedRuns,
        )
    }

    private fun detectStack(projectFiles: Collection<String>): StackDetection {
        val markers = projectFiles.map { it.substringAfterLast('/') }.toSet()
        return when {
            markers.any { it == "pubspec.yaml" || it == "pubspec.yml" } ->
                StackDetection("flutter", listOf("pubspec.yaml"), BuildBackend.CLOUD, "Flutter Android 构建需 SDK/NDK，云端默认", StackDetection.Confidence.HIGH)

            markers.any { it == "Cargo.toml" } ->
                StackDetection("rust", listOf("Cargo.toml"), BuildBackend.CLOUD, "Rust 编译需完整工具链，云端默认", StackDetection.Confidence.HIGH)

            markers.any { it == "settings.gradle.kts" || it == "settings.gradle" || it == "app/build.gradle.kts" } ->
                StackDetection("android", listOf("settings.gradle.kts", "app/build.gradle.kts"), BuildBackend.CLOUD, "Android APK 构建默认云端", StackDetection.Confidence.HIGH)

            markers.any { it == "build.gradle.kts" || it == "build.gradle" } ->
                StackDetection("jvm-gradle", listOf("build.gradle.kts"), BuildBackend.CLOUD, "大型 Gradle 构建默认云端", StackDetection.Confidence.MEDIUM)

            markers.any { it == "pom.xml" } ->
                StackDetection("jvm-maven", listOf("pom.xml"), BuildBackend.CLOUD, "Maven 构建默认云端", StackDetection.Confidence.MEDIUM)

            markers.any { it == "go.mod" } ->
                StackDetection("go", listOf("go.mod"), BuildBackend.LOCAL, "Go 中小项目本地优先；缺工具链/资源时切云端", StackDetection.Confidence.HIGH)

            markers.any { it == "pyproject.toml" || it == "requirements.txt" } ->
                StackDetection("python", listOf("pyproject.toml", "requirements.txt"), BuildBackend.LOCAL, "Python 小项目依赖已缓存时本地优先", StackDetection.Confidence.MEDIUM)

            markers.any { it == "package.json" || it == "pnpm-lock.yaml" } ->
                StackDetection("node", listOf("package.json", "pnpm-lock.yaml"), BuildBackend.LOCAL, "Node 小项目本地优先；大型前端默认云端", StackDetection.Confidence.MEDIUM)

            else ->
                StackDetection("unknown", emptyList(), BuildBackend.LOCAL, "无法识别技术栈，请用户确认后再决策", StackDetection.Confidence.UNKNOWN)
        }
    }

    private fun buildWarnings(stack: StackDetection, template: CloudBuildTemplate): List<String> {
        val warnings = mutableListOf<String>()
        if (stack.confidence == StackDetection.Confidence.UNKNOWN) {
            warnings.add("技术栈未知，模板回退到 generic-command，请人工确认")
        }
        if (stack.suggestedBackend == BuildBackend.CLOUD && !template.signatureRequired) {
            warnings.add("云端构建但模板无需签名，产物为 TEST_ARTIFACT")
        }
        return warnings
    }
}