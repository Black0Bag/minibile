package com.ai.assistance.operit.data.preferences

import android.content.Context
import com.ai.assistance.operit.data.model.ActivePrompt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * 活跃提示词管理器。
 * 角色卡系统已移除：单一 default 目标，主题快照绑定 default 主题键。
 */
class ActivePromptManager private constructor(context: Context) {
    private val userPreferencesManager = UserPreferencesManager.getInstance(context)
    private val themeOperations = ThemeTargetOperationCoordinator()

    val activePromptFlow: Flow<ActivePrompt> =
        flowOf(ActivePrompt()).distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeThemePreferenceSnapshotFlow: Flow<ThemePreferenceSnapshot> =
        activePromptFlow
            .flatMapLatest { prompt ->
                userPreferencesManager.observeThemePreferenceSnapshot(characterCardId = prompt.id)
            }
            .distinctUntilChanged()

    suspend fun getActivePrompt(): ActivePrompt = activePromptFlow.first()

    suspend fun setActivePrompt(prompt: ActivePrompt) {
        themeOperations.runTransition {
            userPreferencesManager.replaceThemeForPrompt(
                target = ActivePrompt(),
                values = ThemePreferenceValues(),
            )
        }
    }

    internal suspend fun <T> runThemeTransition(action: suspend () -> T): T {
        return themeOperations.runTransition(action)
    }

    suspend fun mutateActiveThemeForPrompt(
        target: ActivePrompt,
        transform: (ThemePreferenceValues) -> ThemePreferenceValues,
    ) {
        themeOperations.runTransition {
            if (getActivePrompt() != target) return@runTransition
            userPreferencesManager.mutateThemeForPrompt(
                target = target,
                transform = transform,
            )
        }
    }

    suspend fun commitThemeDraft(
        target: ActivePrompt,
        values: ThemePreferenceValues,
    ) {
        themeOperations.runTransition {
            userPreferencesManager.replaceThemeForPrompt(
                target = target,
                values = values,
            )
        }
    }

    suspend fun resetThemeDraft(
        target: ActivePrompt,
        values: ThemePreferenceValues,
    ) {
        themeOperations.runTransition {
            userPreferencesManager.resetVisualThemeForPrompt(
                target = target,
                values = values,
            )
        }
    }

    suspend fun saveAiAvatarForPrompt(target: ActivePrompt, avatarUri: String?) {
        themeOperations.runTransition {
            userPreferencesManager.saveAiAvatarForCharacterCard(target.id, avatarUri)
        }
    }

    suspend fun saveCustomChatTitleForPrompt(target: ActivePrompt, title: String?) {
        themeOperations.runTransition {
            userPreferencesManager.saveCustomChatTitleForCharacterCard(target.id, title)
        }
    }

    suspend fun activateForChatBinding(characterCardName: String?, characterGroupId: String?) {
        // 角色卡系统已移除：绑定激活退化为 no-op（保持 default 目标）。
    }

    suspend fun resolveActiveCardIdForSend(): String {
        return getActivePrompt().id
    }

    companion object {
        @Volatile
        private var INSTANCE: ActivePromptManager? = null

        fun getInstance(context: Context): ActivePromptManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ActivePromptManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}