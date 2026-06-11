package com.lyihub.archiveassistant.state

import com.lyihub.archiveassistant.domain.AiEngineSettings
import com.lyihub.archiveassistant.domain.AiEngineType
import com.lyihub.archiveassistant.domain.AppPane
import com.lyihub.archiveassistant.domain.ContentType
import com.lyihub.archiveassistant.domain.SampleKnowledgeData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveAssistantStateStoreTest {
    @Test
    fun initialState_exposesSeededDataAndSettings() {
        val store = ArchiveAssistantStateStore()

        assertEquals(AppPane.TOPICS, store.state.selectedPane)
        assertEquals(SampleKnowledgeData.topics, store.state.topics)
        assertEquals(SampleKnowledgeData.items, store.state.items)
        assertEquals(SampleKnowledgeData.defaultAiEngineSettings, store.state.aiSettings)
        assertEquals(
            SampleKnowledgeData.items.filter { it.topicId == SampleKnowledgeData.DefaultTopicId },
            store.state.itemsByTopic.getValue(SampleKnowledgeData.DefaultTopicId),
        )
    }

    @Test
    fun paneTransitions_openAndCloseExpectedSurfaces() {
        val store = ArchiveAssistantStateStore()

        store.openSettings()
        assertEquals(AppPane.SETTINGS, store.state.selectedPane)

        store.closePanes()
        assertEquals(AppPane.TOPICS, store.state.selectedPane)
        assertNull(store.state.selectedTopicId)

        store.openTopicManagement()
        assertEquals(AppPane.MANAGE, store.state.selectedPane)

        store.openTopic(SampleKnowledgeData.DefaultTopicId)
        assertEquals(AppPane.DETAIL, store.state.selectedPane)
        assertEquals(SampleKnowledgeData.DefaultTopicId, store.state.selectedTopicId)

        val itemId = store.state.selectedTopicItems.first().id
        store.openCardModal(itemId)
        assertEquals(AppPane.CARD_DETAIL, store.state.selectedPane)
        assertEquals(itemId, store.state.modalItem?.id)

        store.closeCardModal()
        assertEquals(AppPane.DETAIL, store.state.selectedPane)
        assertNull(store.state.modalItem)
    }

    @Test
    fun deleteTopic_whenActiveTopic_returnsToTopicsAndClearsItems() {
        val store = ArchiveAssistantStateStore()
        store.openTopic(SampleKnowledgeData.DefaultTopicId)

        store.deleteTopic(SampleKnowledgeData.DefaultTopicId)

        assertEquals(AppPane.TOPICS, store.state.selectedPane)
        assertNull(store.state.selectedTopicId)
        assertEquals(ContentType.ALL, store.state.activeDetailFilter)
        assertFalse(store.state.topics.any { it.id == SampleKnowledgeData.DefaultTopicId })
        assertFalse(store.state.items.any { it.topicId == SampleKnowledgeData.DefaultTopicId })
    }

    @Test
    fun filterSelection_updatesVisibleItemsForSelectedTopic() {
        val store = ArchiveAssistantStateStore()
        store.openTopic(SampleKnowledgeData.DefaultTopicId)

        store.selectFilter(ContentType.WEB_ARTICLE)

        assertEquals(ContentType.WEB_ARTICLE, store.state.activeDetailFilter)
        assertTrue(store.state.filteredSelectedTopicItems.isNotEmpty())
        assertTrue(store.state.filteredSelectedTopicItems.all { it.contentType == ContentType.WEB_ARTICLE })
    }

    @Test
    fun submitParserInput_whenClassified_addsItemAndClearsInputAndUpdatesTopicTimestamp() {
        val store = ArchiveAssistantStateStore()
        val initialItemCount = store.state.items.size
        val topicBefore = store.state.topics.first { it.id == "topic-ui-inspiration" }

        store.updateParserInput("UX screenshot image of a settings panel")
        store.submitParserInput()

        val newItem = store.state.items.last()
        assertEquals(initialItemCount + 1, store.state.items.size)
        assertEquals("item-classified-5", newItem.id)
        assertEquals("topic-ui-inspiration", newItem.topicId)
        assertEquals(ContentType.IMAGE_SCREENSHOT, newItem.contentType)
        assertEquals("", store.state.parserInput)
        assertNull(store.state.parserValidationMessage)
        assertEquals(AppPane.DETAIL, store.state.selectedPane)
        assertEquals("topic-ui-inspiration", store.state.selectedTopicId)

        val topicAfter = store.state.topics.first { it.id == "topic-ui-inspiration" }
        assertTrue(topicAfter.updatedAtEpochMillis > topicBefore.updatedAtEpochMillis)
    }

    @Test
    fun submitParserInput_whenBlank_keepsInputAndSetsValidation() {
        val store = ArchiveAssistantStateStore()

        store.updateParserInput("   ")
        store.submitParserInput()

        assertEquals("   ", store.state.parserInput)
        assertEquals("请输入要归档的内容", store.state.parserValidationMessage)
        assertEquals(SampleKnowledgeData.items, store.state.items)
    }

    @Test
    fun recentTopics_returnsTopThreeByUpdatedAtDescending() {
        val store = ArchiveAssistantStateStore()

        val recent = store.state.recentTopics
        assertEquals(3, recent.size)
        assertTrue(recent.zipWithNext { a, b -> a.updatedAtEpochMillis >= b.updatedAtEpochMillis }.all { it })
    }

    @Test
    fun recentTopics_afterClassification_reflectsUpdatedTopicAtTop() {
        val store = ArchiveAssistantStateStore()
        val topicId = "topic-anthropology-clips"
        val topicBefore = store.state.topics.first { it.id == topicId }
        assertTrue(store.state.recentTopics.any { it.id == topicId })

        store.updateParserInput("人类学笔记")
        store.submitParserInput()

        val recent = store.state.recentTopics
        assertEquals(topicId, recent.first().id)
        assertTrue(recent.first().updatedAtEpochMillis > topicBefore.updatedAtEpochMillis)
    }

    @Test
    fun topicCreateRenameAndSettingsUpdate_validateStateActions() {
        val store = ArchiveAssistantStateStore()

        store.createTopic("新主题")
        assertEquals("topic-user-5", store.state.selectedTopicId)
        assertEquals("新主题", store.state.selectedTopic?.title)

        store.renameTopic("topic-user-5", "重命名主题")
        assertEquals("重命名主题", store.state.selectedTopic?.title)

        store.createTopic("重命名主题")
        assertEquals("主题名称已存在", store.state.topicValidationMessage)

        store.createTopic("   ")
        assertEquals("请输入主题名称", store.state.topicValidationMessage)

        val settings = AiEngineSettings(engineType = AiEngineType.LOCAL_MODEL)
        store.updateAiSettings(settings)
        assertEquals(settings, store.state.aiSettings)
        assertNotNull(store.state.topicValidationMessage)
    }

    @Test
    fun openTopicManagementForCreate_navigatesToManageAndOpensCreateDialog() {
        val store = ArchiveAssistantStateStore()

        store.openTopicManagementForCreate()

        assertEquals(AppPane.MANAGE, store.state.selectedPane)
        assertEquals(TopicNameDialogMode.CREATE, store.state.topicNameDialogMode)
        assertNull(store.state.topicNameDialogTopicId)
        assertNull(store.state.topicValidationMessage)
    }

    @Test
    fun openCreateTopicDialog_setsCreateModeAndClearsValidation() {
        val store = ArchiveAssistantStateStore()
        store.createTopic("   ")
        assertNotNull(store.state.topicValidationMessage)

        store.openCreateTopicDialog()

        assertEquals(TopicNameDialogMode.CREATE, store.state.topicNameDialogMode)
        assertNull(store.state.topicNameDialogTopicId)
        assertNull(store.state.topicValidationMessage)
    }

    @Test
    fun openRenameTopicDialog_setsRenameModeAndTopicId() {
        val store = ArchiveAssistantStateStore()

        store.openRenameTopicDialog(SampleKnowledgeData.DefaultTopicId)

        assertEquals(TopicNameDialogMode.RENAME, store.state.topicNameDialogMode)
        assertEquals(SampleKnowledgeData.DefaultTopicId, store.state.topicNameDialogTopicId)
        assertNull(store.state.topicValidationMessage)
    }

    @Test
    fun confirmCreateTopic_whenValid_createsTopicAndClosesDialog() {
        val store = ArchiveAssistantStateStore()
        store.openCreateTopicDialog()

        store.confirmCreateTopic("新建主题")

        assertTrue(store.state.topics.any { it.title == "新建主题" })
        assertEquals(AppPane.DETAIL, store.state.selectedPane)
        assertNull(store.state.topicNameDialogMode)
    }

    @Test
    fun confirmCreateTopic_whenInvalid_keepsDialogOpenAndShowsValidation() {
        val store = ArchiveAssistantStateStore()
        store.openCreateTopicDialog()

        store.confirmCreateTopic("   ")

        assertEquals(TopicNameDialogMode.CREATE, store.state.topicNameDialogMode)
        assertEquals("请输入主题名称", store.state.topicValidationMessage)
    }

    @Test
    fun confirmRenameTopic_whenValid_renamesAndClosesDialog() {
        val store = ArchiveAssistantStateStore()
        store.openRenameTopicDialog(SampleKnowledgeData.DefaultTopicId)

        store.confirmRenameTopic("新名字")

        assertTrue(store.state.topics.any { it.id == SampleKnowledgeData.DefaultTopicId && it.title == "新名字" })
        assertNull(store.state.topicNameDialogMode)
    }

    @Test
    fun renameTopic_updatesTimestamp() {
        val store = ArchiveAssistantStateStore()
        val topicBefore = store.state.topics.first { it.id == SampleKnowledgeData.DefaultTopicId }

        store.renameTopic(SampleKnowledgeData.DefaultTopicId, "重命名后")

        val topicAfter = store.state.topics.first { it.id == SampleKnowledgeData.DefaultTopicId }
        assertEquals("重命名后", topicAfter.title)
        assertTrue(topicAfter.updatedAtEpochMillis > topicBefore.updatedAtEpochMillis)
    }

    @Test
    fun confirmRenameTopic_whenDuplicate_keepsDialogOpenAndShowsValidation() {
        val store = ArchiveAssistantStateStore()
        val existingTitle = store.state.topics.first { it.id != SampleKnowledgeData.DefaultTopicId }.title
        store.openRenameTopicDialog(SampleKnowledgeData.DefaultTopicId)

        store.confirmRenameTopic(existingTitle)

        assertEquals(TopicNameDialogMode.RENAME, store.state.topicNameDialogMode)
        assertEquals("主题名称已存在", store.state.topicValidationMessage)
    }

    @Test
    fun confirmDeleteTopic_deletesAndClosesDialog() {
        val store = ArchiveAssistantStateStore()
        store.openDeleteConfirmDialog(SampleKnowledgeData.DefaultTopicId)

        store.confirmDeleteTopic()

        assertNull(store.state.deleteConfirmTopicId)
        assertFalse(store.state.topics.any { it.id == SampleKnowledgeData.DefaultTopicId })
    }

    @Test
    fun closeDeleteConfirmDialog_clearsDialogWithoutDeleting() {
        val store = ArchiveAssistantStateStore()
        store.openDeleteConfirmDialog(SampleKnowledgeData.DefaultTopicId)

        store.closeDeleteConfirmDialog()

        assertNull(store.state.deleteConfirmTopicId)
        assertTrue(store.state.topics.any { it.id == SampleKnowledgeData.DefaultTopicId })
    }

    @Test
    fun filterSelection_documentPdf_updatesVisibleItemsForSelectedTopic() {
        val store = ArchiveAssistantStateStore()
        store.openTopic(SampleKnowledgeData.DefaultTopicId)

        store.selectFilter(ContentType.DOCUMENT_PDF)

        assertEquals(ContentType.DOCUMENT_PDF, store.state.activeDetailFilter)
        assertTrue(store.state.filteredSelectedTopicItems.isNotEmpty())
        assertTrue(store.state.filteredSelectedTopicItems.all { it.contentType == ContentType.DOCUMENT_PDF })
    }

    @Test
    fun closeCardModal_preservesSelectedFilterAndTopic() {
        val store = ArchiveAssistantStateStore()
        store.openTopic(SampleKnowledgeData.DefaultTopicId)
        store.selectFilter(ContentType.DOCUMENT_PDF)

        val itemId = store.state.filteredSelectedTopicItems.first().id
        store.openCardModal(itemId)
        assertEquals(AppPane.CARD_DETAIL, store.state.selectedPane)
        assertEquals(itemId, store.state.modalItem?.id)
        assertEquals(ContentType.DOCUMENT_PDF, store.state.activeDetailFilter)

        store.closeCardModal()
        assertEquals(AppPane.DETAIL, store.state.selectedPane)
        assertNull(store.state.modalItem)
        assertEquals(ContentType.DOCUMENT_PDF, store.state.activeDetailFilter)
        assertEquals(SampleKnowledgeData.DefaultTopicId, store.state.selectedTopicId)
    }

    @Test
    fun updateAiSettings_switchesEngineTypeAndUpdatesFields() {
        val store = ArchiveAssistantStateStore()
        assertEquals(AiEngineType.CLOUD_API, store.state.aiSettings.engineType)

        val localSettings = AiEngineSettings(
            engineType = AiEngineType.LOCAL_MODEL,
            localEndpoint = "http://localhost:8080",
            modelName = "qwen3-2b",
        )
        store.updateAiSettings(localSettings)

        assertEquals(AiEngineType.LOCAL_MODEL, store.state.aiSettings.engineType)
        assertEquals("http://localhost:8080", store.state.aiSettings.localEndpoint)
        assertEquals("qwen3-2b", store.state.aiSettings.modelName)
    }
}
