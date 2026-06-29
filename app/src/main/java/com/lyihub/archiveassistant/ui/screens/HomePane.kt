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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyihub.archiveassistant.R
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.Topic
import com.lyihub.archiveassistant.ui.theme.ImperialCinnabar
import com.lyihub.archiveassistant.ui.theme.ImperialDisplayFont
import com.lyihub.archiveassistant.ui.theme.ImperialIvory
import com.lyihub.archiveassistant.ui.theme.ImperialTitleFont
import com.lyihub.archiveassistant.ui.theme.ImperialUmber
import com.lyihub.archiveassistant.util.toChineseCount

private val HomeInk = ImperialUmber
private val HomePaper = ImperialIvory

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
  val pendingCount = TOTAL_PENDING_MEMORIALS
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
        text = "拾取资料、整理主题、轮盘批阅、瀑布流阅读",
        style = MaterialTheme.typography.titleSmall.copy(fontFamily = ImperialDisplayFont),
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
  ornamentSize: Dp = if (large) 132.dp else 68.dp,
  ornamentOffsetX: Dp = if (large) 22.dp else 10.dp,
  ornamentOffsetY: Dp = 0.dp,
  ornamentAlignment: Alignment = Alignment.CenterEnd,
  ornamentTint: Color? = null,
  mirrorOrnament: Boolean = false,
  textAlignment: Alignment = Alignment.BottomStart,
) {
  CutoutCell(
    modifier =
      modifier.fillMaxSize().clickable(enabled = enabled, onClick = onClick).testTag(testTag),
    contentColor = contentColor,
    tileVisual = tileVisual,
  ) {
    HomeOrnament(
      imageRes = ornamentRes,
      modifier =
        Modifier.align(ornamentAlignment)
          .offset(x = ornamentOffsetX, y = ornamentOffsetY)
          .size(ornamentSize)
          .graphicsLayer(scaleX = if (mirrorOrnament) -1f else 1f),
      alpha = if (large) 0.5f else 0.58f,
      tint = ornamentTint,
    )
    if (label.isNotBlank()) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor.copy(alpha = 0.7f),
        modifier = Modifier.align(Alignment.TopStart).padding(horizontal = 12.dp, vertical = 10.dp),
      )
    }
    Column(modifier = Modifier.align(textAlignment).padding(if (large) 18.dp else 12.dp)) {
      Text(
        text = title,
        style =
          if (large) {
            MaterialTheme.typography.headlineMedium.copy(fontFamily = ImperialTitleFont)
          } else {
            MaterialTheme.typography.titleLarge.copy(fontFamily = ImperialTitleFont)
          },
        color = contentColor,
        fontWeight = FontWeight.Normal,
        maxLines = if (large) 2 else 1,
      )
      Text(
        text = subtitle,
        style =
          if (large) {
            MaterialTheme.typography.bodyMedium.copy(fontFamily = ImperialDisplayFont)
          } else {
            MaterialTheme.typography.bodySmall.copy(fontFamily = ImperialDisplayFont)
          },
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
            ornamentRes = R.drawable.home_ornament_zhongshu,
            tileVisual = ZhongshuTileVisual,
            modifier = Modifier.weight(1f),
            onClick = {},
            testTag = "workflow-zhongshu-cell",
            enabled = false,
            ornamentSize = 96.dp,
            ornamentOffsetX = 18.dp,
            ornamentTint = Color.White,
          )
          HomeFeatureCell(
            title = "门下递奏",
            subtitle = "筛选、预览、待批",
            contentColor = Color.White,
            ornamentRes = R.drawable.home_ornament_menxia,
            tileVisual = MenxiaTileVisual,
            modifier = Modifier.weight(1f),
            onClick = {},
            testTag = "workflow-menxia-cell",
            enabled = false,
            ornamentSize = 88.dp,
            ornamentOffsetX = (-10).dp,
            ornamentAlignment = Alignment.CenterStart,
            textAlignment = Alignment.BottomEnd,
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
          ornamentRes = R.drawable.home_ornament_sanxingdui,
          tileVisual = ClipboardTileVisual,
          modifier = Modifier.weight(1f).height(searchRowHeight),
          onClick = onOpenClipboard,
          testTag = "clipboard-button",
          ornamentSize = 68.dp,
          ornamentOffsetX = (-8).dp,
          ornamentOffsetY = 10.dp,
          ornamentAlignment = Alignment.TopEnd,
          ornamentTint = Color.White,
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
    HomeOrnament(
      imageRes = R.drawable.home_ornament_library_11092,
      modifier = Modifier.align(Alignment.CenterEnd).offset(x = 18.dp).size(112.dp),
      alpha = 0.66f,
    )
    Column(
      modifier = Modifier.fillMaxSize().padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "藏经阁",
        style = MaterialTheme.typography.titleLarge.copy(fontFamily = ImperialTitleFont),
        color = Color.White,
        fontWeight = FontWeight.Normal,
      )
      BasicTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        singleLine = true,
        textStyle =
          MaterialTheme.typography.bodyMedium.copy(
            color = Color.White,
            fontFamily = ImperialDisplayFont,
          ),
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
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = ImperialDisplayFont),
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
    mirrorBackground = true,
  ) {
    HomeOrnament(
      imageRes = R.drawable.home_ornament_memorial,
      modifier = Modifier.align(Alignment.CenterEnd).offset(x = 24.dp, y = (-18).dp).size(138.dp),
      alpha = 0.66f,
    )
    Column(modifier = Modifier.align(Alignment.BottomStart).padding(13.dp)) {
      Text(
        text = "批奏折",
        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = ImperialTitleFont),
        color = Color.White,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
      )
      Text(
        text = "今日${pendingCount.toChineseCount()}封待批奏章",
        style = MaterialTheme.typography.titleSmall.copy(fontFamily = ImperialDisplayFont),
        color = Color.White.copy(alpha = 0.76f),
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun MinistryStampStack(
  searchQuery: String,
  folders: List<DashboardFolder>,
  onTopicSelected: (String) -> Unit,
  onCreateTopic: () -> Unit,
  onRenameTopic: (String) -> Unit,
  onDeleteTopic: (String) -> Unit,
  isManagingMinistries: Boolean,
  onToggleManage: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .shadow(14.dp, RoundedCornerShape(8.dp), clip = false)
        .testTag("ministry-stamp-stack"),
    verticalArrangement = Arrangement.spacedBy(0.dp),
  ) {
    MinistryFoldSurface(
      modifier = Modifier.fillMaxWidth(),
      shape = ArchiveCutCornerShape,
      foldIntensity = 0.22f,
      showEndFold = true,
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val headerFamily = ImperialTitleFont
        Text(
          text =
            buildAnnotatedString {
              withStyle(
                SpanStyle(
                  color = ImperialCinnabar,
                  fontFamily = headerFamily,
                  fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                )
              ) {
                append("「尚书省」")
              }
              withStyle(
                SpanStyle(
                  color = Color.Black,
                  fontFamily = headerFamily,
                  fontSize = MaterialTheme.typography.titleSmall.fontSize,
                )
              ) {
                append("最近主题")
              }
            },
          style = MaterialTheme.typography.titleMedium.copy(fontFamily = headerFamily),
          fontWeight = FontWeight.Normal,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          lineHeight = MaterialTheme.typography.headlineSmall.lineHeight,
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
      MinistryFoldCard(
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
private fun MinistryFoldCard(
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
  val imageSize = if (compact) 60.dp else 86.dp
  val metaStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = ImperialDisplayFont)
  val titleStyle =
    if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge
  val summaryStyle =
    if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
  MinistryFoldSurface(
    modifier =
      modifier
        .clickable(enabled = enabled && !isManagingMinistries) {
          folder.topic?.let { onTopicSelected(it.id) }
        }
        .testTag("topic-card-${folder.id}"),
    shape = ArchiveFlatCutShape,
    foldIntensity = if (compact) 0.16f else 0.2f,
    showEndFold = true,
  ) {
    Row(
      modifier =
        Modifier.fillMaxSize()
          .padding(
            start = 16.dp,
            top = 7.dp,
            end = 10.dp,
            bottom = 7.dp,
          ),
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
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = visual.description,
          style = summaryStyle.copy(fontFamily = ImperialDisplayFont),
          color = Color.Black.copy(alpha = 0.52f),
          fontWeight = FontWeight.Normal,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (!isManagingMinistries) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = "${folder.itemCount.toChineseCount()}篇",
              style = metaStyle,
              color = Color.Black.copy(alpha = 0.56f),
              maxLines = 1,
              softWrap = false,
            )
            Text(
              text = folder.updatedAtEpochMillis?.let(::friendlyTime) ?: "待启用",
              style = metaStyle,
              color = Color.Black.copy(alpha = 0.46f),
              maxLines = 1,
              softWrap = false,
            )
          }
          Image(
            painter = painterResource(id = visual.imageRes),
            contentDescription = null,
            modifier = Modifier.size(imageSize),
            contentScale = ContentScale.Fit,
            alpha = 0.84f,
          )
        }
      }
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
private fun MinistryFoldSurface(
  modifier: Modifier = Modifier,
  shape: androidx.compose.ui.graphics.Shape,
  foldIntensity: Float,
  showEndFold: Boolean = false,
  content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
  Box(modifier = modifier.clip(shape).background(Color.White, shape)) {
    Image(
      painter = painterResource(id = R.drawable.home_search_tile),
      contentDescription = null,
      modifier = Modifier.matchParentSize(),
      contentScale = ContentScale.Crop,
    )
    Box(modifier = Modifier.matchParentSize().background(Color.White.copy(alpha = 0.32f)))
    Box(
      modifier =
        Modifier.matchParentSize()
          .background(
            Brush.horizontalGradient(
              0f to Color.Transparent,
              0.7f to Color.Transparent,
              0.88f to Color.Black.copy(alpha = 0.045f * foldIntensity * 6f),
              0.95f to Color.White.copy(alpha = 0.34f * foldIntensity * 3f),
              1f to Color.Black.copy(alpha = 0.08f * foldIntensity * 5f),
            )
          )
    )
    Box(
      modifier =
        Modifier.align(Alignment.TopStart)
          .fillMaxWidth()
          .height(7.dp)
          .background(
            Brush.verticalGradient(
              0f to Color.Black.copy(alpha = 0.07f * foldIntensity * 4.5f),
              0.55f to Color.White.copy(alpha = 0.18f * foldIntensity * 3f),
              1f to Color.Transparent,
            )
          )
    )
    Box(
      modifier =
        Modifier.align(Alignment.BottomStart)
          .fillMaxWidth()
          .height(10.dp)
          .background(
            Brush.verticalGradient(
              0f to Color.Transparent,
              0.55f to Color.Black.copy(alpha = 0.05f * foldIntensity * 4f),
              1f to Color.Black.copy(alpha = 0.1f * foldIntensity * 4f),
            )
          )
    )
    if (showEndFold) {
      Box(
        modifier =
          Modifier.align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(12.dp)
            .background(
              Brush.horizontalGradient(
                0f to Color.Transparent,
                0.34f to Color.White.copy(alpha = 0.22f),
                1f to Color.Black.copy(alpha = 0.08f * foldIntensity * 5.5f),
              )
            )
      )
      Box(
        modifier =
          Modifier.align(Alignment.CenterStart)
            .fillMaxHeight()
            .width(1.dp)
            .background(Color.Black.copy(alpha = 0.04f))
      )
    }
    content()
  }
}

@Composable
private fun HomeOrnament(
  @DrawableRes imageRes: Int,
  modifier: Modifier = Modifier,
  alpha: Float = 0.5f,
  tint: Color? = null,
) {
  Image(
    painter = painterResource(id = imageRes),
    contentDescription = null,
    modifier = modifier,
    contentScale = ContentScale.Fit,
    alpha = alpha,
    colorFilter = tint?.let(ColorFilter::tint),
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
  mirrorBackground: Boolean = false,
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
      modifier =
        Modifier.matchParentSize().graphicsLayer(scaleX = if (mirrorBackground) -1f else 1f),
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
    diff < 3_600_000 -> "${(diff / 60_000).toInt().toChineseCount()}分钟前"
    diff < 86_400_000 -> "${(diff / 3_600_000).toInt().toChineseCount()}小时前"
    diff < 2_592_000_000L -> "${(diff / 86_400_000).toInt().toChineseCount()}天前"
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
