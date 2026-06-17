package com.lyihub.archiveassistant.state

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lyihub.archiveassistant.data.AppDataRepository
import com.lyihub.archiveassistant.domain.AiEngineSettings
import com.lyihub.archiveassistant.domain.AppPane
import com.lyihub.archiveassistant.domain.ClassificationResult
import com.lyihub.archiveassistant.domain.ContentType
import com.lyihub.archiveassistant.domain.DocumentFormat
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.MockKnowledgeClassifier
import com.lyihub.archiveassistant.domain.SampleKnowledgeData
import com.lyihub.archiveassistant.domain.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class ArchiveAssistantStateStore(
    private val classifier: MockKnowledgeClassifier = MockKnowledgeClassifier(),
    private val initialState: ArchiveAssistantState = ArchiveAssistantState(
        topics = SampleKnowledgeData.topics,
        items = SampleKnowledgeData.items,
        aiSettings = SampleKnowledgeData.defaultAiEngineSettings,
    ),
    private val appDataRepository: AppDataRepository? = null,
    androidContext: Context? = null,
) {
    private val appContext = androidContext
    private val scope = CoroutineScope(Dispatchers.IO)

    var state: ArchiveAssistantState by mutableStateOf(
        resolveMockImagePaths(
            appDataRepository?.let(::loadPersistedState) ?: initialState
        )
    )
        private set

    private var nextTopicIndex = deriveNextTopicIndex(state.topics)
    private var nextItemIndex = deriveNextItemIndex(state.items)

    private fun loadPersistedState(repo: AppDataRepository): ArchiveAssistantState {
        val persistedTopics = runBlocking { repo.loadTopics() }
        val persistedItems = runBlocking { repo.loadItems() }
        return if (persistedTopics.isNotEmpty() || persistedItems.isNotEmpty()) {
            initialState.copy(
                topics = persistedTopics,
                items = persistedItems,
            )
        } else {
            initialState
        }
    }

    private fun saveData() {
        val repo = appDataRepository ?: return
        scope.launch {
            repo.saveAll(state.topics, state.items)
        }
    }

    private fun resolveMockImagePaths(state: ArchiveAssistantState): ArchiveAssistantState {
        val context = appContext ?: return state
        val mapping = mapOf(
            "item-transformer-diagram" to "transformer_architecture",
        )
        val itemsDir = File(context.filesDir, "items").also { it.mkdirs() }
        return state.copy(
            items = state.items.map { item ->
                val drawableName = mapping[item.id] ?: return@map item
                val dest = File(itemsDir, "$drawableName.png")
                if (!dest.exists()) {
                    val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                    if (resId != 0) {
                        context.resources.openRawResource(resId).use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
                item.copy(sourceUrl = dest.absolutePath)
            }
        )
    }

    private fun deriveNextTopicIndex(topics: List<Topic>): Int {
        var maxIdx = -1
        topics.forEach { topic ->
            val parts = topic.id.removePrefix("topic-").split("-")
            if (parts.size >= 2) parts.last().toIntOrNull()?.let { if (it > maxIdx) maxIdx = it }
        }
        return (if (maxIdx >= 0) maxIdx else topics.size) + 1
    }

    private fun deriveNextItemIndex(items: List<KnowledgeItem>): Int {
        var maxIdx = -1
        items.forEach { item ->
            val parts = item.id.removePrefix("item-").split("-")
            if (parts.size >= 2) parts.last().toIntOrNull()?.let { if (it > maxIdx) maxIdx = it }
        }
        return (if (maxIdx >= 0) maxIdx else items.size) + 1
    }

    fun closePanes() {
        state = state.copy(
            selectedPane = AppPane.TOPICS,
            selectedTopicId = null,
            modalItem = null,
            activeDetailFilter = ContentType.ALL,
        )
    }

    fun openSettings() {
        state = state.copy(selectedPane = AppPane.SETTINGS, modalItem = null)
    }

    fun openTopicManagement() {
        state = state.copy(selectedPane = AppPane.MANAGE, selectedTopicId = null, modalItem = null)
    }

    fun openTopicManagementForCreate() {
        state = state.copy(selectedPane = AppPane.MANAGE, selectedTopicId = null, modalItem = null)
        openCreateTopicDialog()
    }

    fun openCreateTopicDialog() {
        state = state.copy(
            topicNameDialogMode = com.lyihub.archiveassistant.state.TopicNameDialogMode.CREATE,
            topicNameDialogTopicId = null,
            topicValidationMessage = null,
        )
    }

    fun openRenameTopicDialog(topicId: String) {
        state = state.copy(
            topicNameDialogMode = com.lyihub.archiveassistant.state.TopicNameDialogMode.RENAME,
            topicNameDialogTopicId = topicId,
            topicValidationMessage = null,
        )
    }

    fun closeTopicNameDialog() {
        state = state.copy(
            topicNameDialogMode = null,
            topicNameDialogTopicId = null,
            topicValidationMessage = null,
        )
    }

    fun confirmCreateTopic(title: String) {
        createTopic(title)
        if (state.topicValidationMessage == null) {
            closeTopicNameDialog()
        }
    }

    fun confirmRenameTopic(title: String) {
        val topicId = state.topicNameDialogTopicId ?: return
        renameTopic(topicId, title)
        if (state.topicValidationMessage == null) {
            closeTopicNameDialog()
        }
    }

    fun openDeleteConfirmDialog(topicId: String) {
        state = state.copy(deleteConfirmTopicId = topicId)
    }

    fun closeDeleteConfirmDialog() {
        state = state.copy(deleteConfirmTopicId = null)
    }

    fun openAddItemDialog() {
        state = state.copy(
            addItemDialogVisible = true,
            addItemDialogValidationMessage = null,
            addItemDialogPrefill = null,
        )
    }

    fun closeAddItemDialog() {
        state = state.copy(
            addItemDialogVisible = false,
            addItemDialogValidationMessage = null,
            addItemDialogPrefill = null,
        )
    }

    fun confirmAddItem(
        topicId: String,
        title: String,
        contentType: ContentType,
        sourceUrl: String?,
        summary: String,
        useAiSummary: Boolean,
        documentFormat: DocumentFormat? = null,
        fileName: String? = null,
    ) {
        val normalizedTitle = title.trim()
        val normalizedSummary = summary.trim()
        val normalizedSourceUrl = sourceUrl?.trim()?.takeIf { it.isNotBlank() }

        val validationMessage = when {
            state.topics.none { it.id == topicId } -> "请选择归属主题"
            normalizedTitle.isBlank() -> "请输入资料标题"
            contentType == ContentType.WEB_ARTICLE && normalizedSourceUrl == null -> "请输入链接"
            (contentType == ContentType.IMAGE_SCREENSHOT || contentType == ContentType.DOCUMENT)
                && normalizedSourceUrl == null -> "请选择文件"
            else -> null
        }

        if (validationMessage != null) {
            state = state.copy(addItemDialogValidationMessage = validationMessage)
            return
        }

        val itemIndex = nextItemIndex++
        val now = System.currentTimeMillis()
        val finalSummary = if (useAiSummary) "" else normalizedSummary
        val item = KnowledgeItem(
            id = "item-user-$itemIndex",
            topicId = topicId,
            contentType = contentType,
            tag = contentType.label,
            title = normalizedTitle,
            summary = finalSummary,
            fullText = finalSummary,
            sourceUrl = normalizedSourceUrl,
            documentFormat = documentFormat,
            fileName = fileName,
            createdAtEpochMillis = now,
        )
        state = state.copy(
            items = state.items + item,
            topics = state.topics.map { topic ->
                if (topic.id == topicId) topic.copy(updatedAtEpochMillis = now) else topic
            },
            selectedPane = AppPane.DETAIL,
            selectedTopicId = topicId,
            activeDetailFilter = ContentType.ALL,
            addItemDialogVisible = false,
            addItemDialogValidationMessage = null,
            addItemDialogPrefill = null,
        )
        saveData()
    }

    fun confirmDeleteTopic() {
        val topicId = state.deleteConfirmTopicId ?: return
        closeDeleteConfirmDialog()
        deleteTopic(topicId)
    }

    fun deleteItem(itemId: String) {
        val deletingModalItem = state.modalItem?.id == itemId
        state = state.copy(
            items = state.items.filterNot { it.id == itemId },
            modalItem = if (deletingModalItem) null else state.modalItem,
            selectedPane = if (deletingModalItem && state.selectedPane == AppPane.CARD_DETAIL)
                state.selectedPane.let { if (state.selectedTopicId != null) AppPane.DETAIL else AppPane.TOPICS }
            else state.selectedPane,
        )
        saveData()
    }

    fun openDeleteItemConfirmDialog(itemId: String) {
        state = state.copy(deleteConfirmItemId = itemId)
    }

    fun closeDeleteItemConfirmDialog() {
        state = state.copy(deleteConfirmItemId = null)
    }

    fun confirmDeleteItem() {
        val itemId = state.deleteConfirmItemId ?: return
        closeDeleteItemConfirmDialog()
        deleteItem(itemId)
    }

    fun openEditItemDialog(itemId: String) {
        val item = state.items.firstOrNull { it.id == itemId } ?: return
        state = state.copy(editingItem = item, editItemDialogValidationMessage = null)
    }

    fun closeEditItemDialog() {
        state = state.copy(editingItem = null, editItemDialogValidationMessage = null)
    }

    fun confirmEditItem(
        title: String,
        contentType: ContentType,
        sourceUrl: String?,
        summary: String,
        useAiSummary: Boolean,
        documentFormat: DocumentFormat? = null,
        fileName: String? = null,
    ) {
        val originalItem = state.editingItem ?: return
        val normalizedTitle = title.trim()
        val normalizedSummary = summary.trim()
        val normalizedSourceUrl = sourceUrl?.trim()?.takeIf { it.isNotBlank() }

        val validationMessage = when {
            normalizedTitle.isBlank() -> "请输入资料标题"
            else -> null
        }

        if (validationMessage != null) {
            state = state.copy(editItemDialogValidationMessage = validationMessage)
            return
        }

        val finalSummary = if (useAiSummary) "" else normalizedSummary
        val updatedItem = originalItem.copy(
            contentType = contentType,
            tag = contentType.label,
            title = normalizedTitle,
            summary = finalSummary,
            fullText = finalSummary,
            sourceUrl = normalizedSourceUrl,
            documentFormat = documentFormat,
            fileName = fileName,
        )
        val now = System.currentTimeMillis()
        state = state.copy(
            items = state.items.map { if (it.id == originalItem.id) updatedItem else it },
            topics = state.topics.map { topic ->
                if (topic.id == originalItem.topicId) topic.copy(updatedAtEpochMillis = now) else topic
            },
            editingItem = null,
            editItemDialogValidationMessage = null,
            modalItem = if (state.modalItem?.id == originalItem.id) updatedItem else state.modalItem,
        )
        saveData()
    }

    fun openTopic(topicId: String) {
        if (state.topics.none { it.id == topicId }) return

        state = state.copy(
            selectedPane = AppPane.DETAIL,
            selectedTopicId = topicId,
            activeDetailFilter = ContentType.ALL,
            modalItem = null,
        )
    }

    fun updateParserInput(input: String) {
        state = state.copy(parserInput = input, parserValidationMessage = null)
    }

    fun updateHomeSearchQuery(query: String) {
        state = state.copy(homeSearchQuery = query)
    }

    fun submitParserInput() {
        when (val result = classifier.classify(state.parserInput, state.topics)) {
            is ClassificationResult.BlankInput -> {
                state = state.copy(parserValidationMessage = result.message)
            }

            is ClassificationResult.Classified -> {
                val payload = result.payload
                val itemIndex = nextItemIndex++
                val now = System.currentTimeMillis()
                val item = KnowledgeItem(
                    id = "item-classified-$itemIndex",
                    topicId = payload.topicId,
                    contentType = payload.contentType,
                    tag = payload.tag,
                    title = payload.title,
                    summary = payload.summary,
                    fullText = payload.rawInput,
                    sourceUrl = payload.rawInput.extractSourceUrl(payload.contentType),
                    documentFormat = payload.documentFormat,
                    createdAtEpochMillis = now,
                )
        state = state.copy(
            items = state.items + item,
            topics = state.topics.map { topic ->
                if (topic.id == payload.topicId) topic.copy(updatedAtEpochMillis = now) else topic
            },
            parserInput = "",
            parserValidationMessage = null,
            selectedPane = AppPane.DETAIL,
            selectedTopicId = payload.topicId,
            activeDetailFilter = ContentType.ALL,
        )
        saveData()
    }
        }
    }

    fun createTopic(title: String) {
        val normalizedTitle = title.trim()
        val validationMessage = topicTitleValidationMessage(normalizedTitle)
        if (validationMessage != null) {
            state = state.copy(topicValidationMessage = validationMessage)
            return
        }

        val topicIndex = nextTopicIndex++
        val topic = Topic(
            id = "topic-user-$topicIndex",
            title = normalizedTitle,
            iconName = "folder-spark",
            iconColor = "#5e5d59",
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        state = state.copy(
            topics = state.topics + topic,
            selectedPane = AppPane.DETAIL,
            selectedTopicId = topic.id,
            topicValidationMessage = null,
        )
        saveData()
    }

    fun renameTopic(topicId: String, title: String) {
        val normalizedTitle = title.trim()
        val validationMessage = topicTitleValidationMessage(normalizedTitle, topicId)
        if (validationMessage != null) {
            state = state.copy(topicValidationMessage = validationMessage)
            return
        }

        val renameEpoch = System.currentTimeMillis()

        state = state.copy(
            topics = state.topics.map { topic ->
                if (topic.id == topicId) topic.copy(title = normalizedTitle, updatedAtEpochMillis = renameEpoch) else topic
            },
            topicValidationMessage = null,
        )
        saveData()
    }

    fun deleteTopic(topicId: String) {
        val deletingActiveTopic = state.selectedTopicId == topicId
        state = state.copy(
            topics = state.topics.filterNot { it.id == topicId },
            items = state.items.filterNot { it.topicId == topicId },
            selectedPane = if (deletingActiveTopic) AppPane.TOPICS else state.selectedPane,
            selectedTopicId = if (deletingActiveTopic) null else state.selectedTopicId,
            activeDetailFilter = if (deletingActiveTopic) ContentType.ALL else state.activeDetailFilter,
            modalItem = state.modalItem?.takeUnless { it.topicId == topicId },
            topicValidationMessage = null,
        )
        saveData()
    }

    fun selectFilter(contentType: ContentType) {
        state = state.copy(activeDetailFilter = contentType)
    }

    fun openCardModal(itemId: String) {
        val item = state.items.firstOrNull { it.id == itemId } ?: return
        state = state.copy(selectedPane = AppPane.CARD_DETAIL, modalItem = item)
    }

    fun closeCardModal() {
        state = state.copy(
            selectedPane = if (state.selectedTopicId == null) AppPane.TOPICS else AppPane.DETAIL,
            modalItem = null,
        )
    }

    fun showClipboard(
        content: String,
        imageUri: String? = null,
        sourceUri: String? = imageUri,
        sourceContentType: ContentType? = if (imageUri != null) ContentType.IMAGE_SCREENSHOT else null,
        sourceDocumentFormat: DocumentFormat? = null,
        sourceFileName: String? = null,
    ) {
        val normalizedContent = content.trim().takeIf { it.isNotBlank() }
        val normalizedImageUri = imageUri?.trim()?.takeIf { it.isNotBlank() }
        val normalizedSourceUri = sourceUri?.trim()?.takeIf { it.isNotBlank() }
        val normalizedFileName = sourceFileName?.trim()?.takeIf { it.isNotBlank() }
        if (normalizedContent == null && normalizedImageUri == null && normalizedSourceUri == null) return

        val snapshot = ClipboardSnapshot(
            content = normalizedContent,
            imageUri = normalizedImageUri,
            sourceUri = normalizedSourceUri,
            sourceContentType = sourceContentType,
            sourceDocumentFormat = sourceDocumentFormat,
            sourceFileName = normalizedFileName,
        )
        if (snapshot == state.ignoredClipboardSnapshot) {
            state = state.copy(latestClipboardSnapshot = snapshot)
            return
        }

        state = state.copy(
            clipboardContent = normalizedContent,
            clipboardImageUri = normalizedImageUri,
            clipboardSourceUri = normalizedSourceUri,
            clipboardSourceContentType = sourceContentType,
            clipboardSourceDocumentFormat = sourceDocumentFormat,
            clipboardSourceFileName = normalizedFileName,
            latestClipboardSnapshot = snapshot,
            showClipboardDialog = true,
        )
    }

    fun dismissClipboardDialog() {
        val ignoredSnapshot = currentClipboardSnapshot()
        state = state.copy(
            clipboardContent = null,
            clipboardImageUri = null,
            clipboardSourceUri = null,
            clipboardSourceContentType = null,
            clipboardSourceDocumentFormat = null,
            clipboardSourceFileName = null,
            ignoredClipboardSnapshot = ignoredSnapshot ?: state.ignoredClipboardSnapshot,
            showClipboardDialog = false,
        )
    }

    fun openLatestClipboardDialog() {
        val snapshot = state.latestClipboardSnapshot ?: return
        state = state.copy(
            clipboardContent = snapshot.content,
            clipboardImageUri = snapshot.imageUri,
            clipboardSourceUri = snapshot.sourceUri,
            clipboardSourceContentType = snapshot.sourceContentType,
            clipboardSourceDocumentFormat = snapshot.sourceDocumentFormat,
            clipboardSourceFileName = snapshot.sourceFileName,
            showClipboardDialog = true,
        )
    }

    fun acceptClipboardAndSummarize() {
        val content = state.clipboardContent ?: return
        state = state.copy(
            parserInput = content,
            clipboardContent = null,
            clipboardImageUri = null,
            clipboardSourceUri = null,
            clipboardSourceContentType = null,
            clipboardSourceDocumentFormat = null,
            clipboardSourceFileName = null,
            ignoredClipboardSnapshot = null,
            showClipboardDialog = false,
        )
        submitParserInput()
    }

    fun acceptClipboardAndManualCreate() {
        val targetTopicId = state.selectedTopicId
            ?.takeIf { selectedTopicId -> state.topics.any { it.id == selectedTopicId } }
            ?: state.recentTopics.firstOrNull()?.id
            ?: return
        val prefill = clipboardAddItemPrefill()
        state = state.copy(
            clipboardContent = null,
            clipboardImageUri = null,
            clipboardSourceUri = null,
            clipboardSourceContentType = null,
            clipboardSourceDocumentFormat = null,
            clipboardSourceFileName = null,
            ignoredClipboardSnapshot = null,
            showClipboardDialog = false,
            selectedPane = AppPane.DETAIL,
            selectedTopicId = targetTopicId,
            activeDetailFilter = ContentType.ALL,
            modalItem = null,
            addItemDialogVisible = true,
            addItemDialogValidationMessage = null,
            addItemDialogPrefill = prefill,
        )
    }

    fun updateAiSettings(settings: AiEngineSettings) {
        state = state.copy(aiSettings = settings)
    }

    private fun topicTitleValidationMessage(title: String, currentTopicId: String? = null): String? = when {
        title.isBlank() -> "请输入主题名称"
        state.topics.any { it.id != currentTopicId && it.title == title } -> "主题名称已存在"
        else -> null
    }

    private fun String.extractSourceUrl(contentType: ContentType): String? {
        if (contentType != ContentType.WEB_ARTICLE) return null
        return lineSequence()
            .flatMap { it.splitToSequence(' ', '\t', '\n') }
            .firstOrNull { it.startsWith("http://") || it.startsWith("https://") || it.startsWith("www.") }
    }

    private fun String.isBareWebUrl(): Boolean {
        val trimmed = trim()
        return trimmed.isNotBlank() &&
            !trimmed.any(Char::isWhitespace) &&
            (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("www."))
    }

    private fun currentClipboardSnapshot(): ClipboardSnapshot? {
        return ClipboardSnapshot(
            content = state.clipboardContent,
            imageUri = state.clipboardImageUri,
            sourceUri = state.clipboardSourceUri,
            sourceContentType = state.clipboardSourceContentType,
            sourceDocumentFormat = state.clipboardSourceDocumentFormat,
            sourceFileName = state.clipboardSourceFileName,
        ).takeIf { it.content != null || it.imageUri != null || it.sourceUri != null }
    }

    private fun clipboardAddItemPrefill(): AddItemDialogPrefill {
        val content = state.clipboardContent.orEmpty()
        val sourceUri = state.clipboardSourceUri
        val sourceContentType = state.clipboardSourceContentType
        if (sourceUri != null && sourceContentType != null) {
            val isMixedImageText = content.isNotBlank() && sourceContentType == ContentType.IMAGE_SCREENSHOT
            return AddItemDialogPrefill(
                title = content.ifBlank { state.clipboardSourceFileName.orEmpty() },
                contentType = if (isMixedImageText) ContentType.DOCUMENT else sourceContentType,
                sourceUrl = if (isMixedImageText) null else sourceUri,
                documentFormat = if (isMixedImageText) DocumentFormat.MARKDOWN else state.clipboardSourceDocumentFormat,
                fileName = state.clipboardSourceFileName,
                lockContentType = true,
                availableContentTypes = if (isMixedImageText) listOf(ContentType.DOCUMENT) else null,
            )
        }

        val sourceUrl = content.takeIf { it.isBareWebUrl() }
        return AddItemDialogPrefill(
            contentType = if (sourceUrl != null) ContentType.WEB_ARTICLE else ContentType.DOCUMENT,
            sourceUrl = sourceUrl,
            documentFormat = if (sourceUrl == null) DocumentFormat.MARKDOWN else null,
            lockContentType = sourceUrl == null,
            availableContentTypes = if (sourceUrl != null) {
                listOf(ContentType.WEB_ARTICLE, ContentType.DOCUMENT)
            } else {
                listOf(ContentType.DOCUMENT)
            },
            textContent = content,
        )
    }
}
