package com.lyihub.archiveassistant.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.lyihub.archiveassistant.domain.SampleKnowledgeData
import com.lyihub.archiveassistant.ui.theme.ArchiveAssistantTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomePaneTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homePane_displaysParserInputAndClassifyButton() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = "",
                    parserValidationMessage = null,
                    recentTopics = SampleKnowledgeData.topics.take(6),
                    itemsByTopic = SampleKnowledgeData.items.groupBy { it.topicId },
                    onParserInputChanged = {},
                    onSubmitParserInput = {},
                    onTopicSelected = {},
                    onOpenSettings = {},
                    onOpenManage = {},
                    onCreateTopic = {},
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onOpenClipboard = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("parser-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clipboard-button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("classify-button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent-topic-list").assertIsDisplayed()
        composeTestRule.onNodeWithTag("manage-button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home-create-topic-button").assertIsDisplayed()
    }

    @Test
    fun homePane_memorialDemoButton_click_triggersCallback() {
        var memorialDemoCalled = false

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = "",
                    parserValidationMessage = null,
                    recentTopics = emptyList(),
                    itemsByTopic = emptyMap(),
                    onParserInputChanged = {},
                    onSubmitParserInput = {},
                    onTopicSelected = {},
                    onOpenSettings = {},
                    onOpenManage = {},
                    onCreateTopic = {},
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onOpenClipboard = {},
                    onOpenMemorialDemo = { memorialDemoCalled = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("memorial-demo-button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("memorial-demo-button").performClick()
        assertEquals(true, memorialDemoCalled)
    }

    @Test
    fun homePane_parserInput_typeAndSubmit_triggersCallbacks() {
        val inputValue = mutableStateOf("")
        var submitCalled = false

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = inputValue.value,
                    parserValidationMessage = null,
                    recentTopics = emptyList(),
                    itemsByTopic = emptyMap(),
                    onParserInputChanged = { inputValue.value = it },
                    onSubmitParserInput = { submitCalled = true },
                    onTopicSelected = {},
                    onOpenSettings = {},
                    onOpenManage = {},
                    onCreateTopic = {},
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onOpenClipboard = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("parser-input").performTextInput("test input")
        assertEquals("test input", inputValue.value)

        composeTestRule.onNodeWithTag("classify-button").performClick()
        assertEquals(true, submitCalled)
    }

    @Test
    fun homePane_clipboardButton_click_triggersCallback() {
        var clipboardCalled = false

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = "",
                    parserValidationMessage = null,
                    recentTopics = emptyList(),
                    itemsByTopic = emptyMap(),
                    onParserInputChanged = {},
                    onSubmitParserInput = {},
                    onTopicSelected = {},
                    onOpenSettings = {},
                    onOpenManage = {},
                    onCreateTopic = {},
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onOpenClipboard = { clipboardCalled = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("clipboard-button").performClick()
        assertEquals(true, clipboardCalled)
    }

    @Test
    fun homePane_validationMessage_showsErrorText() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = "   ",
                    parserValidationMessage = "请输入要归档的内容",
                    recentTopics = emptyList(),
                    itemsByTopic = emptyMap(),
                    onParserInputChanged = {},
                    onSubmitParserInput = {},
                    onTopicSelected = {},
                    onOpenSettings = {},
                    onOpenManage = {},
                    onCreateTopic = {},
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onOpenClipboard = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("parser-input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("classify-button").assertIsDisplayed()
        composeTestRule.onNodeWithText("请输入要归档的内容").assertIsDisplayed()
    }

    @Test
    fun homePane_recentTopics_showsTopicCards() {
        val recent = SampleKnowledgeData.topics.take(2)

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = "",
                    parserValidationMessage = null,
                    recentTopics = recent,
                    itemsByTopic = SampleKnowledgeData.items.groupBy { it.topicId },
                    onParserInputChanged = {},
                    onSubmitParserInput = {},
                    onTopicSelected = {},
                    onOpenSettings = {},
                    onOpenManage = {},
                    onCreateTopic = {},
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onOpenClipboard = {},
                )
            }
        }

        recent.forEach { topic ->
            composeTestRule.onNodeWithTag("topic-card-${topic.id}").assertIsDisplayed()
        }
    }

    @Test
    fun homePane_createTopicButton_click_triggersCallback() {
        var createTopicCalled = false

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = "",
                    parserValidationMessage = null,
                    recentTopics = emptyList(),
                    itemsByTopic = emptyMap(),
                    onParserInputChanged = {},
                    onSubmitParserInput = {},
                    onTopicSelected = {},
                    onOpenSettings = {},
                    onOpenManage = {},
                    onCreateTopic = { createTopicCalled = true },
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onOpenClipboard = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("home-create-topic-button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home-create-topic-button").performClick()
        assertEquals(true, createTopicCalled)
    }

    @Test
    fun homePane_blankInput_classifyButtonIsDisabled() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = "",
                    parserValidationMessage = null,
                    recentTopics = emptyList(),
                    itemsByTopic = emptyMap(),
                    onParserInputChanged = {},
                    onSubmitParserInput = {},
                    onTopicSelected = {},
                    onOpenSettings = {},
                    onOpenManage = {},
                    onCreateTopic = {},
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onOpenClipboard = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("classify-button").assertIsNotEnabled()
    }

    @Test
    fun homePane_nonBlankInput_classifyButtonIsEnabled() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = "some input",
                    parserValidationMessage = null,
                    recentTopics = emptyList(),
                    itemsByTopic = emptyMap(),
                    onParserInputChanged = {},
                    onSubmitParserInput = {},
                    onTopicSelected = {},
                    onOpenSettings = {},
                    onOpenManage = {},
                    onCreateTopic = {},
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onOpenClipboard = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("classify-button").assertIsEnabled()
    }

    @Test
    fun homePane_smartSummarizing_classifyButtonIsDisabledAndShowsLoadingText() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = "some input",
                    parserValidationMessage = null,
                    recentTopics = emptyList(),
                    itemsByTopic = emptyMap(),
                    onParserInputChanged = {},
                    onSubmitParserInput = {},
                    onTopicSelected = {},
                    onOpenSettings = {},
                    onOpenManage = {},
                    onCreateTopic = {},
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onOpenClipboard = {},
                    isSmartSummarizing = true,
                )
            }
        }

        composeTestRule.onNodeWithTag("classify-button").assertIsNotEnabled()
        composeTestRule.onNodeWithText("归纳中…").assertIsDisplayed()
    }

    @Test
    fun homePane_smartSummarizationMessage_showsErrorText() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = "some input",
                    parserValidationMessage = null,
                    recentTopics = emptyList(),
                    itemsByTopic = emptyMap(),
                    onParserInputChanged = {},
                    onSubmitParserInput = {},
                    onTopicSelected = {},
                    onOpenSettings = {},
                    onOpenManage = {},
                    onCreateTopic = {},
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onOpenClipboard = {},
                    smartSummarizationMessage = "智能归纳失败",
                )
            }
        }

        composeTestRule.onNodeWithText("智能归纳失败").assertIsDisplayed()
    }
}
