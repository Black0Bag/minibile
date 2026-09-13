package com.ai.assistance.operit.core.vibecoding.runtime

import com.ai.assistance.operit.core.vibecoding.domain.BuildBackend
import com.ai.assistance.operit.core.vibecoding.domain.BuildRunEvidence
import com.ai.assistance.operit.core.vibecoding.domain.BuildStrategyDecision
import com.ai.assistance.operit.core.vibecoding.domain.CodingSessionMode
import com.ai.assistance.operit.core.vibecoding.domain.DocumentationDecision
import com.ai.assistance.operit.core.vibecoding.domain.RequirementSpec
import com.ai.assistance.operit.core.vibecoding.domain.ResearchRecord
import com.ai.assistance.operit.core.vibecoding.domain.ReviewRecord
import com.ai.assistance.operit.core.vibecoding.domain.ReleaseEvidence
import com.ai.assistance.operit.core.vibecoding.domain.TaskPlanRevision
import com.ai.assistance.operit.core.vibecoding.domain.ValidationRun
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingActorType
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingDecision
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingTask
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingTaskEngine
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingTaskStage

/**
 * 会话任务协调器（Phase D 纵向切片核心编排）。
 *
 * 职责：把 [VibeCodingTaskEngine] 的状态机转换与 [SessionTodo] 运行时清单
 * 串成一条“澄清 -> 探索 -> 研究 -> 计划 -> 审批 -> Build -> 验证 -> 交付”的
 * 可恢复执行链。本类保持纯 Kotlin、无 Android/IO 依赖，便于 JVM 单测；
 * 持久化由调用方（Repository/DAO）负责。
 */
class SessionTaskCoordinator {
    sealed interface Result {
        data class Success(val task: VibeCodingTask, val todos: List<SessionTodo>) : Result
        data class Rejected(val task: VibeCodingTask, val reason: String) : Result
    }

    fun createSession(taskId: String, sessionId: String, workspaceId: String): Result =
        when (val decision = VibeCodingTaskEngine.createTask(taskId, sessionId, workspaceId)) {
            is VibeCodingDecision.Accepted ->
                Result.Success(
                    decision.task,
                    listOf(
                        newTodo(sessionId, "澄清需求", order = 1),
                        newTodo(sessionId, "探索代码", order = 2),
                        newTodo(sessionId, "联网研究", order = 3),
                        newTodo(sessionId, "制定计划", order = 4),
                        newTodo(sessionId, "等待用户审批", order = 5),
                        newTodo(sessionId, "实施改动", order = 6),
                        newTodo(sessionId, "验证结果", order = 7),
                        newTodo(sessionId, "交付与文档", order = 8),
                    ),
                )

            is VibeCodingDecision.Rejected -> Result.Rejected(decision.task, decision.reason)
        }

    fun recordRequirement(
        task: VibeCodingTask,
        requirement: RequirementSpec,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val decision = VibeCodingTaskEngine.recordRequirement(task, requirement)) {
            is VibeCodingDecision.Accepted ->
                Result.Success(
                    decision.task,
                    markTodo(todos, sessionId, "澄清需求", SessionTodoStatus.COMPLETED),
                )

            is VibeCodingDecision.Rejected -> Result.Rejected(decision.task, decision.reason)
        }

    fun transition(
        task: VibeCodingTask,
        target: VibeCodingTaskStage,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val decision = VibeCodingTaskEngine.transitionTo(task, target, VibeCodingActorType.MAIN_AGENT)) {
            is VibeCodingDecision.Accepted ->
                Result.Success(
                    decision.task,
                    advanceTodo(todos, sessionId, target),
                )

            is VibeCodingDecision.Rejected -> Result.Rejected(decision.task, decision.reason)
        }

    fun recordResearch(
        task: VibeCodingTask,
        record: ResearchRecord,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val decision = VibeCodingTaskEngine.recordResearch(task, record)) {
            is VibeCodingDecision.Accepted -> Result.Success(decision.task, todos)
            is VibeCodingDecision.Rejected -> Result.Rejected(decision.task, decision.reason)
        }

    fun revisePlan(
        task: VibeCodingTask,
        plan: TaskPlanRevision,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val decision = VibeCodingTaskEngine.revisePlan(task, plan)) {
            is VibeCodingDecision.Accepted -> Result.Success(decision.task, todos)
            is VibeCodingDecision.Rejected -> Result.Rejected(decision.task, decision.reason)
        }

    fun approvePlan(
        task: VibeCodingTask,
        approvedAtEpochMillis: Long,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val decision =
            VibeCodingTaskEngine.approvePlan(
                task,
                VibeCodingActorType.USER,
                approvedAtEpochMillis,
            )) {
            is VibeCodingDecision.Accepted ->
                Result.Success(
                    decision.task,
                    markTodo(todos, sessionId, "等待用户审批", SessionTodoStatus.COMPLETED),
                )

            is VibeCodingDecision.Rejected -> Result.Rejected(decision.task, decision.reason)
        }

    fun setMode(
        task: VibeCodingTask,
        mode: CodingSessionMode,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val decision = VibeCodingTaskEngine.setMode(task, mode, VibeCodingActorType.USER)) {
            is VibeCodingDecision.Accepted -> Result.Success(decision.task, todos)
            is VibeCodingDecision.Rejected -> Result.Rejected(decision.task, decision.reason)
        }

    fun recordChangedPaths(
        task: VibeCodingTask,
        paths: Set<String>,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val decision = VibeCodingTaskEngine.recordChangedPaths(task, paths)) {
            is VibeCodingDecision.Accepted -> Result.Success(decision.task, todos)
            is VibeCodingDecision.Rejected -> Result.Rejected(decision.task, decision.reason)
        }

