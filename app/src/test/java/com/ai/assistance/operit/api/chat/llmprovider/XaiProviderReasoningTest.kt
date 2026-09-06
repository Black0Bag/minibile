package com.ai.assistance.operit.api.chat.llmprovider

import com.ai.assistance.operit.data.collects.ApiProviderConfigs
import com.ai.assistance.operit.data.collects.ModelThinkingConfigDefaults
import com.ai.assistance.operit.data.model.ApiProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XaiProviderReasoningTest {
    private fun mapping(modelName: String): ThinkingQualityMapping =
        ThinkingQualityMappingRegistry.resolve(
            providerTypeId = ApiProviderType.XAI.name,
            modelName = modelName,
            thinkingConfigurations = ModelThinkingConfigDefaults.forProvider(ApiProviderType.XAI.name)
        )

    @Test
    fun defaultConfigUsesTheOfficialXaiEndpointAndModel() {
        assertEquals(
            "grok-4.6",
            ApiProviderConfigs.getDefaultModelName(ApiProviderType.XAI)
        )
        assertEquals(
            "https://api.x.ai/v1/chat/completions",
            ApiProviderConfigs.getDefaultApiEndpoint(ApiProviderType.XAI)
        )
        assertEquals(
            "https://api.x.ai/v1/models",
            ModelListFetcher.getModelsListUrl(
                "https://api.x.ai/v1/chat/completions",
                ApiProviderType.XAI
            )
        )
    }

    @Test
    fun grokModelsUseRequiredReasoningEffortLevels() {
        for (modelName in listOf("grok-4.6", "grok-4.5-latest", "grok-3-mini")) {
            val mapping = mapping(modelName)

            assertEquals(ThinkingQualityControl.LEVELS, mapping.control)
            assertEquals("reasoning_effort", mapping.parameterLabel)
            assertTrue(mapping.reasoningRequired)
            assertEquals(
                listOf("low", "medium", "high", "xhigh"),
                mapping.options.map { mapping.textValueFor(it.id) }
            )
        }
    }
}
