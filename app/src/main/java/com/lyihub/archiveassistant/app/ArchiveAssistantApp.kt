package com.lyihub.archiveassistant.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lyihub.archiveassistant.data.AiEngineSettingsRepository
import com.lyihub.archiveassistant.data.AppDataRepository
import com.lyihub.archiveassistant.domain.AiEngineSettings
import com.lyihub.archiveassistant.domain.AppPane
import com.lyihub.archiveassistant.state.ArchiveAssistantStateStore
import com.lyihub.archiveassistant.ui.layout.LayoutMode
import com.lyihub.archiveassistant.ui.layout.rememberWindowLayoutInfo
import com.lyihub.archiveassistant.ui.layout.shouldShowTwoPanes
import com.lyihub.archiveassistant.ui.screens.AddItemDialog
import com.lyihub.archiveassistant.ui.screens.CardModal
import com.lyihub.archiveassistant.ui.screens.DetailPane
import com.lyihub.archiveassistant.ui.screens.HomePane
import com.lyihub.archiveassistant.ui.screens.ManagePane
import com.lyihub.archiveassistant.ui.screens.SettingsPane
import kotlinx.coroutines.launch

@Composable
fun ArchiveAssistantApp(
    stateStore: ArchiveAssistantStateStore? = null,
    aiSettingsRepository: AiEngineSettingsRepository? = null,
    appDataRepository: AppDataRepository? = null,
) {
    val effectiveStateStore = stateStore ?: androidx.compose.runtime.remember(appDataRepository) {
        ArchiveAssistantStateStore(appDataRepository = appDataRepository)
    }
    val coroutineScope = rememberCoroutineScope()
    val onAiSettingsChanged: (AiEngineSettings) -> Unit = { settings ->
        effectiveStateStore.updateAiSettings(settings)
        aiSettingsRepository?.let { repository ->
            coroutineScope.launch {
                repository.save(settings)
            }
        }
    }

    LaunchedEffect(aiSettingsRepository) {
        aiSettingsRepository?.settings?.collect(effectiveStateStore::updateAiSettings)
    }

    val state = effectiveStateStore.state
    val layoutInfo = rememberWindowLayoutInfo()
    val showTwoPanes = layoutInfo.shouldShowTwoPanes(state.selectedTopicId)

    val layoutModeTag = when (layoutInfo.mode) {
        LayoutMode.COMPACT -> "layout-mode-compact"
        LayoutMode.EXPANDED -> "layout-mode-expanded"
        LayoutMode.FOLDABLE -> "layout-mode-foldable"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(layoutModeTag),
    ) {
        if (showTwoPanes) {
            TwoPaneLayout(
                stateStore = effectiveStateStore,
                layoutInfo = layoutInfo,
                onAiSettingsChanged = onAiSettingsChanged,
            )
        } else {
            SinglePaneLayout(
                stateStore = effectiveStateStore,
                onAiSettingsChanged = onAiSettingsChanged,
            )
        }

        state.modalItem?.let { item ->
            CardModal(
                item = item,
                onClose = effectiveStateStore::closeCardModal,
            )
        }
    }
}

