package com.lyihub.archiveassistant.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.lyihub.archiveassistant.data.AiEndpointLatencyResult
import com.lyihub.archiveassistant.domain.AiEnginePreset
import com.lyihub.archiveassistant.domain.AiEngineSettings
import com.lyihub.archiveassistant.domain.AiEngineType
import com.lyihub.archiveassistant.ui.theme.ArchiveAssistantTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsPaneTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsPane_displaysGroupLabelAndEngineSelector() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = AiEngineSettings(engineType = AiEngineType.OPENAI_COMPATIBLE),
                    onAiSettingsChanged = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("settings-pane").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI 推理引擎配置").assertIsDisplayed()
        composeTestRule.onNodeWithTag("engine-type-selector").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preset-selector").assertIsDisplayed()
    }

    @Test
    fun settingsPane_cloudMode_showsCloudFieldsAndHidesLocalFields() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = AiEngineSettings(engineType = AiEngineType.OPENAI_COMPATIBLE),
                    onAiSettingsChanged = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("cloud-base-url-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("api-key-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cloud-model-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("local-endpoint-input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("local-model-input").assertDoesNotExist()
    }

    @Test
    fun settingsPane_localMode_showsLocalFieldsAndHidesCloudFields() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = AiEngineSettings(engineType = AiEngineType.LOCAL_MODEL),
                    onAiSettingsChanged = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("local-endpoint-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("local-model-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cloud-base-url-input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("api-key-input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("cloud-model-input").assertDoesNotExist()
    }

    @Test
    fun settingsPane_switchEngineType_updatesVisibleFields() {
        var currentSettings = AiEngineSettings(engineType = AiEngineType.OPENAI_COMPATIBLE)

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = currentSettings,
                    onAiSettingsChanged = { currentSettings = it },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("cloud-base-url-input").assertIsDisplayed()

        composeTestRule.onNodeWithTag("engine-type-selector").performClick()
        composeTestRule.onNodeWithText("本地模型").performClick()

        composeTestRule.waitForIdle()
        assertEquals(AiEngineType.LOCAL_MODEL, currentSettings.engineType)
        composeTestRule.onNodeWithTag("local-endpoint-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cloud-base-url-input").assertDoesNotExist()
    }

    @Test
    fun settingsPane_apiKeyInput_isMasked() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = AiEngineSettings(engineType = AiEngineType.OPENAI_COMPATIBLE),
                    onAiSettingsChanged = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("api-key-input").performTextInput("sk-test-secret")
        composeTestRule.onNodeWithTag("api-key-input").assertTextEquals("•••••••••••••")
    }

    @Test
    fun settingsPane_baseUrlChange_triggersCallback() {
        var updatedSettings: AiEngineSettings? = null

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = AiEngineSettings(
                        engineType = AiEngineType.OPENAI_COMPATIBLE,
                        baseUrl = "https://api.example.com/v1",
                    ),
                    onAiSettingsChanged = { updatedSettings = it },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("cloud-base-url-input").performTextInput("/chat")
        composeTestRule.waitForIdle()

        assertEquals("https://api.example.com/v1/chat", updatedSettings?.baseUrl)
    }

    @Test
    fun settingsPane_localEndpointChange_triggersCallback() {
        var updatedSettings: AiEngineSettings? = null

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = AiEngineSettings(
                        engineType = AiEngineType.LOCAL_MODEL,
                        localEndpoint = "http://127.0.0.1:11434",
                    ),
                    onAiSettingsChanged = { updatedSettings = it },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("local-endpoint-input").performTextInput("/api")
        composeTestRule.waitForIdle()

        assertEquals("http://127.0.0.1:11434/api", updatedSettings?.localEndpoint)
    }

    @Test
    fun settingsPane_displaysTestLatencyButton() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = AiEngineSettings(engineType = AiEngineType.OPENAI_COMPATIBLE),
                    onAiSettingsChanged = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("test-api-latency-button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("test-api-latency-button").assertTextEquals("测试 API 延迟")
    }

    @Test
    fun settingsPane_testLatencySuccess_showsLatencyResult() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = AiEngineSettings(engineType = AiEngineType.OPENAI_COMPATIBLE),
                    onAiSettingsChanged = {},
                    onBack = {},
                    testLatency = { _, _ -> AiEndpointLatencyResult.Success(123) },
                )
            }
        }

        composeTestRule.onNodeWithTag("test-api-latency-button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("api-latency-result").assertIsDisplayed()
        composeTestRule.onNodeWithTag("api-latency-result").assertTextEquals("延迟：123 ms")
    }

    @Test
    fun settingsPane_testLatencyFailure_showsFailureResult() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = AiEngineSettings(engineType = AiEngineType.OPENAI_COMPATIBLE),
                    onAiSettingsChanged = {},
                    onBack = {},
                    testLatency = { _, _ -> AiEndpointLatencyResult.Failure("connection refused") },
                )
            }
        }

        composeTestRule.onNodeWithTag("test-api-latency-button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("api-latency-result").assertIsDisplayed()
        composeTestRule.onNodeWithTag("api-latency-result")
            .assertTextEquals("测试失败：connection refused")
    }

    @Test
    fun settingsPane_localModel_showsTestLatencyButton() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = AiEngineSettings(engineType = AiEngineType.LOCAL_MODEL),
                    onAiSettingsChanged = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("test-api-latency-button").assertIsDisplayed()
    }

    @Test
    fun settingsPane_selectPreset_appliesPresetSettingsAndApiKey() {
        var currentSettings by mutableStateOf(AiEngineSettings(engineType = AiEngineType.OPENAI_COMPATIBLE))
        val preset = AiEnginePreset(
            name = "Work OpenAI",
            engineType = AiEngineType.OPENAI_RESPONSES,
            baseUrl = "https://work.openai.com",
            modelName = "gpt-4o",
            apiKey = "sk-work-secret",
            localEndpoint = "http://127.0.0.1:11434",
        )

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = currentSettings,
                    onAiSettingsChanged = { currentSettings = it },
                    onBack = {},
                    presets = listOf(preset),
                )
            }
        }

        composeTestRule.onNodeWithTag("preset-selector").performClick()
        composeTestRule.onNodeWithText("Work OpenAI").performClick()
        composeTestRule.waitForIdle()

        assertEquals(AiEngineType.OPENAI_RESPONSES, currentSettings.engineType)
        assertEquals("https://work.openai.com", currentSettings.baseUrl)
        assertEquals("gpt-4o", currentSettings.modelName)
        assertEquals("sk-work-secret", currentSettings.apiKey)
        composeTestRule.onNodeWithTag("api-key-input").assertTextEquals("•••••••••••••")
    }

    @Test
    fun settingsPane_savePreset_addsPresetAndCallsCallback() {
        var currentSettings by mutableStateOf(AiEngineSettings(
            engineType = AiEngineType.ANTHROPIC,
            baseUrl = "https://api.anthropic.com",
            modelName = "claude-3-opus",
        ))
        var savedPresets: List<AiEnginePreset>? = null

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = currentSettings,
                    onAiSettingsChanged = { currentSettings = it },
                    onBack = {},
                    presets = emptyList(),
                    onPresetsChanged = { savedPresets = it },
                )
            }
        }

        composeTestRule.onNodeWithTag("api-key-input").performTextInput("sk-anthropic")
        composeTestRule.onNodeWithTag("preset-selector").performClick()
        composeTestRule.onNodeWithText("保存当前为预设").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("预设名称").performTextInput("Anthropic")
        composeTestRule.onNodeWithText("保存").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, savedPresets?.size)
        val preset = savedPresets?.first()
        assertEquals("Anthropic", preset?.name)
        assertEquals(AiEngineType.ANTHROPIC, preset?.engineType)
        assertEquals("https://api.anthropic.com", preset?.baseUrl)
        assertEquals("claude-3-opus", preset?.modelName)
        assertEquals("sk-anthropic", preset?.apiKey)
    }

    @Test
    fun settingsPane_deletePreset_removesPresetAndCallsCallback() {
        var currentSettings by mutableStateOf(AiEngineSettings(engineType = AiEngineType.OPENAI_COMPATIBLE))
        var savedPresets = listOf(
            AiEnginePreset(
                name = "Preset One",
                engineType = AiEngineType.OPENAI_COMPATIBLE,
                baseUrl = "https://api.example.com",
                modelName = "model",
                apiKey = "key",
                localEndpoint = "http://127.0.0.1:11434",
            ),
        )

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                SettingsPane(
                    aiSettings = currentSettings,
                    onAiSettingsChanged = { currentSettings = it },
                    onBack = {},
                    presets = savedPresets,
                    onPresetsChanged = { savedPresets = it },
                )
            }
        }

        composeTestRule.onNodeWithTag("preset-selector").performClick()
        composeTestRule.onNodeWithText("Preset One").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preset-selector").performClick()
        composeTestRule.onNodeWithText("删除当前预设").performClick()
        composeTestRule.waitForIdle()

        assertEquals(emptyList<AiEnginePreset>(), savedPresets)
    }
}
