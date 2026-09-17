package com.ai.assistance.operit.core.chat.hooks

import android.content.Context
import com.ai.assistance.operit.data.model.ActivePrompt
import com.ai.assistance.operit.data.preferences.ActivePromptManager

suspend fun buildActivePromptHookMetadata(
    context: Context,
    chatId: String? = null,
    roleCardId: String? = null
): Map<String, Any?> {
    val appContext = context.applicationContext
    val activePrompt = ActivePromptManager.getInstance(appContext).getActivePrompt()
    return mapOf(
        "activePrompt" to mapOf(
            "type" to "default",
            "id" to activePrompt.id
        )
    )
}
