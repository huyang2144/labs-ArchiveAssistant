package com.lyihub.archiveassistant.state

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Log
import com.lyihub.archiveassistant.data.AiEngineSettingsRepository
import com.lyihub.archiveassistant.data.AppDataRepository
import com.lyihub.archiveassistant.data.DefaultDocumentContentExtractor
import com.lyihub.archiveassistant.data.ModelDownloadManager
import com.lyihub.archiveassistant.data.RemoteApiSmartSummarizer
import com.lyihub.archiveassistant.data.DefaultWebPageContentFetcher
import com.lyihub.archiveassistant.data.DocumentContentExtractionResult
import com.lyihub.archiveassistant.data.DocumentContentExtractor
import com.lyihub.archiveassistant.data.WebPageContentFetcher
import com.lyihub.archiveassistant.data.WebPageContentFetchResult
import com.lyihub.archiveassistant.domain.AiEngineSettings
import com.lyihub.archiveassistant.domain.AiEngineType
import com.lyihub.archiveassistant.domain.AppPane
import com.lyihub.archiveassistant.domain.BenchResult
import com.lyihub.archiveassistant.domain.ClassificationResult
import com.lyihub.archiveassistant.domain.ContentType
import com.lyihub.archiveassistant.domain.DocumentFormat
import com.lyihub.archiveassistant.domain.FetchedDocumentContext
import com.lyihub.archiveassistant.domain.FetchedWebContext
import com.lyihub.archiveassistant.domain.GEMMA_4_E4B_IT
import com.lyihub.archiveassistant.domain.InferenceBackend
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.LocalLlmClassifier
import com.lyihub.archiveassistant.domain.LocalLlmEngine
import com.lyihub.archiveassistant.domain.LocalLlmSmartSummarizer
import com.lyihub.archiveassistant.domain.LocalModelState
import com.lyihub.archiveassistant.domain.LocalModelStatus
import com.lyihub.archiveassistant.domain.MockKnowledgeClassifier
import com.lyihub.archiveassistant.domain.SampleKnowledgeData
import com.lyihub.archiveassistant.domain.SmartSummarizeRequest
import com.lyihub.archiveassistant.domain.SmartSummarizeResult
import com.lyihub.archiveassistant.domain.SmartSummarizer
import com.lyihub.archiveassistant.domain.Topic
import com.lyihub.archiveassistant.domain.WebUrlDetector
import com.lyihub.archiveassistant.service.LocalInferenceGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class ArchiveAssistantStateStore(
    private val classifier: MockKnowledgeClassifier = MockKnowledgeClassifier(),
    private val initialState: ArchiveAssistantState = ArchiveAssistantState(
        topics = SampleKnowledgeData.topics,
        items = SampleKnowledgeData.items,
        aiSettings = SampleKnowledgeData.defaultAiEngineSettings,
    ),
    private val appDataRepository: AppDataRepository? = null,
    private val aiSettingsRepository: AiEngineSettingsRepository? = null,
    private val localLlmEngine: LocalLlmEngine? = null,
    private val modelDownloadManager: ModelDownloadManager? = null,
    private val inferenceConnection: LocalInferenceGateway? = null,
    private val localModelStateProvider: (() -> LocalModelState)? = null,
    private val localModelFileExists: (() -> Boolean)? = null,
    smartSummarizer: SmartSummarizer? = null,
    private val remoteSmartSummarizerFactory: (AiEngineSettings) -> SmartSummarizer = ::RemoteApiSmartSummarizer,
    private val webPageContentFetcher: WebPageContentFetcher = DefaultWebPageContentFetcher(),
    documentContentExtractor: DocumentContentExtractor? = null,
    androidContext: Context? = null,
) {
    private val appContext = androidContext
    private val documentContentExtractor = documentContentExtractor
        ?: androidContext?.let(::DefaultDocumentContentExtractor)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val smartSummarizer: SmartSummarizer? = smartSummarizer
        ?: localLlmEngine?.let(::LocalLlmSmartSummarizer)

    var state: ArchiveAssistantState by mutableStateOf(
        resolveMockImagePaths(initialState)
    )
        private set

    var releaseDragPermission: (() -> Unit)? = null

    private var nextTopicIndex = deriveNextTopicIndex(state.topics)
    private var nextItemIndex = deriveNextItemIndex(state.items)

    init {
        loadPersistedStateAsync()
        restoreLocalModelState()
        observeLocalModelState()
    }

    private fun loadPersistedStateAsync() {
        val repo = appDataRepository ?: return
        scope.launch {
            val persistedTopics = repo.loadTopics()
            val persistedItems = repo.loadItems()
            if (persistedTopics.isEmpty() && persistedItems.isEmpty()) return@launch

            val restoredState = resolveMockImagePaths(
                state.copy(
                    topics = persistedTopics,
                    items = persistedItems,
                )
            )
            state = restoredState
            nextTopicIndex = deriveNextTopicIndex(restoredState.topics)
            nextItemIndex = deriveNextItemIndex(restoredState.items)
        }
    }

    private fun saveData() {
        val repo = appDataRepository ?: return
        scope.launch {
            repo.saveAll(state.topics, state.items)
        }
    }

    private suspend fun persistData(topics: List<Topic>, items: List<KnowledgeItem>) {
        appDataRepository?.saveAll(topics, items)
    }

    private fun saveAiSettings() {
        val repo = aiSettingsRepository ?: return
        val settings = state.aiSettings
        scope.launch {
            repo.save(settings)
        }
    }

    private fun restoreLocalModelState() {
        if (state.aiSettings.localModelId != GEMMA_4_E4B_IT.id) {
            Log.w(TAG, "模型版本已变更，请重新下载")
            resetLocalModelState()
            return
        }

        val modelFile = localModelFile()
        val modelExists = isLocalModelFilePresent()
        val restoredState = when {
            modelExists -> LocalModelState(
                status = LocalModelStatus.DOWNLOADED,
                downloadProgress = 1f,
                downloadBytes = GEMMA_4_E4B_IT.sizeBytes,
                totalBytes = GEMMA_4_E4B_IT.sizeBytes,
                modelPath = modelFile?.absolutePath,
            )
            else -> LocalModelState(status = LocalModelStatus.NOT_DOWNLOADED)
        }
        state = state.copy(localModelState = restoredState)
    }

    private fun observeLocalModelState() {
        modelDownloadManager?.let { manager ->
            scope.launch {
                manager.downloadState.collectLatest { updateDownloadModelState(it) }
            }
        }
        inferenceConnection?.let { connection ->
            scope.launch {
                connection.serviceState.collectLatest { updateInferenceModelState(it) }
            }
        }
    }

    private fun updateDownloadModelState(nextState: LocalModelState) {
        if (state.localModelState.status in setOf(LocalModelStatus.DOWNLOADED, LocalModelStatus.READY) &&
            nextState.status == LocalModelStatus.NOT_DOWNLOADED
        ) {
            return
        }
        if (state.localModelState.status in serviceOwnedStatuses &&
            nextState.status in setOf(LocalModelStatus.NOT_DOWNLOADED, LocalModelStatus.DOWNLOADED)
        ) {
            return
        }
        updateLocalModelState(nextState)
    }

    private fun updateInferenceModelState(nextState: LocalModelState) {
        updateLocalModelState(nextState)
    }

    private fun updateLocalModelState(nextState: LocalModelState) {
        val current = state.localModelState
        if (nextState.status == LocalModelStatus.DOWNLOADED &&
            current.status in setOf(LocalModelStatus.NOT_DOWNLOADED, LocalModelStatus.DOWNLOADING, LocalModelStatus.ERROR) &&
            modelDownloadManager?.isModelPresent(GEMMA_4_E4B_IT) != true
        ) {
            return
        }
        state = state.copy(localModelState = nextState)
    }

    private val serviceOwnedStatuses = setOf(
        LocalModelStatus.INITIALIZING,
        LocalModelStatus.READY,
        LocalModelStatus.INFERENCING,
        LocalModelStatus.STOPPING,
    )

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
        releaseDragPermission?.invoke()
        releaseDragPermission = null
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
        releaseDragPermission?.invoke()
        releaseDragPermission = null
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
        state = state.copy(
            parserInput = input,
            parserValidationMessage = null,
            smartSummarizationMessage = null,
        )
    }

    fun updateHomeSearchQuery(query: String) {
        state = state.copy(homeSearchQuery = query)
    }

    fun submitParserInput() {
        summarizeParserInput(state.parserInput, closeClipboardOnSuccess = false)
    }

    private fun summarizeParserInput(rawInput: String, closeClipboardOnSuccess: Boolean) {
        if (state.isSmartSummarizing) return

        val normalizedInput = rawInput.trim()
        if (normalizedInput.isBlank()) {
            state = state.copy(
                parserValidationMessage = "请输入要归档的内容",
                smartSummarizationMessage = "请输入要智能总结的内容",
            )
            return
        }

        val currentLocalModelState = localModelStateProvider?.invoke() ?: state.localModelState
        if (state.aiSettings.engineType == AiEngineType.LOCAL_MODEL && currentLocalModelState.status == LocalModelStatus.INFERENCING) {
            state = state.copy(parserValidationMessage = "推理进行中", smartSummarizationMessage = "推理进行中")
            return
        }
        if (state.aiSettings.engineType == AiEngineType.LOCAL_MODEL && currentLocalModelState.status != LocalModelStatus.READY) {
            state = state.copy(parserValidationMessage = "本地模型未就绪，请先开启模型", smartSummarizationMessage = "本地模型未就绪，请先开启模型")
            return
        }

        scope.launch {
            summarizeAndSave(rawInput, closeClipboardOnSuccess)
        }
    }

    private suspend fun summarizeAndSave(rawInput: String, closeClipboardOnSuccess: Boolean) {
        state = state.copy(
            isSmartSummarizing = true,
            parserValidationMessage = null,
            smartSummarizationMessage = null,
        )
        try {
            val result = summarizeRawInput(rawInput)
            handleSmartSummarizeResult(result, rawInput, closeClipboardOnSuccess)
        } catch (_: Exception) {
            state = state.copy(smartSummarizationMessage = "保存失败，请稍后重试")
        } finally {
            state = state.copy(isSmartSummarizing = false)
        }
    }

    private suspend fun summarizeRawInput(rawInput: String): SmartSummarizeResult {
        val request = createSmartSummarizeRequest(rawInput).getOrElse { reason ->
            val prefix = if (reason is DocumentExtractionException) "文档内容解析失败" else "网页内容获取失败"
            return SmartSummarizeResult.Failure("$prefix：${reason.message.orEmpty()}")
        }

        if (state.aiSettings.engineType != AiEngineType.LOCAL_MODEL) {
            val summarizer = smartSummarizer ?: remoteSmartSummarizerFactory(state.aiSettings)
            return summarizer.summarize(
                request,
                state.topics,
                state.items,
            )
        }

        val directEngine = localLlmEngine
        state = state.copy(localModelState = state.localModelState.copy(status = LocalModelStatus.INFERENCING))
        return try {
            if (directEngine != null) {
                LocalLlmSmartSummarizer(directEngine).summarize(request, state.topics, state.items)
            } else {
                inferenceConnection?.summarize(request, state.topics, state.items)
                    ?: SmartSummarizeResult.Failure(LOCAL_AI_UNAVAILABLE_MESSAGE)
            }
        } finally {
            if (state.localModelState.status == LocalModelStatus.INFERENCING) {
                state = state.copy(localModelState = state.localModelState.copy(status = LocalModelStatus.READY))
            }
        }
    }

    private suspend fun createSmartSummarizeRequest(rawInput: String): Result<SmartSummarizeRequest> {
        documentSummarizeSource()?.let { source ->
            return createDocumentSmartSummarizeRequest(rawInput, source)
        }

        val detected = WebUrlDetector.detect(rawInput)
            ?: return Result.success(SmartSummarizeRequest(rawText = rawInput))

        return when (val fetched = webPageContentFetcher.fetch(detected.originalUrl, detected.fetchUrl)) {
            is WebPageContentFetchResult.Failure -> Result.failure(WebPageFetchException(fetched.message))
            is WebPageContentFetchResult.Success -> {
                val content = fetched.content
                Result.success(
                    SmartSummarizeRequest(
                        rawText = rawInput,
                        sourceUrl = detected.originalUrl,
                        sourceTitle = content.title,
                        fetchedWebContext = FetchedWebContext(
                            originalUrl = detected.originalUrl,
                            title = content.title,
                            description = content.description,
                            bodyText = content.bodyText,
                        ),
                    )
                )
            }
        }
    }

    private suspend fun createDocumentSmartSummarizeRequest(
        rawInput: String,
        source: DocumentSummarizeSource,
    ): Result<SmartSummarizeRequest> {
        val extractor = documentContentExtractor
            ?: return Result.failure(DocumentExtractionException("文档解析不可用"))
        return when (val extracted = extractor.extract(source.uri, source.format, source.fileName)) {
            is DocumentContentExtractionResult.Failure -> Result.failure(DocumentExtractionException(extracted.message))
            is DocumentContentExtractionResult.Success -> {
                val content = extracted.content
                Result.success(
                    SmartSummarizeRequest(
                        rawText = rawInput,
                        sourceUrl = source.uri.toString(),
                        sourceTitle = content.fileName,
                        fetchedDocumentContext = FetchedDocumentContext(
                            fileName = content.fileName,
                            format = content.format,
                            extractedText = content.extractedText,
                            originalCharCount = content.originalCharCount,
                            isTruncated = content.isTruncated,
                        ),
                    )
                )
            }
        }
    }

    private suspend fun handleSmartSummarizeResult(
        result: SmartSummarizeResult,
        rawInput: String,
        closeClipboardOnSuccess: Boolean,
    ) {
        when (result) {
            is SmartSummarizeResult.Failure -> {
                state = state.copy(smartSummarizationMessage = result.message)
            }

            is SmartSummarizeResult.Success -> saveSmartSummarizeSuccess(result, rawInput, closeClipboardOnSuccess)
        }
    }

    private suspend fun saveSmartSummarizeSuccess(
        result: SmartSummarizeResult.Success,
        rawInput: String,
        closeClipboardOnSuccess: Boolean,
    ) {
        val validationMessage = when {
            state.topics.none { it.id == result.topicId } -> "智能总结结果无效，请重试"
            result.contentType == ContentType.ALL -> "智能总结结果无效，请重试"
            result.title.isBlank() -> "智能总结结果无效，请重试"
            else -> null
        }
        if (validationMessage != null) {
            state = state.copy(smartSummarizationMessage = validationMessage)
            return
        }

        val itemIndex = nextItemIndex
        val now = System.currentTimeMillis()
        val sourceUrl = result.sourceUrl ?: rawInput.extractSourceUrl(result.contentType)
        val item = KnowledgeItem(
            id = "item-classified-$itemIndex",
            topicId = result.topicId,
            contentType = result.contentType,
            title = result.title,
            summary = result.summary,
            fullText = rawInput,
            sourceUrl = sourceUrl,
            documentFormat = result.documentFormat,
            createdAtEpochMillis = now,
        )
        val topics = state.topics.map { topic ->
            if (topic.id == result.topicId) topic.copy(updatedAtEpochMillis = now) else topic
        }
        val items = state.items + item

        persistData(topics, items)
        nextItemIndex++
        state = state.copy(
            items = items,
            topics = topics,
            parserInput = "",
            parserValidationMessage = null,
            smartSummarizationMessage = null,
            selectedPane = AppPane.DETAIL,
            selectedTopicId = result.topicId,
            activeDetailFilter = ContentType.ALL,
            clipboardContent = if (closeClipboardOnSuccess) null else state.clipboardContent,
            clipboardImageUri = if (closeClipboardOnSuccess) null else state.clipboardImageUri,
            clipboardSourceUri = if (closeClipboardOnSuccess) null else state.clipboardSourceUri,
            clipboardSourceContentType = if (closeClipboardOnSuccess) null else state.clipboardSourceContentType,
            clipboardSourceDocumentFormat = if (closeClipboardOnSuccess) null else state.clipboardSourceDocumentFormat,
            clipboardSourceFileName = if (closeClipboardOnSuccess) null else state.clipboardSourceFileName,
            clipboardSourceLabel = if (closeClipboardOnSuccess) null else state.clipboardSourceLabel,
            ignoredClipboardSnapshot = if (closeClipboardOnSuccess) null else state.ignoredClipboardSnapshot,
            showClipboardDialog = if (closeClipboardOnSuccess) false else state.showClipboardDialog,
        )
        if (closeClipboardOnSuccess) {
            releaseDragPermission?.invoke()
            releaseDragPermission = null
        }
    }

    private fun handleParserClassificationResult(result: ClassificationResult) {
        when (result) {
            is ClassificationResult.BlankInput -> {
                state = state.copy(parserValidationMessage = result.message)
            }

            is ClassificationResult.Unknown -> {
                state = state.copy(parserValidationMessage = "分类失败，请输入有效内容")
            }

            is ClassificationResult.Classified -> {
                val payload = result.payload
                val itemIndex = nextItemIndex++
                val now = System.currentTimeMillis()
                val item = KnowledgeItem(
                    id = "item-classified-$itemIndex",
                    topicId = payload.topicId,
                    contentType = payload.contentType,
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

    private suspend fun classifyParserInput(): ClassificationResult {
        val engine = localLlmEngine ?: inferenceConnection?.getEngine() ?: return ClassificationResult.Unknown
        state = state.copy(localModelState = state.localModelState.copy(status = LocalModelStatus.INFERENCING))
        return try {
            LocalLlmClassifier(engine).classify(state.parserInput, state.topics)
        } finally {
            if (state.localModelState.status == LocalModelStatus.INFERENCING) {
                state = state.copy(localModelState = state.localModelState.copy(status = LocalModelStatus.READY))
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
        sourceLabel: String? = null,
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
            sourceLabel = sourceLabel,
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
            clipboardSourceLabel = sourceLabel,
            latestClipboardSnapshot = snapshot,
            showClipboardDialog = true,
        )
    }

    fun dismissClipboardDialog() {
        releaseDragPermission?.invoke()
        releaseDragPermission = null
        val ignoredSnapshot = currentClipboardSnapshot()
        state = state.copy(
            clipboardContent = null,
            clipboardImageUri = null,
            clipboardSourceUri = null,
            clipboardSourceContentType = null,
            clipboardSourceDocumentFormat = null,
            clipboardSourceFileName = null,
            clipboardSourceLabel = null,
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
            clipboardSourceLabel = snapshot.sourceLabel,
            showClipboardDialog = true,
        )
    }

    fun acceptClipboardAndSummarize() {
        val content = state.clipboardContent ?: state.clipboardSourceFileName ?: state.clipboardSourceUri ?: return
        state = state.copy(parserInput = content)
        summarizeParserInput(content, closeClipboardOnSuccess = true)
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
            clipboardSourceLabel = null,
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
        val previous = state.aiSettings
        state = state.copy(aiSettings = settings)
        saveAiSettings()
        if (previous.engineType == AiEngineType.LOCAL_MODEL && settings.engineType != AiEngineType.LOCAL_MODEL) {
            when (state.localModelState.status) {
                LocalModelStatus.READY,
                LocalModelStatus.INITIALIZING -> stopModel()
                LocalModelStatus.DOWNLOADING -> cancelDownload()
                else -> Unit
            }
        }
    }

    fun downloadModel() {
        if (state.localModelState.status == LocalModelStatus.DOWNLOADING) return
        val manager = modelDownloadManager ?: return
        scope.launch {
            manager.startDownload(GEMMA_4_E4B_IT)
        }
    }

    fun cancelDownload() {
        val manager = modelDownloadManager ?: return
        scope.launch {
            manager.cancelDownload()
        }
    }

    fun importLocalModel(uri: Uri) {
        if (state.localModelState.status in setOf(LocalModelStatus.DOWNLOADING, LocalModelStatus.INITIALIZING)) return
        val manager = modelDownloadManager ?: return
        scope.launch {
            manager.importModel(GEMMA_4_E4B_IT, uri)
        }
    }

    fun startModel() {
        if (state.localModelState.status in setOf(LocalModelStatus.INITIALIZING, LocalModelStatus.READY)) return
        if (shouldValidateLocalModelFileBeforeStart() &&
            !isLocalModelFilePresent()
        ) {
            resetLocalModelState()
            state = state.copy(
                localModelState = LocalModelState(
                    status = LocalModelStatus.ERROR,
                    errorMessage = "模型文件不存在，请重新下载",
                ),
            )
            return
        }
        val connection = inferenceConnection ?: return
        connection.bind()
        connection.startModel(GEMMA_4_E4B_IT, state.aiSettings.localBackendPreference)
        if (state.localModelState.status !in setOf(LocalModelStatus.INITIALIZING, LocalModelStatus.READY)) {
            state = state.copy(localModelState = state.localModelState.copy(status = LocalModelStatus.INITIALIZING))
        }
    }

    fun stopModel() {
        if (state.localModelState.status == LocalModelStatus.STOPPING) return
        inferenceConnection?.stopModel()
    }

    fun updateBackendPreference(backend: InferenceBackend) {
        updateAiSettings(state.aiSettings.copy(localBackendPreference = backend))
    }

    fun runBenchmark() {
        if (state.localModelState.status != LocalModelStatus.READY || state.isBenchmarkRunning) return
        val engine = inferenceConnection?.getEngine() ?: localLlmEngine ?: return
        state = state.copy(isBenchmarkRunning = true, benchmarkResult = null)
        scope.launch {
            val result = engine.benchmark(128, 128)
            state = state.copy(
                benchmarkResult = result.getOrNull(),
                isBenchmarkRunning = false,
            )
        }
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

    private fun documentSummarizeSource(): DocumentSummarizeSource? {
        if (state.clipboardSourceContentType != ContentType.DOCUMENT) return null
        val sourceUri = state.clipboardSourceUri?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
        return DocumentSummarizeSource(
            uri = sourceUri,
            format = state.clipboardSourceDocumentFormat ?: DocumentFormat.UNKNOWN,
            fileName = state.clipboardSourceFileName,
        )
    }

    private fun String.isBareWebUrl(): Boolean {
        val trimmed = trim()
        return trimmed.isNotBlank() &&
            !trimmed.any(Char::isWhitespace) &&
            (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("www."))
    }

    private fun localModelFile(): File? = appContext?.let { context ->
        File(context.filesDir, "models/${GEMMA_4_E4B_IT.fileName}")
    }

    private fun isLocalModelFilePresent(): Boolean = localModelFileExists?.invoke()
        ?: localModelFile()?.exists()
        ?: false

    private fun resetLocalModelState() {
        state = state.copy(localModelState = LocalModelState(status = LocalModelStatus.NOT_DOWNLOADED))
    }

    private fun shouldValidateLocalModelFileBeforeStart(): Boolean =
        state.localModelState.status in setOf(LocalModelStatus.DOWNLOADED, LocalModelStatus.READY) &&
            (appContext != null || state.localModelState.modelPath != null)

    private fun currentClipboardSnapshot(): ClipboardSnapshot? {
        return ClipboardSnapshot(
            content = state.clipboardContent,
            imageUri = state.clipboardImageUri,
            sourceUri = state.clipboardSourceUri,
            sourceContentType = state.clipboardSourceContentType,
            sourceDocumentFormat = state.clipboardSourceDocumentFormat,
            sourceFileName = state.clipboardSourceFileName,
            sourceLabel = state.clipboardSourceLabel,
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

    private companion object {
        const val TAG = "ArchiveAssistantStateStore"
        const val SMART_SUMMARIZER_UNAVAILABLE_MESSAGE = "智能总结不可用，请先配置真实 AI 引擎"
        const val LOCAL_AI_UNAVAILABLE_MESSAGE = "本地 AI 不可用，请先开启模型"
    }

    private class WebPageFetchException(message: String) : Exception(message)

    private class DocumentExtractionException(message: String) : Exception(message)

    private data class DocumentSummarizeSource(
        val uri: Uri,
        val format: DocumentFormat,
        val fileName: String?,
    )

}
