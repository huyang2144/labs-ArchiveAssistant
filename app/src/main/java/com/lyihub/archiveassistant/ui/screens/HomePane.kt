package com.lyihub.archiveassistant.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.text.BasicTextField
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.lyihub.archiveassistant.ui.theme.ImperialBronze
import com.lyihub.archiveassistant.ui.theme.ImperialCinnabar
import com.lyihub.archiveassistant.ui.theme.ImperialIvory
import com.lyihub.archiveassistant.ui.theme.ImperialLightGold
import com.lyihub.archiveassistant.ui.theme.ImperialParchment
import com.lyihub.archiveassistant.ui.theme.ImperialUmber

private val PalaceGreen = ImperialParchment
private val PalaceGreenDeep = ImperialIvory
private val PalaceGreenDark = ImperialUmber
private val PalaceGold = ImperialUmber
private val PalaceGoldBlock = ImperialLightGold
private val PalaceInk = ImperialUmber
private val PalacePaper = ImperialIvory

private val MinistryTicketShape = GenericShape { size, _ ->
    val cut = size.height * 0.16f
    moveTo(cut, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, cut)
    lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height)
    lineTo(cut, size.height)
    lineTo(0f, size.height - cut)
    lineTo(0f, cut)
    close()
}

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
    val pendingCount = pendingCount(recentTopics, itemsByTopic)
    val folders = dashboardFolders(recentTopics, itemsByTopic)

    BoxWithConstraints(
        modifier = modifier
            .testTag("home-pane")
            .fillMaxSize()
            .background(PalaceGreenDeep),
    ) {
        val expanded = maxWidth >= 720.dp
        val horizontalPadding = if (expanded) 28.dp else 16.dp
        val topPadding = if (expanded) 54.dp else 36.dp
        val bottomPadding = if (expanded) 40.dp else 28.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            HomeContentColumn(
                expanded = expanded,
                modifier = Modifier
                    .padding(
                        start = horizontalPadding,
                        top = topPadding,
                        end = horizontalPadding,
                        bottom = bottomPadding,
                    )
                    .fillMaxWidth(),
            ) {
                if (expanded) {
                    ExpandedMosaic(
                        appTitle = title,
                        pendingCount = pendingCount,
                        folders = folders,
                        parserInput = parserInput,
                        validationMessage = parserValidationMessage,
                        smartSummarizationMessage = smartSummarizationMessage,
                        searchQuery = searchQuery,
                        isSmartSummarizing = isSmartSummarizing,
                        onInputChanged = onParserInputChanged,
                        onSubmit = onSubmitParserInput,
                        onOpenClipboard = onOpenClipboard,
                        onOpenMemorialDemo = onOpenMemorialDemo,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onTopicSelected = onTopicSelected,
                        onOpenManage = onOpenManage,
                        onCreateTopic = onCreateTopic,
                        onOpenSettings = onOpenSettings,
                    )
                } else {
                    CompactMosaic(
                        appTitle = title,
                        pendingCount = pendingCount,
                        folders = folders,
                        parserInput = parserInput,
                        validationMessage = parserValidationMessage,
                        smartSummarizationMessage = smartSummarizationMessage,
                        searchQuery = searchQuery,
                        isSmartSummarizing = isSmartSummarizing,
                        onInputChanged = onParserInputChanged,
                        onSubmit = onSubmitParserInput,
                        onOpenClipboard = onOpenClipboard,
                        onOpenMemorialDemo = onOpenMemorialDemo,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onTopicSelected = onTopicSelected,
                        onOpenManage = onOpenManage,
                        onCreateTopic = onCreateTopic,
                        onOpenSettings = onOpenSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContentColumn(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (expanded) 16.dp else 12.dp),
        content = content,
    )
}

@Composable
private fun ColumnScope.ExpandedMosaic(
    appTitle: String,
    pendingCount: Int,
    folders: List<DashboardFolder>,
    parserInput: String,
    validationMessage: String?,
    smartSummarizationMessage: String?,
    searchQuery: String,
    isSmartSummarizing: Boolean,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenClipboard: () -> Unit,
    onOpenMemorialDemo: (() -> Unit)?,
    onSearchQueryChanged: (String) -> Unit,
    onTopicSelected: (String) -> Unit,
    onOpenManage: () -> Unit,
    onCreateTopic: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    HomeHeaderRow(
        appTitle = appTitle,
        onOpenSettings = onOpenSettings,
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PalaceActionCell(
            title = "宣拾遗",
            subtitle = "重新读取剪切板",
            label = "中书",
            color = PalaceGoldBlock,
            contentColor = PalaceGreenDark,
            modifier = Modifier.weight(1f),
            onClick = onOpenClipboard,
            testTag = "clipboard-button",
        )
        PalaceActionCell(
            title = if (isSmartSummarizing) "拟录中" else "中书拟题",
            subtitle = "摘要、归类、拟入档",
            label = "拟录",
            color = PalaceGreen,
            contentColor = PalaceGold,
            modifier = Modifier.weight(1f),
            enabled = !isSmartSummarizing && parserInput.isNotBlank(),
            onClick = onSubmit,
            testTag = "classify-button",
        )
        SearchCell(
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            parserInput = parserInput,
            validationMessage = validationMessage,
            smartSummarizationMessage = smartSummarizationMessage,
            onInputChanged = onInputChanged,
            modifier = Modifier.weight(1.25f),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MemorialCell(
            pendingCount = pendingCount,
            onClick = onOpenMemorialDemo,
            modifier = Modifier.weight(1.2f),
        )
        WorkflowRow(modifier = Modifier.weight(1.8f))
    }
    MinistryHeaderRow(
        onCreateTopic = onCreateTopic,
        onOpenManage = onOpenManage,
    )
    folders.forEachIndexed { index, folder ->
        MinistryTicketCard(
            folder = folder,
            visual = ministryVisual(index),
            onTopicSelected = onTopicSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp),
            compact = false,
        )
    }
}

@Composable
private fun ColumnScope.CompactMosaic(
    appTitle: String,
    pendingCount: Int,
    folders: List<DashboardFolder>,
    parserInput: String,
    validationMessage: String?,
    smartSummarizationMessage: String?,
    searchQuery: String,
    isSmartSummarizing: Boolean,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenClipboard: () -> Unit,
    onOpenMemorialDemo: (() -> Unit)?,
    onSearchQueryChanged: (String) -> Unit,
    onTopicSelected: (String) -> Unit,
    onOpenManage: () -> Unit,
    onCreateTopic: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    HomeHeaderRow(
        appTitle = appTitle,
        onOpenSettings = onOpenSettings,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PalaceActionCell(
            title = "宣拾遗",
            subtitle = "读取剪切板",
            label = "中书",
            color = PalaceGoldBlock,
            contentColor = PalaceGreenDark,
            modifier = Modifier.weight(1f),
            onClick = onOpenClipboard,
            testTag = "clipboard-button",
        )
        PalaceActionCell(
            title = if (isSmartSummarizing) "拟录中" else "中书拟题",
            subtitle = "摘要归类",
            label = "拟录",
            color = PalaceGreen,
            contentColor = PalaceGold,
            modifier = Modifier.weight(1f),
            enabled = !isSmartSummarizing && parserInput.isNotBlank(),
            onClick = onSubmit,
            testTag = "classify-button",
        )
    }
    SearchCell(
        searchQuery = searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        parserInput = parserInput,
        validationMessage = validationMessage,
        smartSummarizationMessage = smartSummarizationMessage,
        onInputChanged = onInputChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
    )
    MemorialCell(
        pendingCount = pendingCount,
        onClick = onOpenMemorialDemo,
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
    )
    WorkflowRow(modifier = Modifier.height(76.dp))
    MinistryHeaderRow(
        onCreateTopic = onCreateTopic,
        onOpenManage = onOpenManage,
    )
    folders.forEachIndexed { index, folder ->
        MinistryTicketCard(
            folder = folder,
            visual = ministryVisual(index),
            onTopicSelected = onTopicSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp),
            compact = true,
        )
    }
}

@Composable
private fun HomeHeaderRow(
    appTitle: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TitleCell(
        appTitle = appTitle,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
private fun TitleCell(
    appTitle: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MosaicCell(
        modifier = modifier.fillMaxSize(),
        color = PalaceGreen,
        contentColor = PalaceGold,
    ) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .testTag("settings-trigger"),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "设置",
                tint = PalaceGold.copy(alpha = 0.86f),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 22.dp, end = 72.dp),
        ) {
            Text(
                text = appTitle,
                style = MaterialTheme.typography.displaySmall,
                color = PalaceGold,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
            )
            Text(
                text = "中书录入 · 门下递奏 · 尚书归档",
                style = MaterialTheme.typography.bodyMedium,
                color = ImperialUmber.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PalaceActionCell(
    title: String,
    subtitle: String,
    label: String,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MosaicCell(
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        color = if (enabled) color else ImperialParchment,
        contentColor = contentColor,
    ) {
        DecorativePlaceholder(
            imageRes = if (label == "中书") {
                R.drawable.imperial_ornament_lantern
            } else {
                R.drawable.imperial_ornament_ruyi
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(56.dp),
            alpha = 0.28f,
            tint = contentColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchCell(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    parserInput: String,
    validationMessage: String?,
    smartSummarizationMessage: String?,
    onInputChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    MosaicCell(
        modifier = modifier.fillMaxSize(),
        color = PalacePaper,
        contentColor = PalaceInk,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "藏经阁",
                    style = MaterialTheme.typography.titleLarge,
                    color = PalaceInk,
                    fontWeight = FontWeight.Normal,
                )
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = PalaceInk.copy(alpha = 0.68f),
                )
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = PalaceInk),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home-search-input"),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ImperialIvory.copy(alpha = 0.74f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        if (searchQuery.isBlank()) {
                            Text(
                                text = "查找主题或资料...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PalaceInk.copy(alpha = 0.44f),
                            )
                        }
                        innerTextField()
                    }
                },
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

@Composable
private fun MemorialCell(
    pendingCount: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    MosaicCell(
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .testTag("memorial-entry-card"),
        color = PalaceGoldBlock,
        contentColor = PalaceGreenDark,
    ) {
        DecorativePlaceholder(
            imageRes = R.drawable.imperial_ornament_gourd,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .size(82.dp),
            alpha = 0.24f,
            tint = PalaceGreenDark,
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "门下递奏",
                style = MaterialTheme.typography.headlineSmall,
                color = PalaceGreenDark,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
            )
            Text(
                text = "今日 $pendingCount 封待批奏章",
                style = MaterialTheme.typography.bodyMedium,
                color = PalaceGreenDark.copy(alpha = 0.72f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WorkflowRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WorkflowCell("中书", "拾取、摘要、拟题", Modifier.weight(1f))
        WorkflowCell("门下", "筛选、递奏、待批", Modifier.weight(1f))
        WorkflowCell("尚书", "六部归藏", Modifier.weight(1f))
    }
}

@Composable
private fun WorkflowCell(title: String, subtitle: String, modifier: Modifier = Modifier) {
    MosaicCell(
        modifier = modifier.fillMaxSize(),
        color = PalaceGreen,
        contentColor = PalaceGold,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = PalaceGold,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ImperialUmber.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MinistryHeaderRow(
    onCreateTopic: () -> Unit,
    onOpenManage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MosaicCell(
            modifier = Modifier
                .weight(2f)
                .fillMaxSize(),
            color = PalaceGreenDark,
            contentColor = PalaceGold,
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = "尚书省",
                    style = MaterialTheme.typography.titleLarge,
                    color = PalaceGold,
                    fontWeight = FontWeight.Normal,
                )
                Text(
                    text = "六个固定文件夹 · 新建与管理",
                    style = MaterialTheme.typography.bodySmall,
                    color = ImperialUmber.copy(alpha = 0.62f),
                )
            }
        }
        HeaderActionCell(
            text = "新建",
            onClick = onCreateTopic,
            testTag = "home-create-topic-button",
            modifier = Modifier.weight(1f),
        )
        HeaderActionCell(
            text = "管理",
            onClick = onOpenManage,
            testTag = "manage-button",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MinistryTicketCard(
    folder: DashboardFolder,
    visual: MinistryVisual,
    onTopicSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean,
) {
    val enabled = folder.topic != null
    val bodyColor = visual.background
    val imageSize = if (compact) 78.dp else 104.dp
    val titleStyle = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
    val summaryStyle = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
    Box(
        modifier = modifier
            .padding(vertical = 3.dp)
            .clip(MinistryTicketShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        bodyColor,
                        bodyColor.copy(alpha = 0.94f),
                        ImperialIvory.copy(alpha = 0.92f),
                    ),
                ),
                MinistryTicketShape,
            )
            .border(1.dp, ImperialBronze.copy(alpha = 0.62f), MinistryTicketShape)
            .clickable(enabled = enabled) { folder.topic?.let { onTopicSelected(it.id) } }
            .testTag("topic-card-${folder.id}"),
    ) {
        Image(
            painter = painterResource(id = R.drawable.memorial_xuan_paper),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.22f,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            visual.accent.copy(alpha = 0.08f),
                            visual.accent.copy(alpha = 0.16f),
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = folder.title,
                        style = titleStyle,
                        color = PalaceInk,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                    )
                    Text(
                        text = visual.duty,
                        style = MaterialTheme.typography.labelMedium,
                        color = visual.accent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = visual.description,
                    style = summaryStyle,
                    color = PalaceInk.copy(alpha = 0.84f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = visual.duty,
                    style = MaterialTheme.typography.bodySmall,
                    color = PalaceInk.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Image(
                    painter = painterResource(id = visual.imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .height(imageSize)
                        .width(imageSize * 1.08f),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = folder.updatedAtEpochMillis?.let { "${folder.itemCount} 篇 · ${friendlyTime(it)}" }
                        ?: "${folder.itemCount} 篇 · 待启用",
                    style = MaterialTheme.typography.labelSmall,
                    color = visual.accent.copy(alpha = 0.84f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HeaderActionCell(
    text: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    MosaicCell(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = PalaceGoldBlock,
        contentColor = PalaceGreenDark,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = PalaceGreenDark,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun MosaicCell(
    modifier: Modifier,
    color: Color,
    contentColor: Color,
    content: @Composable BoxScopeWithContentColor.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(MinistryTicketShape)
            .background(color, MinistryTicketShape)
            .border(1.dp, ImperialBronze.copy(alpha = 0.46f), MinistryTicketShape),
    ) {
        BoxScopeWithContentColor(this, contentColor).content()
    }
}

private class BoxScopeWithContentColor(
    private val boxScope: androidx.compose.foundation.layout.BoxScope,
    val contentColor: Color,
) : androidx.compose.foundation.layout.BoxScope by boxScope

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
    val message = validationMessage ?: smartSummarizationMessage
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = ImperialCinnabar,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DecorativePlaceholder(
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    tint: Color? = null,
) {
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        alpha = alpha,
        colorFilter = tint?.let { ColorFilter.tint(it) },
    )
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
        )
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
    highlightColor: Color,
    highlightBgColor: Color,
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
