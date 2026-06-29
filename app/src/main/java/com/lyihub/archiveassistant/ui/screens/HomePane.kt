package com.lyihub.archiveassistant.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.lyihub.archiveassistant.ui.theme.ImperialCinnabar
import com.lyihub.archiveassistant.ui.theme.ImperialDisplayFont
import com.lyihub.archiveassistant.ui.theme.ImperialIvory
import com.lyihub.archiveassistant.ui.theme.ImperialStampTitleFont
import com.lyihub.archiveassistant.ui.theme.ImperialUmber

private val HomeInk = ImperialUmber
private val HomePaper = ImperialIvory

private val MinistryTicketShape = GenericShape { size, _ ->
  val notchRadius = (size.height * 0.25f).coerceIn(12f, 21f)
  val toothRadius = (size.height * 0.08f).coerceIn(5f, 8.5f)
  val toothCount = 8
  val toothPitch = size.width / toothCount

  moveTo(0f, 0f)
  repeat(toothCount) { index ->
    val left = toothPitch * index
    val mid = left + toothPitch * 0.5f
    val right = left + toothPitch
    lineTo(mid - toothRadius, 0f)
    quadraticTo(mid, toothRadius, mid + toothRadius, 0f)
    lineTo(right, 0f)
  }
  lineTo(size.width, size.height * 0.5f - notchRadius)
  quadraticTo(
    size.width - notchRadius,
    size.height * 0.5f,
    size.width,
    size.height * 0.5f + notchRadius,
  )
  lineTo(size.width, size.height)
  for (index in toothCount - 1 downTo 0) {
    val right = toothPitch * (index + 1)
    val mid = toothPitch * index + toothPitch * 0.5f
    val left = toothPitch * index
    lineTo(mid + toothRadius, size.height)
    quadraticTo(mid, size.height - toothRadius, mid - toothRadius, size.height)
    lineTo(left.coerceAtLeast(0f), size.height)
  }
  lineTo(0f, size.height * 0.5f + notchRadius)
  quadraticTo(notchRadius, size.height * 0.5f, 0f, size.height * 0.5f - notchRadius)
  close()
}

private val FolderFallbackTitles =
  listOf(
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
  parserValidationMessage: String?,
  recentTopics: List<Topic>,
  itemsByTopic: Map<String, List<KnowledgeItem>>,
  searchQuery: String,
  onTopicSelected: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onCreateTopic: () -> Unit,
  onRenameTopic: (String) -> Unit = {},
  onDeleteTopic: (String) -> Unit = {},
  onSearchQueryChanged: (String) -> Unit,
  onOpenClipboard: () -> Unit,
  onOpenMemorialDemo: (() -> Unit)? = null,
  smartSummarizationMessage: String? = null,
  modifier: Modifier = Modifier,
) {
  val pendingCount = pendingCount(recentTopics, itemsByTopic)
  val folders = dashboardFolders(recentTopics, itemsByTopic, searchQuery)
  var isManagingMinistries by remember { mutableStateOf(false) }

  Box(modifier = modifier.testTag("home-pane").fillMaxSize()) {
    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
      HomeContentColumn(
        modifier =
          Modifier.padding(
              start = 24.dp,
              top = 56.dp,
              end = 24.dp,
              bottom = 36.dp,
            )
            .fillMaxWidth()
      ) {
        HomeMosaic(
          appTitle = title,
          pendingCount = pendingCount,
          folders = folders,
          validationMessage = parserValidationMessage,
          smartSummarizationMessage = smartSummarizationMessage,
          searchQuery = searchQuery,
          onOpenClipboard = onOpenClipboard,
          onOpenMemorialDemo = onOpenMemorialDemo,
          onSearchQueryChanged = onSearchQueryChanged,
          onTopicSelected = onTopicSelected,
          onOpenSettings = onOpenSettings,
          onCreateTopic = onCreateTopic,
          onRenameTopic = onRenameTopic,
          onDeleteTopic = onDeleteTopic,
          isManagingMinistries = isManagingMinistries,
          onToggleManage = { isManagingMinistries = !isManagingMinistries },
        )
      }
    }
  }
}

@Composable
private fun HomeContentColumn(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(14.dp),
    content = content,
  )
}

