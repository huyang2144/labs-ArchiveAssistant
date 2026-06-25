package com.lyihub.archiveassistant.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lyihub.archiveassistant.domain.Topic
import com.lyihub.archiveassistant.ui.components.ActionButton
import com.lyihub.archiveassistant.ui.components.TextActionButton
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
    searchQuery: String,
    onParserInputChanged: (String) -> Unit,
    onSubmitParserInput: () -> Unit,
    onTopicSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenManage: () -> Unit,
    onCreateTopic: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOpenClipboard: () -> Unit,
    onOpenMemorialDemo: (() -> Unit)? = null,
    isSmartSummarizing: Boolean = false,
    smartSummarizationMessage: String? = null,
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
            actions = {
                if (onOpenMemorialDemo != null) {
                    MemorialDemoButton(onClick = onOpenMemorialDemo)
                }
            },
        )
        PaneDivider()
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            item {
                PaneContentPadding {
                    ParserSection(
                        input = parserInput,
                        validationMessage = parserValidationMessage,
                        onInputChanged = onParserInputChanged,
                        onSubmit = onSubmitParserInput,
                        onOpenClipboard = onOpenClipboard,
                        isSmartSummarizing = isSmartSummarizing,
                        smartSummarizationMessage = smartSummarizationMessage,
                    )
                }
            }
            item {
                PaneContentPadding {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home-search-input"),
                        placeholder = {
                            Text(
                                text = "搜索主题或资料...",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }
            }
            item {
                Column(modifier = Modifier.testTag("recent-topic-list")) {
                    PaneDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "最近主题",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextActionButton(
                                label = "新建",
                                onClick = onCreateTopic,
                                testTag = "home-create-topic-button",
                                icon = Icons.Default.Add,
                            )
                            TextActionButton(
                                label = "管理",
                                onClick = onOpenManage,
                                testTag = "manage-button",
                                icon = Icons.AutoMirrored.Filled.List,
                            )
                        }
                    }
                    recentTopics.forEach { topic ->
                        TopicCard(
                            topic = topic,
                            itemCount = itemsByTopic[topic.id]?.size ?: 0,
                            searchQuery = searchQuery,
                            onClick = { onTopicSelected(topic.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemorialDemoButton(onClick: () -> Unit) {
    val outline = Color(0xFF8A6421)
    val paper = Color(0xFFFFF4D6)
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, outline.copy(alpha = 0.55f), MaterialTheme.shapes.small)
            .testTag("memorial-demo-button"),
        colors = ButtonDefaults.textButtonColors(
            containerColor = paper.copy(alpha = 0.9f),
            contentColor = outline,
        ),
    ) {
        Text(
            text = "奏章",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ParserSection(
    input: String,
    validationMessage: String?,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenClipboard: () -> Unit,
    isSmartSummarizing: Boolean = false,
    smartSummarizationMessage: String? = null,
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextActionButton(
                    label = "剪切板",
                    onClick = onOpenClipboard,
                    testTag = "clipboard-button",
                )
                ActionButton(
                    label = if (isSmartSummarizing) "归纳中…" else "智能归纳",
                    onClick = onSubmit,
                    testTag = "classify-button",
                    enabled = !isSmartSummarizing && input.isNotBlank(),
                )
            }
        }
        if (validationMessage != null) {
            Text(
                text = validationMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        if (smartSummarizationMessage != null) {
            Text(
                text = smartSummarizationMessage,
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
    searchQuery: String,
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
                    text = if (searchQuery.isBlank()) {
                        androidx.compose.ui.text.AnnotatedString(topic.title)
                    } else {
                        buildHighlightedText(
                            text = topic.title,
                            query = searchQuery,
                            highlightColor = MaterialTheme.colorScheme.primary,
                            highlightBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        )
                    },
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

internal fun buildHighlightedText(
    text: String,
    query: String,
    highlightColor: androidx.compose.ui.graphics.Color,
    highlightBgColor: androidx.compose.ui.graphics.Color,
): androidx.compose.ui.text.AnnotatedString {
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    return buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val matchIndex = lowerText.indexOf(lowerQuery, start)
            if (matchIndex < 0) {
                append(text.substring(start))
                break
            }
            if (matchIndex > start) {
                append(text.substring(start, matchIndex))
            }
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    background = highlightBgColor,
                    color = highlightColor,
                )
            ) {
                append(text.substring(matchIndex, matchIndex + query.length))
            }
            start = matchIndex + query.length
        }
    }
}
