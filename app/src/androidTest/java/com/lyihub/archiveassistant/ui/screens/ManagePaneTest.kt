package com.lyihub.archiveassistant.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lyihub.archiveassistant.domain.SampleKnowledgeData
import com.lyihub.archiveassistant.state.TopicNameDialogMode
import com.lyihub.archiveassistant.ui.theme.ArchiveAssistantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ManagePaneTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun managePane_displaysTopicListAndCreateButton() {
    composeTestRule.setContent {
      ArchiveAssistantTheme {
        ManagePane(
          topics = SampleKnowledgeData.topics,
          itemsByTopic = SampleKnowledgeData.items.groupBy { it.topicId },
          onBack = {},
          onTopicSelected = {},
          onCreateTopic = {},
          onRenameTopic = {},
          onDeleteTopic = {},
          onConfirmCreateTopic = {},
          onConfirmRenameTopic = {},
          onConfirmDeleteTopic = {},
          onCloseTopicNameDialog = {},
          onCloseDeleteConfirmDialog = {},
          topicNameDialogMode = null,
          topicNameDialogTopicId = null,
          topicValidationMessage = null,
          deleteConfirmTopicId = null,
        )
      }
    }

    composeTestRule.onNodeWithTag("manage-pane").assertIsDisplayed()
    composeTestRule.onNodeWithTag("create-topic-button").assertIsDisplayed()
    SampleKnowledgeData.topics.forEach { topic ->
      composeTestRule.onNodeWithTag("rename-topic-button-${topic.id}").assertIsDisplayed()
      composeTestRule.onNodeWithTag("delete-topic-button-${topic.id}").assertIsDisplayed()
    }
  }

  @Test
  fun managePane_emptyTopics_showsEmptyState() {
    composeTestRule.setContent {
      ArchiveAssistantTheme {
        ManagePane(
          topics = emptyList(),
          itemsByTopic = emptyMap(),
          onBack = {},
          onTopicSelected = {},
          onCreateTopic = {},
          onRenameTopic = {},
          onDeleteTopic = {},
          onConfirmCreateTopic = {},
          onConfirmRenameTopic = {},
          onConfirmDeleteTopic = {},
          onCloseTopicNameDialog = {},
          onCloseDeleteConfirmDialog = {},
          topicNameDialogMode = null,
          topicNameDialogTopicId = null,
          topicValidationMessage = null,
          deleteConfirmTopicId = null,
        )
      }
    }

    composeTestRule.onNodeWithText("暂无主题").assertIsDisplayed()
  }

  @Test
  fun managePane_tapTopic_triggersOnTopicSelected() {
    var selectedId = ""

    composeTestRule.setContent {
      ArchiveAssistantTheme {
        ManagePane(
          topics = SampleKnowledgeData.topics,
          itemsByTopic = SampleKnowledgeData.items.groupBy { it.topicId },
          onBack = {},
          onTopicSelected = { selectedId = it },
          onCreateTopic = {},
          onRenameTopic = {},
          onDeleteTopic = {},
          onConfirmCreateTopic = {},
          onConfirmRenameTopic = {},
          onConfirmDeleteTopic = {},
          onCloseTopicNameDialog = {},
          onCloseDeleteConfirmDialog = {},
          topicNameDialogMode = null,
          topicNameDialogTopicId = null,
          topicValidationMessage = null,
          deleteConfirmTopicId = null,
        )
      }
    }

    val firstTopic = SampleKnowledgeData.topics.first()
    composeTestRule.onNodeWithText(firstTopic.title).performClick()
    assertEquals(firstTopic.id, selectedId)
  }

  @Test
  fun managePane_createTopicButton_triggersOnCreateTopic() {
    var createCalled = false

    composeTestRule.setContent {
      ArchiveAssistantTheme {
        ManagePane(
          topics = SampleKnowledgeData.topics,
          itemsByTopic = SampleKnowledgeData.items.groupBy { it.topicId },
          onBack = {},
          onTopicSelected = {},
          onCreateTopic = { createCalled = true },
          onRenameTopic = {},
          onDeleteTopic = {},
          onConfirmCreateTopic = {},
          onConfirmRenameTopic = {},
          onConfirmDeleteTopic = {},
          onCloseTopicNameDialog = {},
          onCloseDeleteConfirmDialog = {},
          topicNameDialogMode = null,
          topicNameDialogTopicId = null,
          topicValidationMessage = null,
          deleteConfirmTopicId = null,
        )
      }
    }

    composeTestRule.onNodeWithTag("create-topic-button").performClick()
    assertTrue(createCalled)
  }

  @Test
  fun managePane_renameButton_triggersOnRenameTopic() {
    var renameId = ""

    composeTestRule.setContent {
      ArchiveAssistantTheme {
        ManagePane(
          topics = SampleKnowledgeData.topics,
          itemsByTopic = SampleKnowledgeData.items.groupBy { it.topicId },
          onBack = {},
          onTopicSelected = {},
          onCreateTopic = {},
          onRenameTopic = { renameId = it },
          onDeleteTopic = {},
          onConfirmCreateTopic = {},
          onConfirmRenameTopic = {},
          onConfirmDeleteTopic = {},
          onCloseTopicNameDialog = {},
          onCloseDeleteConfirmDialog = {},
          topicNameDialogMode = null,
          topicNameDialogTopicId = null,
          topicValidationMessage = null,
          deleteConfirmTopicId = null,
        )
      }
    }

    val firstTopic = SampleKnowledgeData.topics.first()
    composeTestRule.onNodeWithTag("rename-topic-button-${firstTopic.id}").performClick()
    assertEquals(firstTopic.id, renameId)
  }

  @Test
  fun managePane_deleteButton_triggersOnDeleteTopic() {
    var deleteId = ""

    composeTestRule.setContent {
      ArchiveAssistantTheme {
        ManagePane(
          topics = SampleKnowledgeData.topics,
          itemsByTopic = SampleKnowledgeData.items.groupBy { it.topicId },
          onBack = {},
          onTopicSelected = {},
          onCreateTopic = {},
          onRenameTopic = {},
          onDeleteTopic = { deleteId = it },
          onConfirmCreateTopic = {},
          onConfirmRenameTopic = {},
          onConfirmDeleteTopic = {},
          onCloseTopicNameDialog = {},
          onCloseDeleteConfirmDialog = {},
          topicNameDialogMode = null,
          topicNameDialogTopicId = null,
          topicValidationMessage = null,
          deleteConfirmTopicId = null,
        )
      }
    }

    val firstTopic = SampleKnowledgeData.topics.first()
    composeTestRule.onNodeWithTag("delete-topic-button-${firstTopic.id}").performClick()
    assertEquals(firstTopic.id, deleteId)
  }

  @Test
  fun managePane_createDialog_showsWhenModeIsCreate() {
    var confirmedName = ""
    var dismissed = false

    composeTestRule.setContent {
      ArchiveAssistantTheme {
        ManagePane(
          topics = SampleKnowledgeData.topics,
          itemsByTopic = SampleKnowledgeData.items.groupBy { it.topicId },
          onBack = {},
          onTopicSelected = {},
          onCreateTopic = {},
          onRenameTopic = {},
          onDeleteTopic = {},
          onConfirmCreateTopic = { confirmedName = it },
          onConfirmRenameTopic = {},
          onConfirmDeleteTopic = {},
          onCloseTopicNameDialog = { dismissed = true },
          onCloseDeleteConfirmDialog = {},
          topicNameDialogMode = TopicNameDialogMode.CREATE,
          topicNameDialogTopicId = null,
          topicValidationMessage = null,
          deleteConfirmTopicId = null,
        )
      }
    }

    composeTestRule.onNodeWithTag("topic-name-dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("建立新主题").assertIsDisplayed()

    composeTestRule.onNodeWithTag("topic-name-dialog-confirm").performClick()
    assertEquals("", confirmedName)

    composeTestRule.onNodeWithTag("topic-name-dialog-dismiss").performClick()
    assertTrue(dismissed)
  }

  @Test
  fun managePane_renameDialog_showsWhenModeIsRename() {
    var confirmedName = ""

    composeTestRule.setContent {
      ArchiveAssistantTheme {
        ManagePane(
          topics = SampleKnowledgeData.topics,
          itemsByTopic = SampleKnowledgeData.items.groupBy { it.topicId },
          onBack = {},
          onTopicSelected = {},
          onCreateTopic = {},
          onRenameTopic = {},
          onDeleteTopic = {},
          onConfirmCreateTopic = {},
          onConfirmRenameTopic = { confirmedName = it },
          onConfirmDeleteTopic = {},
          onCloseTopicNameDialog = {},
          onCloseDeleteConfirmDialog = {},
          topicNameDialogMode = TopicNameDialogMode.RENAME,
          topicNameDialogTopicId = SampleKnowledgeData.topics.first().id,
          topicValidationMessage = null,
          deleteConfirmTopicId = null,
        )
      }
    }

    composeTestRule.onNodeWithTag("topic-name-dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("重命名主题").assertIsDisplayed()

    composeTestRule.onNodeWithTag("topic-name-dialog-confirm").performClick()
    assertEquals(SampleKnowledgeData.topics.first().title, confirmedName)
  }

  @Test
  fun managePane_validationMessage_showsErrorText() {
    composeTestRule.setContent {
      ArchiveAssistantTheme {
        ManagePane(
          topics = SampleKnowledgeData.topics,
          itemsByTopic = SampleKnowledgeData.items.groupBy { it.topicId },
          onBack = {},
          onTopicSelected = {},
          onCreateTopic = {},
          onRenameTopic = {},
          onDeleteTopic = {},
          onConfirmCreateTopic = {},
          onConfirmRenameTopic = {},
          onConfirmDeleteTopic = {},
          onCloseTopicNameDialog = {},
          onCloseDeleteConfirmDialog = {},
          topicNameDialogMode = TopicNameDialogMode.CREATE,
          topicNameDialogTopicId = null,
          topicValidationMessage = "请输入主题名称",
          deleteConfirmTopicId = null,
        )
      }
    }

    composeTestRule.onNodeWithText("请输入主题名称").assertIsDisplayed()
  }

  @Test
  fun managePane_deleteConfirmDialog_showsWhenDeleteConfirmTopicIdIsSet() {
    var confirmed = false
    var dismissed = false

    composeTestRule.setContent {
      ArchiveAssistantTheme {
        ManagePane(
          topics = SampleKnowledgeData.topics,
          itemsByTopic = SampleKnowledgeData.items.groupBy { it.topicId },
          onBack = {},
          onTopicSelected = {},
          onCreateTopic = {},
          onRenameTopic = {},
          onDeleteTopic = {},
          onConfirmCreateTopic = {},
          onConfirmRenameTopic = {},
          onConfirmDeleteTopic = { confirmed = true },
          onCloseTopicNameDialog = {},
          onCloseDeleteConfirmDialog = { dismissed = true },
          topicNameDialogMode = null,
          topicNameDialogTopicId = null,
          topicValidationMessage = null,
          deleteConfirmTopicId = SampleKnowledgeData.topics.first().id,
        )
      }
    }

    composeTestRule.onNodeWithTag("delete-confirm-dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("确认删除").assertIsDisplayed()

    composeTestRule.onNodeWithTag("delete-confirm-dialog-confirm").performClick()
    assertTrue(confirmed)

    composeTestRule.onNodeWithTag("delete-confirm-dialog-dismiss").performClick()
    assertTrue(dismissed)
  }
}
