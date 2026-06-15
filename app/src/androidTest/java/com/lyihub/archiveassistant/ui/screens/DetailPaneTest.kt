package com.lyihub.archiveassistant.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lyihub.archiveassistant.domain.ContentType
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.SampleKnowledgeData
import com.lyihub.archiveassistant.domain.Topic
import com.lyihub.archiveassistant.ui.theme.ArchiveAssistantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetailPaneTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultTopic = SampleKnowledgeData.topics.first { it.id == SampleKnowledgeData.DefaultTopicId }
    private val defaultItems = SampleKnowledgeData.items.filter { it.topicId == SampleKnowledgeData.DefaultTopicId }

    @Test
    fun detailPane_displaysTabsAndCards() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                DetailPane(
                    topic = defaultTopic,
                    items = defaultItems,
                    activeFilter = ContentType.ALL,
                    onBack = {},
                    onFilterSelected = {},
                    onItemClick = {},
                    onAddItemClick = {},
                    searchQuery = "",
                )
            }
        }

        composeTestRule.onNodeWithTag("detail-pane").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail-tabs").assertIsDisplayed()
        defaultItems.forEach { item ->
            composeTestRule.onNodeWithTag("knowledge-card-${item.id}").assertIsDisplayed()
        }
    }

    @Test
    fun detailPane_filterTabs_switchFilter() {
        var selectedFilter: ContentType? = null

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                DetailPane(
                    topic = defaultTopic,
                    items = defaultItems,
                    activeFilter = ContentType.ALL,
                    onBack = {},
                    onFilterSelected = { selectedFilter = it },
                    onItemClick = {},
                    onAddItemClick = {},
                    searchQuery = "",
                )
            }
        }

        composeTestRule.onNodeWithText("文档").performClick()
        assertEquals(ContentType.DOCUMENT, selectedFilter)

        composeTestRule.onNodeWithText("网页").performClick()
        assertEquals(ContentType.WEB_ARTICLE, selectedFilter)
    }

    @Test
    fun detailPane_emptyFilter_showsEmptyState() {
        composeTestRule.setContent {
            ArchiveAssistantTheme {
                DetailPane(
                    topic = defaultTopic,
                    items = emptyList(),
                    activeFilter = ContentType.DOCUMENT,
                    onBack = {},
                    onFilterSelected = {},
                    onItemClick = {},
                    onAddItemClick = {},
                    searchQuery = "",
                )
            }
        }

        composeTestRule.onNodeWithTag("detail-tabs").assertIsDisplayed()
        composeTestRule.onNodeWithText("该分类下暂无资料").assertIsDisplayed()
    }

    @Test
    fun detailPane_cardClick_triggersCallback() {
        var clickedItemId: String? = null

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                DetailPane(
                    topic = defaultTopic,
                    items = defaultItems,
                    activeFilter = ContentType.ALL,
                    onBack = {},
                    onFilterSelected = {},
                    onItemClick = { clickedItemId = it },
                    onAddItemClick = {},
                    searchQuery = "",
                )
            }
        }

        val targetItem = defaultItems.first()
        composeTestRule.onNodeWithTag("knowledge-card-${targetItem.id}").performClick()
        assertEquals(targetItem.id, clickedItemId)
    }

    @Test
    fun cardModal_displaysItemDetailsAndCloses() {
        val item = defaultItems.first()
        var closeCalled = false

        composeTestRule.setContent {
            ArchiveAssistantTheme {
                CardModal(
                    item = item,
                    onClose = { closeCalled = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("card-modal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card-modal-close").assertIsDisplayed()
        composeTestRule.onNodeWithText(item.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(item.tag).assertIsDisplayed()

        composeTestRule.onNodeWithTag("card-modal-close").performClick()
        assertTrue(closeCalled)
    }
}
