package com.lyihub.archiveassistant.state

import com.lyihub.archiveassistant.data.ModelDownloadManager
import com.lyihub.archiveassistant.domain.AiEngineSettings
import com.lyihub.archiveassistant.domain.AiEngineType
import com.lyihub.archiveassistant.domain.AppPane
import com.lyihub.archiveassistant.domain.ContentType
import com.lyihub.archiveassistant.domain.DocumentFormat
import com.lyihub.archiveassistant.domain.FakeLocalLlmEngine
import com.lyihub.archiveassistant.domain.InferenceBackend
import com.lyihub.archiveassistant.domain.LocalLlmEngine
import com.lyihub.archiveassistant.domain.LocalModelInfo
import com.lyihub.archiveassistant.domain.LocalModelState
import com.lyihub.archiveassistant.domain.LocalModelStatus
import com.lyihub.archiveassistant.domain.SampleKnowledgeData
import com.lyihub.archiveassistant.service.LocalInferenceGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.TestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveAssistantStateStoreTest {
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
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
        assertEquals("item-classified-6", newItem.id)
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
    fun showClipboard_whenPayloadIsBlank_doesNotOpenClipboardDialog() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard("   ", "   ")

        assertFalse(store.state.showClipboardDialog)
        assertNull(store.state.clipboardContent)
        assertNull(store.state.clipboardImageUri)
        assertNull(store.state.latestClipboardSnapshot)
    }

    @Test
    fun showClipboard_whenDismissedSamePayload_doesNotOpenAgainButCanBeReopened() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard("Clipboard note")
        store.dismissClipboardDialog()
        store.showClipboard("Clipboard note")

        assertFalse(store.state.showClipboardDialog)
        assertNull(store.state.clipboardContent)
        assertEquals("Clipboard note", store.state.latestClipboardSnapshot?.content)

        store.openLatestClipboardDialog()

        assertTrue(store.state.showClipboardDialog)
        assertEquals("Clipboard note", store.state.clipboardContent)
    }

    @Test
    fun showClipboard_whenDismissedPayloadChanges_opensAgain() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard("First note")
        store.dismissClipboardDialog()
        store.showClipboard("Second note")

        assertTrue(store.state.showClipboardDialog)
        assertEquals("Second note", store.state.clipboardContent)
    }

    @Test
    fun openLatestClipboardDialog_whenNoClipboardSeen_doesNothing() {
        val store = ArchiveAssistantStateStore()

        store.openLatestClipboardDialog()

        assertFalse(store.state.showClipboardDialog)
        assertNull(store.state.clipboardContent)
    }

    @Test
    fun acceptClipboardAndManualCreate_whenWebClipboard_opensAddItemDialogWithWebPrefill() {
        val store = ArchiveAssistantStateStore()
        store.openTopic(SampleKnowledgeData.DefaultTopicId)

        store.showClipboard("https://example.com/article")
        store.acceptClipboardAndManualCreate()

        assertFalse(store.state.showClipboardDialog)
        assertNull(store.state.clipboardContent)
        assertEquals(AppPane.DETAIL, store.state.selectedPane)
        assertEquals(SampleKnowledgeData.DefaultTopicId, store.state.selectedTopicId)
        assertTrue(store.state.addItemDialogVisible)
        assertEquals("", store.state.addItemDialogPrefill?.title)
        assertEquals(ContentType.WEB_ARTICLE, store.state.addItemDialogPrefill?.contentType)
        assertEquals("https://example.com/article", store.state.addItemDialogPrefill?.sourceUrl)
        assertFalse(store.state.addItemDialogPrefill?.lockContentType ?: true)
        assertEquals("https://example.com/article", store.state.addItemDialogPrefill?.textContent)
        assertEquals(
            listOf(ContentType.WEB_ARTICLE, ContentType.DOCUMENT),
            store.state.addItemDialogPrefill?.availableContentTypes,
        )
    }

    @Test
    fun acceptClipboardAndManualCreate_whenTextClipboard_prefillsDocumentTextContent() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard("Plain clipboard note")
        store.acceptClipboardAndManualCreate()

        assertEquals("", store.state.addItemDialogPrefill?.title)
        assertEquals(ContentType.DOCUMENT, store.state.addItemDialogPrefill?.contentType)
        assertEquals(DocumentFormat.MARKDOWN, store.state.addItemDialogPrefill?.documentFormat)
        assertEquals("Plain clipboard note", store.state.addItemDialogPrefill?.textContent)
        assertTrue(store.state.addItemDialogPrefill?.lockContentType ?: false)
        assertEquals(
            listOf(ContentType.DOCUMENT),
            store.state.addItemDialogPrefill?.availableContentTypes,
        )
    }

    @Test
    fun acceptClipboardAndManualCreate_whenTextContainsUrl_prefillsDocumentTextContent() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard("Read this https://example.com/article")
        store.acceptClipboardAndManualCreate()

        assertEquals(ContentType.DOCUMENT, store.state.addItemDialogPrefill?.contentType)
        assertNull(store.state.addItemDialogPrefill?.sourceUrl)
        assertEquals("Read this https://example.com/article", store.state.addItemDialogPrefill?.textContent)
    }

    @Test
    fun acceptClipboardAndManualCreate_whenImageOnlyClipboard_opensAddItemDialogWithImagePrefill() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard("", "content://clipboard/image")
        store.acceptClipboardAndManualCreate()

        assertFalse(store.state.showClipboardDialog)
        assertNull(store.state.clipboardImageUri)
        assertEquals(AppPane.DETAIL, store.state.selectedPane)
        assertEquals(store.state.recentTopics.first().id, store.state.selectedTopicId)
        assertTrue(store.state.addItemDialogVisible)
        assertEquals(ContentType.IMAGE_SCREENSHOT, store.state.addItemDialogPrefill?.contentType)
        assertEquals("content://clipboard/image", store.state.addItemDialogPrefill?.sourceUrl)
        assertTrue(store.state.addItemDialogPrefill?.lockContentType ?: false)
        assertNull(store.state.addItemDialogPrefill?.availableContentTypes)
    }

    @Test
    fun acceptClipboardAndManualCreate_whenMixedImageTextClipboard_locksPrefillToDocument() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard("Caption and notes", "content://clipboard/image")
        store.acceptClipboardAndManualCreate()

        assertTrue(store.state.addItemDialogVisible)
        assertEquals(ContentType.DOCUMENT, store.state.addItemDialogPrefill?.contentType)
        assertEquals("Caption and notes", store.state.addItemDialogPrefill?.title)
        assertNull(store.state.addItemDialogPrefill?.sourceUrl)
        assertEquals(DocumentFormat.MARKDOWN, store.state.addItemDialogPrefill?.documentFormat)
        assertTrue(store.state.addItemDialogPrefill?.lockContentType ?: false)
        assertEquals(
            listOf(ContentType.DOCUMENT),
            store.state.addItemDialogPrefill?.availableContentTypes,
        )
    }

    @Test
    fun acceptClipboardAndManualCreate_whenDocumentClipboard_opensAddItemDialogWithDocumentPrefill() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard(
            content = "paper.pdf",
            sourceUri = "content://clipboard/document",
            sourceContentType = ContentType.DOCUMENT,
            sourceDocumentFormat = DocumentFormat.PDF,
            sourceFileName = "paper.pdf",
        )
        store.acceptClipboardAndManualCreate()

        assertFalse(store.state.showClipboardDialog)
        assertNull(store.state.clipboardSourceUri)
        assertTrue(store.state.addItemDialogVisible)
        assertEquals(ContentType.DOCUMENT, store.state.addItemDialogPrefill?.contentType)
        assertEquals("content://clipboard/document", store.state.addItemDialogPrefill?.sourceUrl)
        assertEquals(DocumentFormat.PDF, store.state.addItemDialogPrefill?.documentFormat)
        assertEquals("paper.pdf", store.state.addItemDialogPrefill?.fileName)
        assertTrue(store.state.addItemDialogPrefill?.lockContentType ?: false)
    }

    @Test
    fun confirmAddItem_usesDialogSelectedTopic() {
        val store = ArchiveAssistantStateStore()
        val targetTopicId = store.state.topics.first { it.id != SampleKnowledgeData.DefaultTopicId }.id
        store.openTopic(SampleKnowledgeData.DefaultTopicId)

        store.confirmAddItem(
            topicId = targetTopicId,
            title = "Selected topic item",
            contentType = ContentType.WEB_ARTICLE,
            sourceUrl = "https://example.com/selected-topic",
            summary = "",
            useAiSummary = true,
        )

        val newItem = store.state.items.last()
        assertEquals(targetTopicId, newItem.topicId)
        assertEquals(targetTopicId, store.state.selectedTopicId)
    }

    @Test
    fun recentTopics_returnsTopFiveByUpdatedAtDescending() {
        val store = ArchiveAssistantStateStore()

        val recent = store.state.recentTopics
        assertEquals(5, recent.size)
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
        assertEquals("topic-user-6", store.state.selectedTopicId)
        assertEquals("新主题", store.state.selectedTopic?.title)

        store.renameTopic("topic-user-6", "重命名主题")
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

        store.selectFilter(ContentType.DOCUMENT)

        assertEquals(ContentType.DOCUMENT, store.state.activeDetailFilter)
        assertTrue(store.state.filteredSelectedTopicItems.isNotEmpty())
        assertTrue(store.state.filteredSelectedTopicItems.all { it.contentType == ContentType.DOCUMENT })
    }

    @Test
    fun closeCardModal_preservesSelectedFilterAndTopic() {
        val store = ArchiveAssistantStateStore()
        store.openTopic(SampleKnowledgeData.DefaultTopicId)
        store.selectFilter(ContentType.DOCUMENT)

        val itemId = store.state.filteredSelectedTopicItems.first().id
        store.openCardModal(itemId)
        assertEquals(AppPane.CARD_DETAIL, store.state.selectedPane)
        assertEquals(itemId, store.state.modalItem?.id)
        assertEquals(ContentType.DOCUMENT, store.state.activeDetailFilter)

        store.closeCardModal()
        assertEquals(AppPane.DETAIL, store.state.selectedPane)
        assertNull(store.state.modalItem)
        assertEquals(ContentType.DOCUMENT, store.state.activeDetailFilter)
        assertEquals(SampleKnowledgeData.DefaultTopicId, store.state.selectedTopicId)
    }

    @Test
    fun updateAiSettings_switchesEngineTypeAndUpdatesFields() {
        val store = ArchiveAssistantStateStore()
        assertEquals(AiEngineType.OPENAI_COMPATIBLE, store.state.aiSettings.engineType)

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

    @Test
    fun showClipboard_withSourceLabel_storesSourceLabelAndOpensDialog() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard(content = "test", sourceLabel = "拖拽")

        assertTrue(store.state.showClipboardDialog)
        assertEquals("拖拽", store.state.clipboardSourceLabel)
        assertEquals("test", store.state.clipboardContent)
    }

    @Test
    fun dismissClipboardDialog_clearsSourceLabel() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard(content = "test", sourceLabel = "拖拽")
        assertTrue(store.state.showClipboardDialog)
        assertEquals("拖拽", store.state.clipboardSourceLabel)

        store.dismissClipboardDialog()

        assertFalse(store.state.showClipboardDialog)
        assertNull(store.state.clipboardSourceLabel)
        assertNull(store.state.clipboardContent)
    }

    @Test
    fun showClipboard_withDifferentSourceLabel_reopensAfterDismiss() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard(content = "same content", sourceLabel = "拖拽")
        assertTrue(store.state.showClipboardDialog)
        store.dismissClipboardDialog()
        assertFalse(store.state.showClipboardDialog)

        store.showClipboard(content = "same content", sourceLabel = "剪切板")
        assertTrue(store.state.showClipboardDialog)
        assertEquals("剪切板", store.state.clipboardSourceLabel)
    }

    @Test
    fun showClipboard_withFileMetadata_storesAllFields() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard(
            content = "",
            sourceUri = "content://test/image.png",
            sourceContentType = ContentType.IMAGE_SCREENSHOT,
            sourceFileName = "image.png",
            sourceLabel = "拖拽",
        )

        assertTrue(store.state.showClipboardDialog)
        assertEquals("content://test/image.png", store.state.clipboardSourceUri)
        assertEquals(ContentType.IMAGE_SCREENSHOT, store.state.clipboardSourceContentType)
        assertEquals("image.png", store.state.clipboardSourceFileName)
        assertEquals("拖拽", store.state.clipboardSourceLabel)
    }

    @Test
    fun acceptClipboardAndManualCreate_withDragSource_opensAddItemDialog() {
        val store = ArchiveAssistantStateStore()
        store.createTopic("Drag test topic")

        store.showClipboard(
            content = "test.pdf",
            sourceUri = "content://test/test.pdf",
            sourceContentType = ContentType.DOCUMENT,
            sourceDocumentFormat = DocumentFormat.PDF,
            sourceFileName = "test.pdf",
            sourceLabel = "拖拽",
        )
        assertTrue(store.state.showClipboardDialog)

        store.acceptClipboardAndManualCreate()

        assertTrue(store.state.addItemDialogVisible)
        assertNotNull(store.state.addItemDialogPrefill)
        assertEquals("test.pdf", store.state.addItemDialogPrefill?.title)
        assertFalse(store.state.showClipboardDialog)
        assertNull(store.state.clipboardSourceLabel)
        assertEquals(store.state.items.size, SampleKnowledgeData.items.size)
    }

    @Test
    fun acceptClipboardAndSummarize_withDragSource_clearsClipboardState() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard(content = "some text", sourceLabel = "拖拽")
        assertTrue(store.state.showClipboardDialog)

        store.acceptClipboardAndSummarize()

        assertFalse(store.state.showClipboardDialog)
        assertNull(store.state.clipboardSourceLabel)
        assertEquals("", store.state.parserInput)
    }

    @Test
    fun dismissClipboardDialog_withDragSource_clearsAndSetsIgnoredSnapshot() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard(content = "text", sourceLabel = "拖拽")
        assertTrue(store.state.showClipboardDialog)

        store.dismissClipboardDialog()

        assertFalse(store.state.showClipboardDialog)
        assertNull(store.state.clipboardSourceLabel)
        assertNotNull(store.state.ignoredClipboardSnapshot)
        assertEquals("拖拽", store.state.ignoredClipboardSnapshot?.sourceLabel)
    }

    @Test
    fun showClipboard_sameDragContentAfterDismiss_doesNotReopen() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard(content = "text", sourceLabel = "拖拽")
        assertTrue(store.state.showClipboardDialog)
        store.dismissClipboardDialog()
        assertFalse(store.state.showClipboardDialog)

        store.showClipboard(content = "text", sourceLabel = "拖拽")

        assertFalse(store.state.showClipboardDialog)
    }

    @Test
    fun showClipboard_sameContentDifferentSourceLabel_reopensAfterDismiss() {
        val store = ArchiveAssistantStateStore()

        store.showClipboard(content = "text", sourceLabel = "拖拽")
        assertTrue(store.state.showClipboardDialog)
        store.dismissClipboardDialog()
        assertFalse(store.state.showClipboardDialog)

        store.showClipboard(content = "text", sourceLabel = null)

        assertTrue(store.state.showClipboardDialog)
    }

    @Test
    fun releaseDragPermission_invokedOnDismiss() {
        val store = ArchiveAssistantStateStore()
        var released = false
        store.releaseDragPermission = { released = true }

        store.showClipboard(content = "text", sourceLabel = "拖拽")
        store.dismissClipboardDialog()

        assertTrue(released)
        assertNull(store.releaseDragPermission)
    }

    @Test
    fun acceptClipboardAndSummarize_withNullContent_doesNotReleasePermission() {
        val store = ArchiveAssistantStateStore()
        var released = false
        store.releaseDragPermission = { released = true }

        store.showClipboard(
            content = "",
            sourceUri = "content://test/image.png",
            sourceContentType = ContentType.IMAGE_SCREENSHOT,
            sourceLabel = "拖拽",
        )
        store.acceptClipboardAndSummarize()

        assertFalse(released)
        assertNotNull(store.releaseDragPermission)
        assertTrue(store.state.showClipboardDialog)
    }

    @Test
    fun releaseDragPermission_invokedOnManualCreateConfirm() {
        val store = ArchiveAssistantStateStore()
        store.openTopic(SampleKnowledgeData.DefaultTopicId)
        var released = false
        store.releaseDragPermission = { released = true }

        store.showClipboard(
            content = "drag.docx",
            sourceUri = "content://test/drag.docx",
            sourceContentType = ContentType.DOCUMENT,
            sourceDocumentFormat = DocumentFormat.DOCX,
            sourceFileName = "drag.docx",
            sourceLabel = "拖拽",
        )
        store.acceptClipboardAndManualCreate()

        assertFalse(released)
        assertNotNull(store.releaseDragPermission)

        store.confirmAddItem(
            topicId = SampleKnowledgeData.DefaultTopicId,
            title = "drag.docx",
            contentType = ContentType.DOCUMENT,
            sourceUrl = "content://test/drag.docx",
            summary = "summary",
            useAiSummary = false,
            documentFormat = DocumentFormat.DOCX,
            fileName = "drag.docx",
        )

        assertTrue(released)
        assertNull(store.releaseDragPermission)
    }

    @Test
    fun localModelFullFlow() {
        val downloadManager = FakeModelDownloadManager()
        val engine = FakeLocalLlmEngine().also {
            runBlocking { it.initialize("/tmp/model", InferenceBackend.CPU) }
        }
        val inferenceConnection = FakeLocalInferenceGateway(engine)
        val store = localStore(downloadManager, inferenceConnection)

        assertEquals(LocalModelStatus.NOT_DOWNLOADED, store.state.localModelState.status)

        store.downloadModel()
        downloadManager.emit(LocalModelState(status = LocalModelStatus.DOWNLOADING))
        waitUntil { store.state.localModelState.status == LocalModelStatus.DOWNLOADING }
        downloadManager.emit(LocalModelState(status = LocalModelStatus.DOWNLOADED))
        waitUntil { store.state.localModelState.status == LocalModelStatus.DOWNLOADED }

        store.startModel()
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.INITIALIZING))
        waitUntil { store.state.localModelState.status == LocalModelStatus.INITIALIZING }
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.READY, activeBackend = InferenceBackend.CPU))
        waitUntil { store.state.localModelState.status == LocalModelStatus.READY }

        store.updateAiSettings(AiEngineSettings(engineType = AiEngineType.LOCAL_MODEL))
        store.updateParserInput("local inference note")
        store.submitParserInput()
        assertEquals(LocalModelStatus.READY, store.state.localModelState.status)

        store.stopModel()
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.STOPPING))
        waitUntil { store.state.localModelState.status == LocalModelStatus.STOPPING }
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.DOWNLOADED))
        waitUntil { store.state.localModelState.status == LocalModelStatus.DOWNLOADED }
    }

    @Test
    fun concurrentStartIgnored() {
        val inferenceConnection = FakeLocalInferenceGateway(FakeLocalLlmEngine())
        val store = localStore(inferenceConnection = inferenceConnection)
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.INITIALIZING))
        waitUntil { store.state.localModelState.status == LocalModelStatus.INITIALIZING }

        store.startModel()

        assertEquals(0, inferenceConnection.startCount)
    }

    @Test
    fun stopModelDuringStartModel_handlesGracefully() {
        val inferenceConnection = FakeLocalInferenceGateway(FakeLocalLlmEngine())
        val store = localStore(inferenceConnection = inferenceConnection)

        store.startModel()
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.INITIALIZING))
        waitUntil { store.state.localModelState.status == LocalModelStatus.INITIALIZING }
        store.stopModel()
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.STOPPING))
        waitUntil { store.state.localModelState.status == LocalModelStatus.STOPPING }
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.DOWNLOADED))
        waitUntil { store.state.localModelState.status == LocalModelStatus.DOWNLOADED }

        assertEquals(1, inferenceConnection.startCount)
        assertEquals(1, inferenceConnection.stopCount)
    }

    @Test
    fun inferencingStateRejectsNewInferenceWithMessage() {
        val store = localStore(
            inferenceConnection = FakeLocalInferenceGateway(FakeLocalLlmEngine()),
            localModelStateProvider = { LocalModelState(status = LocalModelStatus.INFERENCING) },
        )
        store.updateAiSettings(AiEngineSettings(engineType = AiEngineType.LOCAL_MODEL))
        store.updateParserInput("local inference note")

        store.submitParserInput()

        assertEquals("推理进行中", store.state.parserValidationMessage)
    }

    @Test
    fun downloadFailureThenRetry() {
        val downloadManager = FakeModelDownloadManager()
        val store = localStore(downloadManager = downloadManager)

        store.downloadModel()
        downloadManager.emit(LocalModelState(status = LocalModelStatus.ERROR, errorMessage = "network failed"))
        waitUntil { store.state.localModelState.status == LocalModelStatus.ERROR }
        store.downloadModel()
        downloadManager.emit(LocalModelState(status = LocalModelStatus.DOWNLOADING))
        waitUntil { store.state.localModelState.status == LocalModelStatus.DOWNLOADING }
        downloadManager.emit(LocalModelState(status = LocalModelStatus.DOWNLOADED))
        waitUntil { store.state.localModelState.status == LocalModelStatus.DOWNLOADED }

        assertEquals(2, downloadManager.startCount)
    }

    @Test
    fun engineSwitchStopsModel() {
        val inferenceConnection = FakeLocalInferenceGateway(FakeLocalLlmEngine())
        val store = localStore(inferenceConnection = inferenceConnection)
        store.updateAiSettings(AiEngineSettings(engineType = AiEngineType.LOCAL_MODEL))
        store.startModel()
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.INITIALIZING))
        waitUntil { store.state.localModelState.status == LocalModelStatus.INITIALIZING }
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.READY))
        waitUntil { store.state.localModelState.status == LocalModelStatus.READY }

        store.updateAiSettings(store.state.aiSettings.copy(engineType = AiEngineType.OPENAI_COMPATIBLE))

        assertEquals(1, inferenceConnection.stopCount)
    }

    @Test
    fun backendPreferenceSwitchWhenReadyStopsModelBeforeRestart() {
        val inferenceConnection = FakeLocalInferenceGateway(FakeLocalLlmEngine())
        val store = localStore(inferenceConnection = inferenceConnection)
        store.updateAiSettings(AiEngineSettings(engineType = AiEngineType.LOCAL_MODEL))
        store.startModel()
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.INITIALIZING))
        waitUntil { store.state.localModelState.status == LocalModelStatus.INITIALIZING }
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.READY, activeBackend = InferenceBackend.CPU))
        waitUntil { store.state.localModelState.status == LocalModelStatus.READY }

        store.updateAiSettings(store.state.aiSettings.copy(localBackendPreference = InferenceBackend.GPU))
        store.stopModel()
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.STOPPING))
        waitUntil { store.state.localModelState.status == LocalModelStatus.STOPPING }
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.DOWNLOADED))
        waitUntil { store.state.localModelState.status == LocalModelStatus.DOWNLOADED }
        store.startModel()

        assertEquals(1, inferenceConnection.stopCount)
        assertEquals(2, inferenceConnection.startCount)
        assertEquals(InferenceBackend.GPU, inferenceConnection.lastBackend)
    }

    @Test
    fun restoreAfterDeath() {
        val downloadedStore = localStore(modelFileExists = true)
        val missingStore = localStore(modelFileExists = false)

        assertEquals(LocalModelStatus.DOWNLOADED, downloadedStore.state.localModelState.status)
        assertEquals(LocalModelStatus.NOT_DOWNLOADED, missingStore.state.localModelState.status)
    }

    @Test
    fun modelFileDeleted() {
        val store = localStore(modelFileExists = false)
        store.updateAiSettings(AiEngineSettings(engineType = AiEngineType.LOCAL_MODEL))
        store.updateParserInput("local inference note")

        store.submitParserInput()

        assertEquals(LocalModelStatus.NOT_DOWNLOADED, store.state.localModelState.status)
        assertEquals("本地模型未就绪，请先开启模型", store.state.parserValidationMessage)
    }

    @Test
    fun benchmarkSuccess() {
        val engine = FakeLocalLlmEngine().also {
            runBlocking { it.initialize("/tmp/model", InferenceBackend.CPU) }
            it.delayMillis = 100L
        }
        val inferenceConnection = FakeLocalInferenceGateway(engine)
        val store = localStore(inferenceConnection = inferenceConnection, modelFileExists = true)
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.READY))
        waitUntil { store.state.localModelState.status == LocalModelStatus.READY }

        store.runBenchmark()
        waitUntil { store.state.isBenchmarkRunning }
        waitUntil { !store.state.isBenchmarkRunning && store.state.benchmarkResult != null }

        assertNotNull(store.state.benchmarkResult)
        assertTrue((store.state.benchmarkResult?.decodeTokensPerSecond ?: 0f) > 0f)
    }

    @Test
    fun benchmarkIgnoredWhenNotReady() {
        val store = localStore(inferenceConnection = FakeLocalInferenceGateway(FakeLocalLlmEngine()))

        store.runBenchmark()

        assertFalse(store.state.isBenchmarkRunning)
        assertNull(store.state.benchmarkResult)
    }

    @Test
    fun benchmarkFailure() {
        val engine = FakeLocalLlmEngine().also {
            runBlocking { it.initialize("/tmp/model", InferenceBackend.CPU) }
            it.delayMillis = 100L
            it.benchmarkFailure = IllegalStateException("benchmark failed")
        }
        val inferenceConnection = FakeLocalInferenceGateway(engine)
        val store = localStore(inferenceConnection = inferenceConnection, modelFileExists = true)
        inferenceConnection.emit(LocalModelState(status = LocalModelStatus.READY))
        waitUntil { store.state.localModelState.status == LocalModelStatus.READY }

        store.runBenchmark()
        waitUntil { store.state.isBenchmarkRunning }
        waitUntil { !store.state.isBenchmarkRunning }

        assertNull(store.state.benchmarkResult)
    }

    private fun localStore(
        downloadManager: FakeModelDownloadManager = FakeModelDownloadManager(),
        inferenceConnection: FakeLocalInferenceGateway = FakeLocalInferenceGateway(FakeLocalLlmEngine()),
        modelFileExists: Boolean = false,
        localModelStateProvider: (() -> LocalModelState)? = null,
    ) = ArchiveAssistantStateStore(
        modelDownloadManager = downloadManager,
        inferenceConnection = inferenceConnection,
        localModelFileExists = { modelFileExists },
        localModelStateProvider = localModelStateProvider,
    )

    private fun waitUntil(assertion: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000L
        while (System.currentTimeMillis() < deadline) {
            // Pump the DefaultExecutor/IO dispatcher's event loop so that
            // collectLatest coroutines launched from the store's init block
            // (which run on Dispatchers.IO) get a chance to process flow
            // emissions before we check state.
            runBlocking(Dispatchers.IO) { }
            if (assertion()) return
            Thread.sleep(10L)
        }
        assertTrue(assertion())
    }

    private class FakeModelDownloadManager : ModelDownloadManager {
        private val state = MutableStateFlow(LocalModelState(status = LocalModelStatus.NOT_DOWNLOADED))
        var startCount = 0
        var cancelCount = 0
        var modelPresent = false

        override val downloadState: Flow<LocalModelState> = state

        override suspend fun startDownload(model: LocalModelInfo): Result<Unit> {
            startCount++
            return Result.success(Unit)
        }

        override suspend fun cancelDownload() {
            cancelCount++
            emit(LocalModelState(status = LocalModelStatus.NOT_DOWNLOADED))
        }

        override suspend fun deleteModel(model: LocalModelInfo): Result<Unit> = Result.success(Unit)

        override fun isModelPresent(model: LocalModelInfo): Boolean = modelPresent

        fun emit(localModelState: LocalModelState) {
            state.value = localModelState
            modelPresent = localModelState.status == LocalModelStatus.DOWNLOADED || modelPresent
        }
    }

    private class FakeLocalInferenceGateway(
        private val engine: LocalLlmEngine?,
    ) : LocalInferenceGateway {
        private val state = MutableStateFlow(LocalModelState(status = LocalModelStatus.DOWNLOADED))
        var bindCount = 0
        var unbindCount = 0
        var startCount = 0
        var stopCount = 0
        var lastBackend: InferenceBackend? = null

        override val serviceState: Flow<LocalModelState> = state

        override fun bind() {
            bindCount++
        }

        override fun unbind() {
            unbindCount++
        }

        override fun getEngine(): LocalLlmEngine? = engine

        override fun startModel(model: LocalModelInfo, backend: InferenceBackend) {
            startCount++
            lastBackend = backend
        }

        override fun stopModel() {
            stopCount++
        }

        fun emit(localModelState: LocalModelState) {
            state.value = localModelState
        }
    }
}