    fun recordValidation(
        task: VibeCodingTask,
        run: ValidationRun,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val decision = VibeCodingTaskEngine.recordValidation(task, run)) {
            is VibeCodingDecision.Accepted ->
                Result.Success(
                    decision.task,
                    if (run.status == com.ai.assistance.operit.core.vibecoding.domain.ValidationStatus.PASSED) {
                        markTodo(todos, sessionId, "验证结果", SessionTodoStatus.COMPLETED)
                    } else {
                        markTodo(todos, sessionId, "验证结果", SessionTodoStatus.IN_PROGRESS)
                    },
                )

            is VibeCodingDecision.Rejected -> Result.Rejected(decision.task, decision.reason)
        }

    fun recordDocumentation(
        task: VibeCodingTask,
        decision: DocumentationDecision,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val engineDecision = VibeCodingTaskEngine.recordDocumentationDecision(task, decision)) {
            is VibeCodingDecision.Accepted ->
                Result.Success(
                    engineDecision.task,
                    markTodo(todos, sessionId, "交付与文档", SessionTodoStatus.IN_PROGRESS),
                )

            is VibeCodingDecision.Rejected -> Result.Rejected(engineDecision.task, engineDecision.reason)
        }

    fun recordReview(
        task: VibeCodingTask,
        review: ReviewRecord,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val engineDecision = VibeCodingTaskEngine.recordReview(task, review)) {
            is VibeCodingDecision.Accepted -> Result.Success(engineDecision.task, todos)
            is VibeCodingDecision.Rejected -> Result.Rejected(engineDecision.task, engineDecision.reason)
        }

    fun recordBuildStrategy(
        task: VibeCodingTask,
        backend: BuildBackend,
        reason: String,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val engineDecision =
            VibeCodingTaskEngine.recordBuildStrategy(task, BuildStrategyDecision(backend, reason))) {
            is VibeCodingDecision.Accepted -> Result.Success(engineDecision.task, todos)
            is VibeCodingDecision.Rejected -> Result.Rejected(engineDecision.task, engineDecision.reason)
        }

    fun recordBuildRun(
        task: VibeCodingTask,
        run: BuildRunEvidence,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val engineDecision = VibeCodingTaskEngine.recordBuildRun(task, run)) {
            is VibeCodingDecision.Accepted -> Result.Success(engineDecision.task, todos)
            is VibeCodingDecision.Rejected -> Result.Rejected(engineDecision.task, engineDecision.reason)
        }

    fun recordReleaseEvidence(
        task: VibeCodingTask,
        evidence: ReleaseEvidence,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result =
        when (val engineDecision = VibeCodingTaskEngine.recordReleaseEvidence(task, evidence)) {
            is VibeCodingDecision.Accepted ->
                Result.Success(
                    engineDecision.task,
                    markTodo(todos, sessionId, "交付与文档", SessionTodoStatus.COMPLETED),
                )

            is VibeCodingDecision.Rejected -> Result.Rejected(engineDecision.task, engineDecision.reason)
        }

    /** 六段式交付收口：将最后 TODO 标记完成（不绕过状态机，仅更新 TODO 清单）。 */
    fun completeDelivery(
        task: VibeCodingTask,
        todos: List<SessionTodo>,
        sessionId: String,
    ): Result {
        val finalTodos = markTodo(todos, sessionId, "交付与文档", SessionTodoStatus.COMPLETED)
        return Result.Success(task, finalTodos)
    }

    private fun newTodo(
        sessionId: String,
        content: String,
        order: Int,
    ) = SessionTodo(
        id = "todo-$sessionId-$order",
        sessionId = sessionId,
        content = content,
        order = order,
    )

    private fun markTodo(
        todos: List<SessionTodo>,
        sessionId: String,
        content: String,
        status: SessionTodoStatus,
    ): List<SessionTodo> =
        todos.map { todo ->
            if (todo.sessionId == sessionId && todo.content == content) {
                when (status) {
                    SessionTodoStatus.COMPLETED -> todo.complete()
                    SessionTodoStatus.IN_PROGRESS -> todo.start()
                    else -> todo
                }
            } else {
                todo
            }
        }

    private fun advanceTodo(
        todos: List<SessionTodo>,
        sessionId: String,
        stage: VibeCodingTaskStage,
    ): List<SessionTodo> {
        val contentForStage =
            when (stage) {
                VibeCodingTaskStage.CLARIFYING -> "澄清需求"
                VibeCodingTaskStage.EXPLORING -> "探索代码"
                VibeCodingTaskStage.RESEARCHING -> "联网研究"
                VibeCodingTaskStage.PLANNING,
                VibeCodingTaskStage.WAITING_PLAN_APPROVAL,
                -> "制定计划"
                VibeCodingTaskStage.READY_TO_BUILD -> "等待用户审批"
                VibeCodingTaskStage.IMPLEMENTING -> "实施改动"
                VibeCodingTaskStage.VALIDATING -> "验证结果"
                VibeCodingTaskStage.DOCUMENTING,
                VibeCodingTaskStage.REVIEWING,
                VibeCodingTaskStage.LOCAL_BUILD,
                VibeCodingTaskStage.CLOUD_BUILD,
                VibeCodingTaskStage.RELEASING,
                VibeCodingTaskStage.COMPLETED,
                -> "交付与文档"
                else -> null
            } ?: return todos

        return todos.map { todo ->
            if (todo.sessionId == sessionId && todo.content == contentForStage) {
                todo.start()
            } else {
                todo
            }
        }
    }
}