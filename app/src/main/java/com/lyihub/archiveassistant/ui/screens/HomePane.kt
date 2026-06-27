package com.lyihub.archiveassistant.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lyihub.archiveassistant.R
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.Topic
import com.lyihub.archiveassistant.ui.components.TextActionButton
import com.lyihub.archiveassistant.ui.components.PaneContainer
import com.lyihub.archiveassistant.ui.components.PaneDivider
import com.lyihub.archiveassistant.ui.components.PaneHeader

private val PalaceGreen = Color(0xFF0F5A43)
private val PalaceGreenDeep = Color(0xFF0A3D31)
private val PalaceGreenDark = Color(0xFF092E27)
private val PalaceGold = Color(0xFFD6A43A)
private val PalaceGoldBlock = Color(0xFFE0B13C)
private val PalaceInk = Color(0xFF20352D)
private val PalacePaper = Color(0xFFFFF7E1)
private val PalaceLine = Color(0xFF628E62)
private val PalaceGridShape = RoundedCornerShape(6.dp)

private val DashboardFallbackTitles = listOf(
    "大模型架构研究",
    "UX/UI 灵感板",
    "阅读剪报",
    "旅行参考",
    "开源工具",
    "待归档",
)

private data class DashboardFolder(
    val id: String,
    val title: String,
    val itemCount: Int,
    val updatedAtEpochMillis: Long?,
    val topic: Topic?,
    val isGoldCell: Boolean,
)

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
    modifier: Modifier = Modifier,
) {
    PaneContainer(modifier = modifier.testTag("home-pane")) {
        PaneHeader(
            title = title,
            subtitle = "外屏总览",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(PalaceGreenDeep),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                PalaceHero(
                    totalItems = itemsByTopic.values.sumOf { it.size },
                    pendingCount = pendingCount(recentTopics, itemsByTopic),
                    isSmartSummarizing = isSmartSummarizing,
                )
            }
            item {
                QuickActionGrid(
                    parserInput = parserInput,
                    validationMessage = parserValidationMessage,
                    smartSummarizationMessage = smartSummarizationMessage,
                    onInputChanged = onParserInputChanged,
                    onSubmit = onSubmitParserInput,
                    onOpenClipboard = onOpenClipboard,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    isSmartSummarizing = isSmartSummarizing,
                )
            }
            item {
                WorkflowStrip()
            }
            item {
                TopicGrid(
                    topics = recentTopics,
                    itemsByTopic = itemsByTopic,
                    searchQuery = searchQuery,
                    onTopicSelected = onTopicSelected,
                    onCreateTopic = onCreateTopic,
                    onOpenManage = onOpenManage,
                )
            }
        }
    }
}