@Composable
private fun ColumnScope.HomeMosaic(
  appTitle: String,
  pendingCount: Int,
  folders: List<DashboardFolder>,
  validationMessage: String?,
  smartSummarizationMessage: String?,
  searchQuery: String,
  onOpenClipboard: () -> Unit,
  onOpenMemorialDemo: (() -> Unit)?,
  onSearchQueryChanged: (String) -> Unit,
  onTopicSelected: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onCreateTopic: () -> Unit,
  onRenameTopic: (String) -> Unit,
  onDeleteTopic: (String) -> Unit,
  isManagingMinistries: Boolean,
  onToggleManage: () -> Unit,
) {
  HomeHeaderRow(
    appTitle = appTitle,
    onOpenSettings = onOpenSettings,
    modifier = Modifier.fillMaxWidth(),
  )
  PalaceDashboardBlock(
    pendingCount = pendingCount,
    onOpenClipboard = onOpenClipboard,
    onOpenMemorialDemo = onOpenMemorialDemo,
    searchQuery = searchQuery,
    onSearchQueryChanged = onSearchQueryChanged,
    validationMessage = validationMessage,
    smartSummarizationMessage = smartSummarizationMessage,
    modifier = Modifier.fillMaxWidth(),
  )
  MinistryStampStack(
    searchQuery = searchQuery,
    resultCount = folders.count { it.topic != null },
    folders = folders,
    onTopicSelected = onTopicSelected,
    onCreateTopic = onCreateTopic,
    onRenameTopic = onRenameTopic,
    onDeleteTopic = onDeleteTopic,
    isManagingMinistries = isManagingMinistries,
    onToggleManage = onToggleManage,
    modifier = Modifier.fillMaxWidth(),
  )
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
  Box(modifier = modifier.fillMaxWidth().padding(bottom = 4.dp)) {
    IconButton(
      onClick = onOpenSettings,
      modifier = Modifier.align(Alignment.TopEnd).testTag("settings-trigger"),
    ) {
      Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "设置",
        tint = Color.Black.copy(alpha = 0.82f),
      )
    }
    Column(
      modifier = Modifier.align(Alignment.CenterStart).padding(end = 64.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = appTitle,
        style = MaterialTheme.typography.displayLarge,
        color = Color.Black,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
      )
      Text(
        text = "中书录入 · 批奏折 · 尚书归档",
        style = MaterialTheme.typography.titleSmall,
        color = Color.Black.copy(alpha = 0.72f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun HomeFeatureCell(
  title: String,
  subtitle: String,
  label: String = "",
  contentColor: Color,
  @DrawableRes ornamentRes: Int,
  tileVisual: ArchiveTileVisual,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  large: Boolean = false,
) {
  CutoutCell(
    modifier =
      modifier.fillMaxSize().clickable(enabled = enabled, onClick = onClick).testTag(testTag),
    contentColor = contentColor,
    tileVisual = tileVisual,
  ) {
    HomeOrnament(
      imageRes = ornamentRes,
      tint = contentColor,
      modifier =
        Modifier.align(Alignment.CenterEnd)
          .offset(x = if (large) 22.dp else 10.dp)
          .size(if (large) 132.dp else 68.dp),
      alpha = if (large) 0.5f else 0.58f,
    )
    if (label.isNotBlank()) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor.copy(alpha = 0.7f),
        modifier = Modifier.align(Alignment.TopStart).padding(horizontal = 12.dp, vertical = 10.dp),
      )
    }
    Column(modifier = Modifier.align(Alignment.BottomStart).padding(if (large) 18.dp else 12.dp)) {
      Text(
        text = title,
        style =
          if (large) MaterialTheme.typography.headlineMedium
          else MaterialTheme.typography.titleLarge,
        color = contentColor,
        fontWeight = FontWeight.Normal,
        maxLines = if (large) 2 else 1,
      )
      Text(
        text = subtitle,
        style =
          if (large) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
        color = contentColor.copy(alpha = 0.76f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun PalaceDashboardBlock(
  pendingCount: Int,
  onOpenClipboard: () -> Unit,
  onOpenMemorialDemo: (() -> Unit)?,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  validationMessage: String?,
  smartSummarizationMessage: String?,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier = modifier) {
    val gap = 12.dp
    val bottomSquareWidth = (maxWidth - gap) / 3f
    val searchRowHeight = bottomSquareWidth
    val topColumnWidth = (maxWidth - gap) / 2f
    val topRowHeight = topColumnWidth
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(gap),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().height(topRowHeight),
        horizontalArrangement = Arrangement.spacedBy(gap),
      ) {
        Column(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(gap),
        ) {
          HomeFeatureCell(
            title = "中书录入",
            subtitle = "拾取、摘要、拟题",
            contentColor = Color.White,
            ornamentRes = R.drawable.imperial_ornament_lantern,
            tileVisual = ZhongshuTileVisual,
            modifier = Modifier.weight(1f),
            onClick = {},
            testTag = "workflow-zhongshu-cell",
            enabled = false,
          )
          HomeFeatureCell(
            title = "门下递奏",
            subtitle = "筛选、预览、待批",
            contentColor = Color.White,
            ornamentRes = R.drawable.imperial_ornament_ruyi,
            tileVisual = MenxiaTileVisual,
            modifier = Modifier.weight(1f),
            onClick = {},
            testTag = "workflow-menxia-cell",
            enabled = false,
          )
        }
        MemorialCell(
          pendingCount = pendingCount,
          onClick = onOpenMemorialDemo,
          modifier = Modifier.weight(1f).height(topRowHeight),
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth().height(searchRowHeight),
        horizontalArrangement = Arrangement.spacedBy(gap),
      ) {
        HomeFeatureCell(
          title = "宣拾遗",
          subtitle = "读取剪切板",
          contentColor = Color.White,
          ornamentRes = R.drawable.imperial_ornament_gourd,
          tileVisual = ClipboardTileVisual,
          modifier = Modifier.weight(1f).height(searchRowHeight),
          onClick = onOpenClipboard,
          testTag = "clipboard-button",
        )
        SearchCell(
          searchQuery = searchQuery,
          onSearchQueryChanged = onSearchQueryChanged,
          validationMessage = validationMessage,
          smartSummarizationMessage = smartSummarizationMessage,
          modifier = Modifier.weight(2f).height(searchRowHeight),
        )
      }
    }
  }
}

@Composable
private fun SearchCell(
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  validationMessage: String?,
  smartSummarizationMessage: String?,
  modifier: Modifier = Modifier,
) {
  CutoutCell(
    modifier = modifier.fillMaxSize(),
    contentColor = Color.White,
    tileVisual = SearchTileVisual,
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(14.dp),
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
          color = Color.White,
          fontWeight = FontWeight.Normal,
        )
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "搜索",
          tint = Color.White.copy(alpha = 0.76f),
        )
      }
      BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
        modifier = Modifier.fillMaxWidth().testTag("home-search-input"),
        decorationBox = { innerTextField ->
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color.White.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 8.dp)
          ) {
            if (searchQuery.isBlank()) {
              Text(
                text = "查找主题或资料...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.62f),
              )
            }
            innerTextField()
          }
        },
      )
      val message = validationMessage ?: smartSummarizationMessage
      Text(
        text =
          message
            ?: if (searchQuery.isBlank()) {
              "输入后筛选下方文件夹与资料"
            } else {
              "正在筛选相关文件夹"
            },
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = if (message == null) 0.72f else 0.9f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
  CutoutCell(
    modifier =
      modifier
        .fillMaxSize()
        .clickable(enabled = onClick != null) { onClick?.invoke() }
        .testTag("memorial-entry-card"),
    contentColor = Color.White,
    tileVisual = MemorialTileVisual,
  ) {
    HomeOrnament(
      imageRes = R.drawable.imperial_ornament_gate_guard,
      tint = Color.White,
      modifier = Modifier.align(Alignment.CenterEnd).offset(x = 24.dp).size(138.dp),
      alpha = 0.48f,
    )
    Column(modifier = Modifier.align(Alignment.BottomStart).padding(18.dp)) {
      Text(
        text = "批奏折",
        style = MaterialTheme.typography.headlineLarge,
        color = Color.White,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
      )
      Text(
        text = "今日 $pendingCount 封待批奏章",
        style = MaterialTheme.typography.titleSmall,
        color = Color.White.copy(alpha = 0.76f),
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun MinistryStampStack(
  searchQuery: String,
  resultCount: Int,
  folders: List<DashboardFolder>,
  onTopicSelected: (String) -> Unit,
  onCreateTopic: () -> Unit,
  onRenameTopic: (String) -> Unit,
  onDeleteTopic: (String) -> Unit,
  isManagingMinistries: Boolean,
  onToggleManage: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val stackShadowShape = RoundedCornerShape(8.dp)
  Column(
    modifier =
      modifier.shadow(11.dp, stackShadowShape, clip = false).testTag("ministry-stamp-stack"),
    verticalArrangement = Arrangement.spacedBy(0.dp),
  ) {
    MinistryTicketSurface(
      modifier = Modifier.fillMaxWidth(),
      borderAlpha = 0.28f,
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val headerStyle = MaterialTheme.typography.titleLarge
        Text(
          text =
            buildAnnotatedString {
              withStyle(
                SpanStyle(
                  color = ImperialCinnabar,
                  fontFamily = ImperialStampTitleFont,
                  fontSize = headerStyle.fontSize,
                )
              ) {
                append("「尚书省」")
              }
              withStyle(
                SpanStyle(
                  color = Color.Black,
                  fontFamily = ImperialDisplayFont,
                  fontSize = headerStyle.fontSize,
                )
              ) {
                append("最近主题")
              }
            },
          style = headerStyle,
          fontWeight = FontWeight.Normal,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          lineHeight = headerStyle.lineHeight,
          modifier = Modifier.weight(1f),
        )
        MinistryHeaderAction(
          title = "新建",
          onClick = onCreateTopic,
          testTag = "home-create-topic-button",
        )
        MinistryHeaderAction(
          title = if (isManagingMinistries) "完成" else "管理",
          onClick = onToggleManage,
          testTag = "manage-button",
        )
      }
    }
    FolderResultList(
      folders = folders,
      searchQuery = searchQuery,
      onTopicSelected = onTopicSelected,
      onRenameTopic = onRenameTopic,
      onDeleteTopic = onDeleteTopic,
      isManagingMinistries = isManagingMinistries,
      compact = true,
    )
  }
}

@Composable
private fun MinistryHeaderAction(
  title: String,
  onClick: () -> Unit,
  testTag: String,
) {
  Box(
    modifier =
      Modifier.padding(start = 6.dp)
        .background(Color.White.copy(alpha = 0.52f), RoundedCornerShape(4.dp))
        .border(0.7.dp, Color.Black.copy(alpha = 0.16f), RoundedCornerShape(4.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = Color.Black,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
      maxLines = 1,
    )
  }
}

@Composable
private fun FolderResultList(
  folders: List<DashboardFolder>,
  searchQuery: String,
  onTopicSelected: (String) -> Unit,
  onRenameTopic: (String) -> Unit,
  onDeleteTopic: (String) -> Unit,
  isManagingMinistries: Boolean,
  compact: Boolean,
) {
  if (folders.isEmpty()) {
    PlainCell(
      modifier = Modifier.fillMaxWidth().height(if (compact) 74.dp else 96.dp),
      color = HomePaper,
      contentColor = HomeInk,
    ) {
      Column(
        modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "未找到相关文件夹",
          style = MaterialTheme.typography.titleLarge,
          color = HomeInk,
          fontWeight = FontWeight.Normal,
        )
        Text(
          text = "藏经阁暂未检出「$searchQuery」相关内容",
          style = MaterialTheme.typography.bodyMedium,
          color = HomeInk.copy(alpha = 0.68f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    return
  }
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(0.dp),
  ) {
    folders.forEachIndexed { index, folder ->
      MinistryTicketCard(
        folder = folder,
        visual = folderVisual(index),
        onTopicSelected = onTopicSelected,
        onRenameTopic = onRenameTopic,
        onDeleteTopic = onDeleteTopic,
        isManagingMinistries = isManagingMinistries,
        modifier = Modifier.fillMaxWidth().height(if (compact) 72.dp else 94.dp),
        compact = compact,
      )
    }
  }
}

@Composable
private fun MinistryTicketCard(
  folder: DashboardFolder,
  visual: FolderVisual,
  onTopicSelected: (String) -> Unit,
  onRenameTopic: (String) -> Unit,
  onDeleteTopic: (String) -> Unit,
  isManagingMinistries: Boolean,
  modifier: Modifier = Modifier,
  compact: Boolean,
) {
  val enabled = folder.topic != null
  val imageSize = if (compact) 34.dp else 64.dp
  val titleStyle =
    if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge
  val summaryStyle =
    if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
  MinistryTicketSurface(
    modifier =
      modifier
        .clickable(enabled = enabled && !isManagingMinistries) {
          folder.topic?.let { onTopicSelected(it.id) }
        }
        .testTag("topic-card-${folder.id}"),
    borderAlpha = 0.22f,
  ) {
    Row(
      modifier =
        Modifier.fillMaxSize().padding(start = 16.dp, top = 7.dp, end = 12.dp, bottom = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp),
      ) {
        Text(
          text = folder.title,
          style = titleStyle,
          color = Color.Black.copy(alpha = 0.88f),
          fontWeight = FontWeight.Normal,
          maxLines = 1,
        )
        Text(
          text = visual.description,
          style = summaryStyle,
          color = Color.Black.copy(alpha = 0.52f),
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Column(
        modifier =
          Modifier.width(if (isManagingMinistries) 104.dp else if (compact) 78.dp else 104.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        if (isManagingMinistries && folder.topic != null) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MinistryInlineAction(
              title = "改名",
              onClick = { onRenameTopic(folder.topic.id) },
              testTag = "rename-topic-button-${folder.id}",
            )
            MinistryInlineAction(
              title = "删除",
              onClick = { onDeleteTopic(folder.topic.id) },
              testTag = "delete-topic-button-${folder.id}",
            )
          }
        } else {
          Image(
            painter = painterResource(id = visual.imageRes),
            contentDescription = null,
            modifier = Modifier.size(imageSize),
            contentScale = ContentScale.Fit,
          )
          Text(
            text =
              folder.updatedAtEpochMillis?.let { "${folder.itemCount} 篇 · ${friendlyTime(it)}" }
                ?: "${folder.itemCount} 篇 · 待启用",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black.copy(alpha = 0.48f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun MinistryInlineAction(
  title: String,
  onClick: () -> Unit,
  testTag: String,
) {
  Box(
    modifier =
      Modifier.background(Color.White.copy(alpha = 0.58f), RoundedCornerShape(4.dp))
        .border(0.7.dp, Color.Black.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = Color.Black,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
      maxLines = 1,
    )
  }
}

@Composable
private fun MinistryTicketSurface(
  modifier: Modifier = Modifier,
  borderAlpha: Float,
  content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
  Box(modifier = modifier.clip(MinistryTicketShape).background(Color.White, MinistryTicketShape)) {
    Image(
      painter = painterResource(id = R.drawable.home_search_tile),
      contentDescription = null,
      modifier = Modifier.matchParentSize(),
      contentScale = ContentScale.Crop,
    )
    Box(modifier = Modifier.matchParentSize().background(Color.White.copy(alpha = 0.24f)))
    content()
  }
}

@Composable
private fun HomeOrnament(
  @DrawableRes imageRes: Int,
  tint: Color,
  modifier: Modifier = Modifier,
  alpha: Float = 0.5f,
) {
  Image(
    painter = painterResource(id = imageRes),
    contentDescription = null,
    modifier = modifier,
    contentScale = ContentScale.Fit,
    colorFilter = ColorFilter.tint(tint.copy(alpha = 0.92f)),
    alpha = alpha,
  )
}

@Composable
private fun PlainCell(
  modifier: Modifier,
  color: Color,
  contentColor: Color,
  content: @Composable BoxScopeWithContentColor.() -> Unit,
) {
  val shape = RoundedCornerShape(6.dp)
  Box(modifier = modifier.clip(shape).background(color, shape)) {
    BoxScopeWithContentColor(this, contentColor).content()
  }
}

@Composable
private fun CutoutCell(
  modifier: Modifier,
  contentColor: Color,
  tileVisual: ArchiveTileVisual,
  content: @Composable BoxScopeWithContentColor.() -> Unit,
) {
  Box(
    modifier =
      modifier
        .shadow(8.dp, ArchiveCutCornerShape, clip = false)
        .clip(ArchiveCutCornerShape)
        .background(HomePaper, ArchiveCutCornerShape)
        .border(1.2.dp, tileVisual.borderColor.copy(alpha = 0.86f), ArchiveCutCornerShape)
  ) {
    Image(
      painter = painterResource(id = tileVisual.backgroundRes),
      contentDescription = null,
      modifier = Modifier.matchParentSize(),
      contentScale = ContentScale.Crop,
    )
    Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.18f)))
    BoxScopeWithContentColor(this, contentColor).content()
  }
}

private class BoxScopeWithContentColor(
  private val boxScope: androidx.compose.foundation.layout.BoxScope,
  val contentColor: Color,
) : androidx.compose.foundation.layout.BoxScope by boxScope

private fun pendingCount(
  topics: List<Topic>,
  itemsByTopic: Map<String, List<KnowledgeItem>>,
): Int =
  topics.take(3).sumOf { topic ->
    ((itemsByTopic[topic.id]?.size ?: 0) + topic.title.length) % 3
  }

private fun dashboardFolders(
  topics: List<Topic>,
  itemsByTopic: Map<String, List<KnowledgeItem>>,
  searchQuery: String = "",
): List<DashboardFolder> {
  if (searchQuery.isNotBlank()) {
    return topics.map { topic ->
      DashboardFolder(
        id = topic.id,
        title = topic.title,
        itemCount = itemsByTopic[topic.id]?.size ?: 0,
        updatedAtEpochMillis = topic.updatedAtEpochMillis,
        topic = topic,
      )
    }
  }
  return List(6) { index ->
    val topic = topics.getOrNull(index)
    DashboardFolder(
      id = topic?.id ?: "dashboard-folder-${index + 1}",
      title = topic?.title ?: FolderFallbackTitles[index],
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
