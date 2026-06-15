package com.lyihub.archiveassistant.state

import com.lyihub.archiveassistant.domain.AiEngineSettings
import com.lyihub.archiveassistant.domain.AppPane
import com.lyihub.archiveassistant.domain.ContentType
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.Topic

enum class TopicNameDialogMode {
    CREATE,
    RENAME,
}

data class ArchiveAssistantState(
    val selectedPane: AppPane = AppPane.TOPICS,
    val selectedTopicId: String? = null,
    val topics: List<Topic> = emptyList(),
    val items: List<KnowledgeItem> = emptyList(),
    val activeDetailFilter: ContentType = ContentType.ALL,
    val parserInput: String = "",
    val parserValidationMessage: String? = null,
    val topicValidationMessage: String? = null,
    val modalItem: KnowledgeItem? = null,
    val aiSettings: AiEngineSettings = AiEngineSettings(),
    val topicNameDialogMode: TopicNameDialogMode? = null,
    val topicNameDialogTopicId: String? = null,
    val deleteConfirmTopicId: String? = null,
    val addItemDialogVisible: Boolean = false,
    val addItemDialogValidationMessage: String? = null,
    val homeSearchQuery: String = "",
) {
    val itemsByTopic: Map<String, List<KnowledgeItem>> = items.groupBy { it.topicId }

    val selectedTopic: Topic? = topics.firstOrNull { it.id == selectedTopicId }

    val selectedTopicItems: List<KnowledgeItem> = selectedTopicId
        ?.let { topicId -> itemsByTopic[topicId].orEmpty() }
        .orEmpty()

    val filteredSelectedTopicItems: List<KnowledgeItem> = when (activeDetailFilter) {
        ContentType.ALL -> selectedTopicItems
        else -> selectedTopicItems.filter { it.contentType == activeDetailFilter }
    }

    val recentTopics: List<Topic> = topics
        .sortedByDescending { it.updatedAtEpochMillis }
        .take(5)

    val searchedTopics: List<Topic> = if (homeSearchQuery.isBlank()) {
        recentTopics
    } else {
        val query = homeSearchQuery.lowercase()
        val matchingTopicIds = items
            .filter { it.title.lowercase().contains(query) || it.summary.lowercase().contains(query) }
            .map { it.topicId }
            .toSet()
        topics
            .filter { it.title.lowercase().contains(query) || it.id in matchingTopicIds }
            .sortedByDescending { it.updatedAtEpochMillis }
    }
}
