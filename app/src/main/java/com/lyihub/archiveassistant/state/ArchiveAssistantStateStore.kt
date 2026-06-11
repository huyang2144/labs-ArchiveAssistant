package com.lyihub.archiveassistant.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lyihub.archiveassistant.domain.AiEngineSettings
import com.lyihub.archiveassistant.domain.AppPane
import com.lyihub.archiveassistant.domain.ClassificationResult
import com.lyihub.archiveassistant.domain.ContentType
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.MockKnowledgeClassifier
import com.lyihub.archiveassistant.domain.SampleKnowledgeData
import com.lyihub.archiveassistant.domain.Topic

class ArchiveAssistantStateStore(
    private val classifier: MockKnowledgeClassifier = MockKnowledgeClassifier(),
    initialState: ArchiveAssistantState = ArchiveAssistantState(
        topics = SampleKnowledgeData.topics,
        items = SampleKnowledgeData.items,
        aiSettings = SampleKnowledgeData.defaultAiEngineSettings,
    ),
) {
    var state: ArchiveAssistantState by mutableStateOf(initialState)
        private set

    private var nextTopicIndex = initialState.topics.size + 1
    private var nextItemIndex = initialState.items.size + 1

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

    fun confirmDeleteTopic() {
        val topicId = state.deleteConfirmTopicId ?: return
        closeDeleteConfirmDialog()
        deleteTopic(topicId)
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

    fun submitParserInput() {
        when (val result = classifier.classify(state.parserInput, state.topics)) {
            is ClassificationResult.BlankInput -> {
                state = state.copy(parserValidationMessage = result.message)
            }

            is ClassificationResult.Classified -> {
                val payload = result.payload
                val itemIndex = nextItemIndex++
                val now = GeneratedItemEpochMillis + itemIndex
                val item = KnowledgeItem(
                    id = "item-classified-$itemIndex",
                    topicId = payload.topicId,
                    contentType = payload.contentType,
                    tag = payload.tag,
                    title = payload.title,
                    summary = payload.summary,
                    fullText = payload.rawInput,
                    sourceUrl = payload.rawInput.extractSourceUrl(payload.contentType),
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
            updatedAtEpochMillis = GeneratedTopicEpochMillis + topicIndex,
        )
        state = state.copy(
            topics = state.topics + topic,
            selectedPane = AppPane.DETAIL,
            selectedTopicId = topic.id,
            topicValidationMessage = null,
        )
    }

    fun renameTopic(topicId: String, title: String) {
        val normalizedTitle = title.trim()
        val validationMessage = topicTitleValidationMessage(normalizedTitle, topicId)
        if (validationMessage != null) {
            state = state.copy(topicValidationMessage = validationMessage)
            return
        }

        val renameEpoch = GeneratedTopicEpochMillis + nextTopicIndex++

        state = state.copy(
            topics = state.topics.map { topic ->
                if (topic.id == topicId) topic.copy(title = normalizedTitle, updatedAtEpochMillis = renameEpoch) else topic
            },
            topicValidationMessage = null,
        )
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

    private companion object {
        const val GeneratedTopicEpochMillis = 1_716_100_000_000
        const val GeneratedItemEpochMillis = 1_716_000_000_000
    }
}
