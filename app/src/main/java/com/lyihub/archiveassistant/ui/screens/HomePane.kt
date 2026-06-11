package com.lyihub.archiveassistant.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyihub.archiveassistant.domain.Topic
import com.lyihub.archiveassistant.ui.components.ActionButton
import com.lyihub.archiveassistant.ui.components.PaneContainer
import com.lyihub.archiveassistant.ui.components.PaneContentPadding
import com.lyihub.archiveassistant.ui.components.PaneDivider
import com.lyihub.archiveassistant.ui.components.PaneHeader

@Composable
fun HomePane(
    title: String,
    parserInput: String,
    parserValidationMessage: String?,
    recentTopics: List<Topic>,
    itemsByTopic: Map<String, List<com.lyihub.archiveassistant.domain.KnowledgeItem>>,
    onParserInputChanged: (String) -> Unit,
    onSubmitParserInput: () -> Unit,
    onTopicSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenManage: () -> Unit,
    onCreateTopic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaneContainer(modifier = modifier.testTag("home-pane")) {
        PaneHeader(
            title = title,
            navigationIcon = {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag("settings-trigger"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        PaneDivider()
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            item {
                PaneContentPadding {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ParserSection(
                            input = parserInput,
                            validationMessage = parserValidationMessage,
                            onInputChanged = onParserInputChanged,
                            onSubmit = onSubmitParserInput,
                        )
                        ActionButton(
                            label = "新建主题",
                            onClick = onCreateTopic,
                            testTag = "home-create-topic-button",
                            icon = Icons.Default.Add,
                        )
                        ActionButton(
                            label = "管理主题",
                            onClick = onOpenManage,
                            testTag = "manage-button",
                            icon = Icons.AutoMirrored.Filled.List,
                        )
                    }
                }
            }
            item {
                Column(modifier = Modifier.testTag("recent-topic-list")) {
                    PaneDivider()
                    Text(
                        text = "最近主题",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    recentTopics.forEach { topic ->
                        TopicCard(
                            topic = topic,
                            itemCount = itemsByTopic[topic.id]?.size ?: 0,
                            onClick = { onTopicSelected(topic.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParserSection(
    input: String,
    validationMessage: String?,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("parser-input"),
            placeholder = {
                Text(
                    text = "拖拽文件、输入链接、纯文本，或直接从剪切板粘贴...",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            minLines = 3,
            maxLines = 6,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "支持多模态解析",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ActionButton(
                label = "智能归纳",
                onClick = onSubmit,
                testTag = "classify-button",
            )
        }
        if (validationMessage != null) {
            Text(
                text = validationMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun TopicCard(
    topic: Topic,
    itemCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("topic-card-${topic.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${friendlyTime(topic.updatedAtEpochMillis)} · ${itemCount} 项内容",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun friendlyTime(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diff = nowMillis - epochMillis
    return when {
        diff < 0 -> "未来"
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        diff < 2_592_000_000L -> "${diff / 86_400_000} 天前"
        else -> "很久以前"
    }
}