@Composable
private fun SinglePaneLayout(
    stateStore: ArchiveAssistantStateStore,
    onAiSettingsChanged: (AiEngineSettings) -> Unit,
) {
    val state = stateStore.state

    when (state.selectedPane) {
        AppPane.TOPICS -> HomePane(
            title = "聚合拾遗",
            parserInput = state.parserInput,
            parserValidationMessage = state.parserValidationMessage,
            recentTopics = state.searchedTopics,
            itemsByTopic = state.itemsByTopic,
            searchQuery = state.homeSearchQuery,
            onParserInputChanged = stateStore::updateParserInput,
            onSubmitParserInput = stateStore::submitParserInput,
            onTopicSelected = stateStore::openTopic,
            onOpenSettings = stateStore::openSettings,
            onOpenManage = stateStore::openTopicManagement,
            onCreateTopic = stateStore::openTopicManagementForCreate,
            onSearchQueryChanged = stateStore::updateHomeSearchQuery,
        )

        AppPane.DETAIL -> {
            val topic = state.selectedTopic
            if (topic != null) {
                DetailPane(
                    topic = topic,
                    items = state.filteredSelectedTopicItems,
                    activeFilter = state.activeDetailFilter,
                    searchQuery = state.homeSearchQuery,
                    onBack = stateStore::closePanes,
                    onFilterSelected = stateStore::selectFilter,
                    onItemClick = stateStore::openCardModal,
                    onAddItemClick = stateStore::openAddItemDialog,
                )
                if (state.addItemDialogVisible) {
                    AddItemDialog(
                        onDismiss = stateStore::closeAddItemDialog,
                        onConfirm = { title, contentType, sourceUrl, summary, useAiSummary, documentFormat, fileName ->
                            stateStore.confirmAddItem(title, contentType, sourceUrl, summary, useAiSummary, documentFormat, fileName)
                        },
                        validationMessage = state.addItemDialogValidationMessage,
                    )
                }
            } else {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = state.parserInput,
                    parserValidationMessage = state.parserValidationMessage,
                    recentTopics = state.searchedTopics,
                    itemsByTopic = state.itemsByTopic,
                    searchQuery = state.homeSearchQuery,
                    onParserInputChanged = stateStore::updateParserInput,
                    onSubmitParserInput = stateStore::submitParserInput,
                    onTopicSelected = stateStore::openTopic,
                    onOpenSettings = stateStore::openSettings,
                    onOpenManage = stateStore::openTopicManagement,
                    onCreateTopic = stateStore::openTopicManagementForCreate,
                    onSearchQueryChanged = stateStore::updateHomeSearchQuery,
                )
            }
        }

        AppPane.SETTINGS -> SettingsPane(
                aiSettings = state.aiSettings,
                onAiSettingsChanged = onAiSettingsChanged,
                onBack = stateStore::closePanes,
            )

        AppPane.MANAGE -> ManagePane(
            topics = state.topics,
            itemsByTopic = state.itemsByTopic,
            onBack = stateStore::closePanes,
            onTopicSelected = stateStore::openTopic,
            onCreateTopic = stateStore::openCreateTopicDialog,
            onRenameTopic = stateStore::openRenameTopicDialog,
            onDeleteTopic = stateStore::openDeleteConfirmDialog,
            onConfirmCreateTopic = stateStore::confirmCreateTopic,
            onConfirmRenameTopic = stateStore::confirmRenameTopic,
            onConfirmDeleteTopic = stateStore::confirmDeleteTopic,
            onCloseTopicNameDialog = stateStore::closeTopicNameDialog,
            onCloseDeleteConfirmDialog = stateStore::closeDeleteConfirmDialog,
            topicNameDialogMode = state.topicNameDialogMode,
            topicNameDialogTopicId = state.topicNameDialogTopicId,
            topicValidationMessage = state.topicValidationMessage,
            deleteConfirmTopicId = state.deleteConfirmTopicId,
        )

        AppPane.CLASSIFICATION_REVIEW -> HomePane(
            title = "聚合拾遗",
            parserInput = state.parserInput,
            parserValidationMessage = state.parserValidationMessage,
            recentTopics = state.searchedTopics,
            itemsByTopic = state.itemsByTopic,
            searchQuery = state.homeSearchQuery,
            onParserInputChanged = stateStore::updateParserInput,
            onSubmitParserInput = stateStore::submitParserInput,
            onTopicSelected = stateStore::openTopic,
            onOpenSettings = stateStore::openSettings,
            onOpenManage = stateStore::openTopicManagement,
            onCreateTopic = stateStore::openTopicManagementForCreate,
            onSearchQueryChanged = stateStore::updateHomeSearchQuery,
        )

        AppPane.CARD_DETAIL -> {
            val topic = state.selectedTopic
            if (topic != null) {
                DetailPane(
                    topic = topic,
                    items = state.filteredSelectedTopicItems,
                    activeFilter = state.activeDetailFilter,
                    searchQuery = state.homeSearchQuery,
                    onBack = stateStore::closeCardModal,
                    onFilterSelected = stateStore::selectFilter,
                    onItemClick = stateStore::openCardModal,
                    onAddItemClick = stateStore::openAddItemDialog,
                )
                if (state.addItemDialogVisible) {
                    AddItemDialog(
                        onDismiss = stateStore::closeAddItemDialog,
                        onConfirm = { title, contentType, sourceUrl, summary, useAiSummary, documentFormat, fileName ->
                            stateStore.confirmAddItem(title, contentType, sourceUrl, summary, useAiSummary, documentFormat, fileName)
                        },
                        validationMessage = state.addItemDialogValidationMessage,
                    )
                }
            } else {
                HomePane(
                    title = "聚合拾遗",
                    parserInput = state.parserInput,
                    parserValidationMessage = state.parserValidationMessage,
                    recentTopics = state.searchedTopics,
                    itemsByTopic = state.itemsByTopic,
                    searchQuery = state.homeSearchQuery,
                    onParserInputChanged = stateStore::updateParserInput,
                    onSubmitParserInput = stateStore::submitParserInput,
                    onTopicSelected = stateStore::openTopic,
                    onOpenSettings = stateStore::openSettings,
                    onOpenManage = stateStore::openTopicManagement,
                    onCreateTopic = stateStore::openTopicManagementForCreate,
                    onSearchQueryChanged = stateStore::updateHomeSearchQuery,
                )
            }
        }
    }
}

