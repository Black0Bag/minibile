package com.ai.assistance.operit.core.vibecoding.policy

import com.ai.assistance.operit.core.vibecoding.domain.CodingSessionMode
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingActorType

/**
 * 统一工具硬门禁（fail-closed）。
 *
 * 判定规则：
 * - 无上下文：放行（兼容旧调用/UI 直连；Agent 路径必须显式携带上下文）。
 * - 用户直连调用：放行（由 UI 主动发起，保留工具级权限审批）。
 * - PLAN 模式：拒绝写类工具；只读工具放行。
 * - BUILD 模式：必须有与当前计划版本一致的审批快照；否则拒绝。
 * - 调用者必须是受支持的 Agent/用户类型。
 *
 * 本类保持纯 Kotlin 无 Android 依赖，便于 JVM 单测；与 Phase A 的领域模型共用模式/审批语义。
 */
object ToolPolicyGate {

    /** 写类工具白名单（不可穷尽时应按“保守判定”拒绝未知写工具，避免误放行）。 */
    private val writeToolNames: Set<String> =
        setOf(
            // 文件系统写
            "write_file", "write_file_binary", "delete_file", "move_file", "copy_file",
            "make_directory", "zip_files", "unzip_files", "apply_file", "edit_file", "create_file",
            // 终端/Shell 写
            "execute_shell", "execute_in_terminal_session", "execute_in_terminal_session_streaming",
            "input_in_terminal_session", "execute_hidden_terminal_command",
            // 环境变量写
            "write_environment_variable",
            // 系统/手机写
            "install_app", "uninstall_app", "start_app", "stop_app",
            "modify_system_setting", "send_broadcast", "execute_intent", "send_notification",
            // 记忆/画像写
            "create_memory", "update_memory", "delete_memory", "move_memory",
            "link_memories", "update_memory_link", "delete_memory_link",
            "update_user_profile", "update_user_preferences",
            // 会话写
            "create_new_chat", "delete_chat", "update_chat_title", "switch_chat",
            "start_chat_service", "stop_chat_service",
            // 包/工作流写
            "use_package", "set_sandbox_package_enabled",
            "create_workflow", "update_workflow", "patch_workflow", "delete_workflow",
            "enable_workflow", "disable_workflow", "trigger_workflow", "trigger_tasker_event",
            // MCP / Skill 写
            "restart_mcp_with_logs",
        )

    /** 只读工具白名单（可安全放行的常见读取类）。 */
    private val readToolNames: Set<String> =
        setOf(
            "list_files", "read_file", "read_file_part", "read_file_full", "read_file_binary",
            "file_exists", "file_info", "find_files", "grep_code", "grep_context",
            "list_sandbox_packages", "read_environment_variable",
            "list_model_configs", "get_function_model_config", "list_function_model_configs",
            "list_installed_apps", "get_system_setting", "get_notifications",
            "get_device_info", "get_device_location", "get_app_usage_time",
            "query_memory", "get_memory_by_title", "query_memory_links",
            "list_chats", "find_chat", "get_chat_messages", "get_chat_messages_range",
            "calculate", "sleep", "agent_status",
            "visit_web", "http_request", "multipart_request",
            "search_repositories", "get_repository", "get_file_content",
            "browser_navigate", "browser_snapshot", "browser_evaluate", "browser_tabs",
            "get_page_info",
            "test_model_config_connection",
        )

    /**
     * 判定单次工具调用是否可执行。
     *
     * @param toolName 协议工具 ID（如 `read_file`、`super_admin:terminal`、`pkg:tool`）。
     * @param context 执行上下文；null 表示调用方未提供上下文（兼容旧调用，按 USER_DIRECT 放行）。
     */
    fun evaluate(toolName: String, context: ToolExecutionContext?): ToolPolicyDecision {
        val name = toolName.trim()
        if (name.isEmpty()) {
            return deny(ToolPolicyRejectionCode.UNKNOWN_TOOL, "工具名为空")
        }

        // 兼容旧调用：无上下文视为用户直连，不阻断现有 UI/子系统行为。
        if (context == null) {
            return ToolPolicyDecision.Allow
        }

        // 用户直连调用由 UI 主动发起，不纳入 Agent 模式门禁（但保留工具级权限审批）。
        if (context.invocationSource == ToolInvocationSource.USER_DIRECT) {
            return ToolPolicyDecision.Allow
        }

        // 调用者类型检查：仅允许用户/主代理/子代理发起。
        if (context.actorType !in allowedActorTypes) {
            return deny(ToolPolicyRejectionCode.ACTOR_NOT_ALLOWED, "调用者类型不允许: ${context.actorType}")
        }

        // 只读判定（先按白名单判断；无法判定时，写类工具按拒绝处理，避免误放行）。
        val isWrite = isWriteTool(name)
        if (context.modeSnapshot == CodingSessionMode.PLAN) {
            return if (isWrite) {
                deny(ToolPolicyRejectionCode.PLAN_WRITE_DENIED, "PLAN 模式禁止写操作: $name")
            } else {
                ToolPolicyDecision.Allow
            }
        }

        // BUILD 模式：需要当前计划审批且审批版本与当前版本一致。
        if (context.modeSnapshot == CodingSessionMode.BUILD) {
            val approved = context.approvedPlanRevision
            val current = context.currentPlanRevision
            if (approved == null) {
                return deny(ToolPolicyRejectionCode.BUILD_APPROVAL_REQUIRED, "BUILD 模式缺少当前计划审批")
            }
            if (current != null && approved != current) {
                return deny(ToolPolicyRejectionCode.APPROVAL_STALE, "计划已变化，旧审批失效")
            }
            return ToolPolicyDecision.Allow
        }

        // 未知模式：fail-closed。
        return deny(ToolPolicyRejectionCode.MISSING_CONTEXT, "未知会话模式: ${context.modeSnapshot}")
    }

    /** 是否属于写类工具。包工具/未知工具按「写」保守处理（仅允许显式只读名单放行）。 */
    fun isWriteTool(toolName: String): Boolean {
        val name = toolName.trim()
        // 显式只读白名单优先放行（含包内工具名称匹配）。
        if (readToolNames.contains(name) || readToolNames.any { name.endsWith(":$it") }) {
            return false
        }
        if (writeToolNames.contains(name)) {
            return true
        }
        // 未知工具：按写处理，PLAN 拒绝；避免漏放行写类工具。
        return true
    }

    private val allowedActorTypes: Set<VibeCodingActorType> =
        setOf(VibeCodingActorType.USER, VibeCodingActorType.MAIN_AGENT, VibeCodingActorType.SUBAGENT)

    private fun deny(code: ToolPolicyRejectionCode, reason: String): ToolPolicyDecision =
        ToolPolicyDecision.Deny(code, reason)
}