@Composable
private fun PalaceHero(
    totalItems: Int,
    pendingCount: Int,
    isSmartSummarizing: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PalaceGridShape,
        color = PalaceGreen,
        border = BorderStroke(1.dp, PalaceLine.copy(alpha = 0.72f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "朝堂视角",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PalaceGold,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "中书拟题 · 门下筛选 · 尚书归档",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.74f),
                    )
                }
                StatusSeal(
                    label = if (isSmartSummarizing) "拟录中" else "待批",
                    value = if (isSmartSummarizing) "中" else "6",
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBlock(label = "宣入新章", value = "12", modifier = Modifier.weight(1f))
                MetricBlock(label = "尚书藏档", value = totalItems.toString(), modifier = Modifier.weight(1f))
                MetricBlock(label = "门下待审", value = pendingCount.toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusSeal(
    label: String,
    value: String,
) {
    Surface(
        shape = PalaceGridShape,
        color = PalaceGold,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = PalaceGreenDark,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = PalaceGreenDark,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MetricBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = PalaceGridShape,
        color = PalaceGreenDark.copy(alpha = 0.64f),
        border = BorderStroke(1.dp, PalaceGold.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = PalaceGold,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun QuickActionGrid(
    parserInput: String,
    validationMessage: String?,
    smartSummarizationMessage: String?,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenClipboard: () -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    isSmartSummarizing: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(
                title = "宣拾遗",
                subtitle = "再取剪切板",
                tone = PalaceGoldBlock,
                contentColor = PalaceGreenDark,
                onClick = onOpenClipboard,
                modifier = Modifier
                    .weight(1f)
                    .testTag("clipboard-button"),
            )
            ActionTile(
                title = if (isSmartSummarizing) "拟录中" else "中书拟题",
                subtitle = "归纳来源与拟归属",
                tone = Color(0xFF2F8D72),
                contentColor = Color.White,
                onClick = onSubmit,
                enabled = !isSmartSummarizing && parserInput.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("classify-button"),
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = PalaceGridShape,
            color = PalacePaper,
            border = BorderStroke(1.dp, Color(0xFFE0C383)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "藏经阁",
                        style = MaterialTheme.typography.titleSmall,
                        color = PalaceInk,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "检索旧档",
                        style = MaterialTheme.typography.labelMedium,
                        color = PalaceInk.copy(alpha = 0.62f),
                    )
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home-search-input"),
                    placeholder = {
                        Text(
                            text = "查找主题或资料...",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = PalaceInk.copy(alpha = 0.72f),
                        )
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.72f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.72f),
                        focusedBorderColor = PalaceGold,
                        unfocusedBorderColor = Color(0xFFD9BF82),
                    ),
                )
                HiddenParserInput(
                    input = parserInput,
                    validationMessage = validationMessage,
                    smartSummarizationMessage = smartSummarizationMessage,
                    onInputChanged = onInputChanged,
                )
            }
        }
    }
}

@Composable
private fun HiddenParserInput(
    input: String,
    validationMessage: String?,
    smartSummarizationMessage: String?,
    onInputChanged: (String) -> Unit,
) {
    BasicTextField(
        value = input,
        onValueChange = onInputChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(0.dp)
            .alpha(0f)
            .testTag("parser-input"),
        singleLine = true,
    )
    if (validationMessage != null || smartSummarizationMessage != null) {
        Text(
            text = validationMessage ?: smartSummarizationMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    tone: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier
            .aspectRatio(1.58f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = PalaceGridShape,
        color = if (enabled) tone.copy(alpha = 0.92f) else Color(0xFFD9D2C4),
        border = BorderStroke(1.dp, if (enabled) PalaceLine.copy(alpha = 0.4f) else Color.Transparent),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DecorativePlaceholder(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(42.dp),
                alpha = 0.28f,
                tint = if (contentColor == Color.White) Color.White else PalaceGreen,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) contentColor else Color(0xFF7A7164),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (enabled) contentColor.copy(alpha = 0.78f) else Color(0xFF7A7164),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WorkflowStrip() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PalaceGridShape,
        color = PalaceGreen,
        border = BorderStroke(1.dp, PalaceLine.copy(alpha = 0.72f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "运行链路",
                style = MaterialTheme.typography.titleSmall,
                color = PalaceGold,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                WorkflowNode("尚书", "旧档", Modifier.weight(1f))
                WorkflowNode("中书", "拟题", Modifier.weight(1f))
                WorkflowNode("门下", "过筛", Modifier.weight(1f))
                WorkflowNode("御前", "待批", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WorkflowNode(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = PalaceGridShape,
        color = PalaceGreenDark.copy(alpha = 0.36f),
        border = BorderStroke(1.dp, PalaceGold.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = PalaceGold,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun TopicGrid(
    topics: List<Topic>,
    itemsByTopic: Map<String, List<KnowledgeItem>>,
    searchQuery: String,
    onTopicSelected: (String) -> Unit,
    onCreateTopic: () -> Unit,
    onOpenManage: () -> Unit,
) {
    val folders = dashboardFolders(topics, itemsByTopic)
    Column(modifier = Modifier.testTag("recent-topic-list")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "尚书档案",
                    style = MaterialTheme.typography.titleMedium,
                    color = PalaceGold,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "六个固定文件夹",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextActionButton(
                    label = "新建",
                    onClick = onCreateTopic,
                    testTag = "home-create-topic-button",
                    icon = Icons.Default.Add,
                    contentColor = PalaceGold,
                )
                TextActionButton(
                    label = "管理",
                    onClick = onOpenManage,
                    testTag = "manage-button",
                    icon = Icons.AutoMirrored.Filled.List,
                    contentColor = PalaceGold,
                )
            }
        }
        folders.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { folder ->
                    TopicCard(
                        folder = folder,
                        searchQuery = searchQuery,
                        onClick = { folder.topic?.let { onTopicSelected(it.id) } },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TopicCard(
    folder: DashboardFolder,
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = folder.topic?.let { topicColor(it.iconColor) } ?: PalaceGold
    val cellColor = if (folder.isGoldCell) PalaceGoldBlock else PalaceGreen
    val textColor = if (folder.isGoldCell) PalaceGreenDark else PalacePaper
    val accentColor = if (folder.isGoldCell) PalaceGreenDark else PalaceGold
    Surface(
        color = cellColor,
        shape = PalaceGridShape,
        border = BorderStroke(1.dp, PalaceLine.copy(alpha = 0.58f)),
        modifier = modifier
            .aspectRatio(1.18f)
            .clickable(enabled = folder.topic != null, onClick = onClick)
            .testTag("topic-card-${folder.id}"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DecorativePlaceholder(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(52.dp),
                alpha = if (folder.isGoldCell) 0.26f else 0.42f,
                tint = if (folder.isGoldCell) PalaceGreen else PalaceGold,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (searchQuery.isBlank()) {
                        androidx.compose.ui.text.AnnotatedString(folder.title)
                    } else {
                        buildHighlightedText(
                            text = folder.title,
                            query = searchQuery,
                            highlightColor = tone,
                            highlightBgColor = tone.copy(alpha = 0.15f),
                        )
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = folder.updatedAtEpochMillis?.let { "${friendlyTime(it)} · ${folder.itemCount} 项" }
                        ?: "待启用 · 0 项",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor.copy(alpha = 0.82f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DecorativePlaceholder(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    tint: Color? = null,
) {
    Image(
        painter = painterResource(id = R.drawable.dashboard_placeholder),
        contentDescription = null,
        modifier = modifier.clip(PalaceGridShape),
        contentScale = ContentScale.Crop,
        alpha = alpha,
        colorFilter = tint?.let { ColorFilter.tint(it) },
    )
}

@Composable
private fun MemorialDemoButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, PalaceGold.copy(alpha = 0.8f), MaterialTheme.shapes.small)
            .testTag("memorial-demo-button"),
        colors = ButtonDefaults.textButtonColors(
            containerColor = PalaceGreen.copy(alpha = 0.08f),
            contentColor = PalaceGreen,
        ),
    ) {
        Text(
            text = "奏章",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun pendingCount(
    topics: List<Topic>,
    itemsByTopic: Map<String, List<KnowledgeItem>>,
): Int = topics.take(3).sumOf { topic ->
    ((itemsByTopic[topic.id]?.size ?: 0) + topic.title.length) % 3
}

private fun dashboardFolders(
    topics: List<Topic>,
    itemsByTopic: Map<String, List<KnowledgeItem>>,
): List<DashboardFolder> {
    return List(6) { index ->
        val topic = topics.getOrNull(index)
        DashboardFolder(
            id = topic?.id ?: "dashboard-folder-${index + 1}",
            title = topic?.title ?: DashboardFallbackTitles[index],
            itemCount = topic?.let { itemsByTopic[it.id]?.size ?: 0 } ?: 0,
            updatedAtEpochMillis = topic?.updatedAtEpochMillis,
            topic = topic,
            isGoldCell = index == 1 || index == 4,
        )
    }
}

private fun topicColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    PalaceGreen
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