@Composable
private fun TwoPaneLayout(
    stateStore: ArchiveAssistantStateStore,
    layoutInfo: com.lyihub.archiveassistant.ui.layout.WindowLayoutInfo,
    onAiSettingsChanged: (AiEngineSettings) -> Unit,
) {
    val state = stateStore.state
    val hingeBounds = layoutInfo.hingeBounds

    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            HomePane(
                title = "聚合拾遗",
                parserInput = state.parserInput,
                parserValidationMessage = state.parserValidationMessage,
                recentTopics = state.searchedTopics,
                itemsByTopic = state.itemsByTopic,
                searchQuery = state.homeSearchQuery,
                onParserInputChanged = stateStore::updateParserInput,
                onSubmitParserInput = stateStore::submitParserInput,
                onTopicSelected = stateStore::openTopic,
                onOpenSettings = stateStore::openSettings,
                onOpenManage = stateStore::openTopicManagement,
                onCreateTopic = stateStore::openTopicManagementForCreate,
                onSearchQueryChanged = stateStore::updateHomeSearchQuery,
            )
        }

        if (hingeBounds.isNotEmpty()) {
            val hingeWidth = hingeBounds.maxOf { it.right - it.left }.coerceAtLeast(0)
            if (hingeWidth > 0) {
                Box(
                    modifier = Modifier
                        .width(hingeWidth.dp)
                        .fillMaxHeight()
                        .testTag("hinge-spacer"),
                )
            } else {
                VerticalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        } else {
            VerticalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.fillMaxHeight(),
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (state.selectedPane) {
                AppPane.DETAIL -> {
                    val topic = state.selectedTopic
                    if (topic != null) {
                        DetailPane(
                            topic = topic,
                            items = state.filteredSelectedTopicItems,
                            activeFilter = state.activeDetailFilter,
                            searchQuery = state.homeSearchQuery,
                            onBack = stateStore::closePanes,
                            onFilterSelected = stateStore::selectFilter,
                            onItemClick = stateStore::openCardModal,
                            onAddItemClick = stateStore::openAddItemDialog,
                        )
                        if (state.addItemDialogVisible) {
                            AddItemDialog(
                                onDismiss = stateStore::closeAddItemDialog,
                                onConfirm = { title, contentType, sourceUrl, summary, useAiSummary, documentFormat, fileName ->
                            stateStore.confirmAddItem(title, contentType, sourceUrl, summary, useAiSummary, documentFormat, fileName)
                        },
                                validationMessage = state.addItemDialogValidationMessage,
                            )
                        }
                    }
                }

                AppPane.SETTINGS -> SettingsPane(
                    aiSettings = state.aiSettings,
                    onAiSettingsChanged = onAiSettingsChanged,
                    onBack = stateStore::closePanes,
                )

                AppPane.TOPICS -> EmptyDetailPane()

                AppPane.MANAGE -> ManagePane(
                    topics = state.topics,
                    itemsByTopic = state.itemsByTopic,
                    onBack = stateStore::closePanes,
                    onTopicSelected = stateStore::openTopic,
                    onCreateTopic = stateStore::openCreateTopicDialog,
                    onRenameTopic = stateStore::openRenameTopicDialog,
                    onDeleteTopic = stateStore::openDeleteConfirmDialog,
                    onConfirmCreateTopic = stateStore::confirmCreateTopic,
                    onConfirmRenameTopic = stateStore::confirmRenameTopic,
                    onConfirmDeleteTopic = stateStore::confirmDeleteTopic,
                    onCloseTopicNameDialog = stateStore::closeTopicNameDialog,
                    onCloseDeleteConfirmDialog = stateStore::closeDeleteConfirmDialog,
                    topicNameDialogMode = state.topicNameDialogMode,
                    topicNameDialogTopicId = state.topicNameDialogTopicId,
                    topicValidationMessage = state.topicValidationMessage,
                    deleteConfirmTopicId = state.deleteConfirmTopicId,
                )

                AppPane.CLASSIFICATION_REVIEW -> EmptyDetailPane()

                AppPane.CARD_DETAIL -> {
                    val topic = state.selectedTopic
                    if (topic != null) {
                        DetailPane(
                            topic = topic,
                            items = state.filteredSelectedTopicItems,
                            activeFilter = state.activeDetailFilter,
                            searchQuery = state.homeSearchQuery,
                            onBack = stateStore::closeCardModal,
                            onFilterSelected = stateStore::selectFilter,
                            onItemClick = stateStore::openCardModal,
                            onAddItemClick = stateStore::openAddItemDialog,
                        )
                        if (state.addItemDialogVisible) {
                            AddItemDialog(
                                onDismiss = stateStore::closeAddItemDialog,
                                onConfirm = { title, contentType, sourceUrl, summary, useAiSummary, documentFormat, fileName ->
                            stateStore.confirmAddItem(title, contentType, sourceUrl, summary, useAiSummary, documentFormat, fileName)
                        },
                                validationMessage = state.addItemDialogValidationMessage,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDetailPane() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("empty-detail-pane"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "选择主题查看",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

