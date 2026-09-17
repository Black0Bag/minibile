package com.ai.assistance.operit.data.model

/**
 * 活跃提示词目标。角色卡系统已移除，仅保留 default 目标以兼容主题/头像/标题等键控存储。
 */
data class ActivePrompt(val id: String = "default")