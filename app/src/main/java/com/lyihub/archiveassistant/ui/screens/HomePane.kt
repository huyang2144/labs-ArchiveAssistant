package com.lyihub.archiveassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.MinistryProfile
import com.lyihub.archiveassistant.domain.SixMinistryCatalog
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
    itemsByTopic: Map<String, List<KnowledgeItem>>,
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
    showMinistryGrid: Boolean = true,
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
                    CourtBriefingCard(totalItems = itemsByTopic.values.sumOf { it.size })
                }
            }
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
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "尚书省 · 六部归档",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "固定六部承接全部收藏，方便演示信息治理秩序",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextActionButton(
                                label = "新增",
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
                    val displayTopics = ministryOrderedTopics(recentTopics)
                    if (showMinistryGrid) {
                        displayTopics.chunked(2).forEach { rowTopics ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowTopics.forEach { topic ->
                                    TopicCard(
                                        topic = topic,
                                        itemCount = itemsByTopic[topic.id]?.size ?: 0,
                                        searchQuery = searchQuery,
                                        onClick = { onTopicSelected(topic.id) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (rowTopics.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        MinistryIndexStrip(
                            topics = displayTopics,
                            itemsByTopic = itemsByTopic,
                            onTopicSelected = onTopicSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinistryIndexStrip(
    topics: List<Topic>,
    itemsByTopic: Map<String, List<KnowledgeItem>>,
    onTopicSelected: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "六部索引",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${topics.sumOf { itemsByTopic[it.id]?.size ?: 0 }} 项案牍",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                topics.take(6).forEach { topic ->
                    MinistryIndexChip(
                        topic = topic,
                        itemCount = itemsByTopic[topic.id]?.size ?: 0,
                        onClick = { onTopicSelected(topic.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                text = "快速跳转至对应部档",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MinistryIndexChip(
    topic: Topic,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ministry = SixMinistryCatalog.profileForTopicId(topic.id)
    val accent = ministry?.color ?: parseColor(topic.iconColor)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = ministry?.name?.take(1) ?: topic.title.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = itemCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CourtBriefingCard(totalItems: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Color(0xFFFFF7E5),
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFD7B56D).copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "三省六部信息中枢",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4A3216),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "中书拟录 · 门下复核 · 尚书归档",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF765A2F),
                    )
                }
                Text(
                    text = "$totalItems 项",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF7A5518),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("录入", "审核", "归档", "批阅").forEach { label ->
                    FlowChip(label = label)
                }
            }
        }
    }
}

@Composable
private fun FlowChip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = Color(0xFFFFFFFF).copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFD7B56D).copy(alpha = 0.35f),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF7A5518),
            fontWeight = FontWeight.Medium,
        )
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionEyebrow(
                title = "中书省",
                subtitle = "录入 / 智能归纳",
            )
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
                    text = "入省后生成摘要、来源与拟归属",
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
                        label = if (isSmartSummarizing) "归纳中…" else "中书拟录",
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
}

@Composable
private fun TopicCard(
    topic: Topic,
    itemCount: Int,
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ministry = SixMinistryCatalog.profileForTopicId(topic.id)
    val accent = ministry?.color ?: parseColor(topic.iconColor)
    Surface(
        color = accent.copy(alpha = 0.07f),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("topic-card-${topic.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ministry?.name ?: topic.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = ministry?.domain ?: "主题归档",
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                MinistrySeal(text = ministry?.name?.take(1) ?: topic.title.take(1), color = accent)
            }
            Text(
                text = ministry?.responsibility ?: topic.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (searchQuery.isBlank()) {
                        androidx.compose.ui.text.AnnotatedString("尚书省案牍")
                    } else {
                        buildHighlightedText(
                            text = topic.title,
                            query = searchQuery,
                            highlightColor = MaterialTheme.colorScheme.primary,
                            highlightBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${itemCount} 项",
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private val MinistryProfile.color: Color
    get() = parseColor(colorHex)

@Composable
private fun SectionEyebrow(
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MinistrySeal(
    text: String,
    color: Color,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(MaterialTheme.shapes.small)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.38f), MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun ministryOrderedTopics(topics: List<Topic>): List<Topic> {
    val topicById = topics.associateBy { it.id }
    val ordered = SixMinistryCatalog.ministries.mapNotNull { topicById[it.topicId] }
    val extras = topics.filterNot { it.id in SixMinistryCatalog.topicIds }
    return ordered + extras
}

private fun parseColor(value: String): Color = try {
    Color(android.graphics.Color.parseColor(value))
} catch (_: Exception) {
    Color(0xFF8A6421)
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
