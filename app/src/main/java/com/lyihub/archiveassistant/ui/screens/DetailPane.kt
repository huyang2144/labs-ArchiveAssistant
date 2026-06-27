package com.lyihub.archiveassistant.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import android.webkit.MimeTypeMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lyihub.archiveassistant.R
import androidx.core.content.FileProvider
import com.lyihub.archiveassistant.domain.ContentType
import com.lyihub.archiveassistant.domain.DocumentFormat
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.Topic
import com.lyihub.archiveassistant.state.AddItemDialogPrefill
import com.lyihub.archiveassistant.ui.components.ActionButton
import com.lyihub.archiveassistant.ui.components.PaneContainer
import com.lyihub.archiveassistant.ui.components.TextActionButton
import com.lyihub.archiveassistant.ui.theme.ImperialBronze
import com.lyihub.archiveassistant.ui.theme.ImperialCinnabar
import com.lyihub.archiveassistant.ui.theme.ImperialIvory
import com.lyihub.archiveassistant.ui.theme.ImperialLightGold
import com.lyihub.archiveassistant.ui.theme.ImperialParchment
import com.lyihub.archiveassistant.ui.theme.ImperialUmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lyihub.archiveassistant.data.copyUriToFile
import com.lyihub.archiveassistant.data.importFileName
import com.lyihub.archiveassistant.data.resolveDisplayName
import com.lyihub.archiveassistant.data.uniqueImportFile
import java.io.File

private val DetailTabTypes = listOf(
    ContentType.ALL,
    ContentType.WEB_ARTICLE,
    ContentType.IMAGE_SCREENSHOT,
    ContentType.DOCUMENT,
)

private val DetailPalaceGreen = ImperialIvory
private val DetailPalaceGreenMid = ImperialParchment
private val DetailPalaceGreenDark = ImperialUmber
private val DetailPalaceGold = ImperialUmber
private val DetailPalaceGoldBlock = ImperialLightGold
private val DetailPaper = ImperialIvory
private val DetailPaperDeep = ImperialParchment
private val DetailInk = ImperialUmber
private val DetailCinnabar = ImperialCinnabar
private val DetailLine = ImperialBronze

private const val FileProviderAuthoritySuffix = ".fileprovider"

private fun markdownFileName(title: String): String {
    val baseName = title
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .trim()
        .take(48)
        .replace(Regex("[\\\\/:*?\"<>|]+"), "-")
        .ifBlank { "clipboard-note" }
    return if (baseName.endsWith(".md", ignoreCase = true)) baseName else "$baseName.md"
}

private fun writeMarkdownPrefillFile(context: Context, title: String, content: String): File? {
    return try {
        val itemsDir = File(context.filesDir, "items").also { it.mkdirs() }
        val dest = uniqueImportFile(itemsDir, markdownFileName(title))
        dest.writeText(content)
        dest
    } catch (_: Exception) {
        null
    }
}

@Composable
fun DetailPane(
    topic: Topic,
    items: List<KnowledgeItem>,
    activeFilter: ContentType,
    searchQuery: String,
    onBack: () -> Unit,
    onFilterSelected: (ContentType) -> Unit,
    onItemClick: (String) -> Unit,
    onAddItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaneContainer(
        modifier = modifier
            .testTag("detail-pane")
            .background(DetailPalaceGreen),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(
                    Brush.verticalGradient(
                        listOf(DetailPalaceGreen, DetailPalaceGreenDark),
                    ),
                ),
        ) {
            val expanded = maxWidth >= 720.dp
            val horizontalPadding = if (expanded) 28.dp else 16.dp
            val maxContentWidth = if (expanded) 980.dp else 560.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    top = if (expanded) 22.dp else 16.dp,
                    end = horizontalPadding,
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(if (expanded) 16.dp else 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    DetailCourtHeader(
                        topic = topic,
                        itemCount = items.size,
                        activeFilter = activeFilter,
                        onBack = onBack,
                        onAddItemClick = onAddItemClick,
                        onFilterSelected = onFilterSelected,
                        modifier = Modifier.widthIn(max = maxContentWidth),
                    )
                }
                if (items.isEmpty()) {
                    item {
                        EmptyMemorialShelf(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = maxContentWidth),
                        )
                    }
                } else {
                    if (expanded) {
                        item {
                            ArticleMasonryGrid(
                                items = items,
                                searchQuery = searchQuery,
                                onItemClick = onItemClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = maxContentWidth),
                            )
                        }
                    } else {
                        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                            MemorialArticleCard(
                                item = item,
                                visual = articleVisual(index, hasArticleImage(item)),
                                searchQuery = searchQuery,
                                onClick = { onItemClick(item.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = maxContentWidth),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleMasonryGrid(
    items: List<KnowledgeItem>,
    searchQuery: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(2) { column ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEachIndexed { index, item ->
                    if (index % 2 == column) {
                        MemorialArticleCard(
                            item = item,
                            visual = articleVisual(index, hasArticleImage(item)),
                            searchQuery = searchQuery,
                            onClick = { onItemClick(item.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

private fun hasArticleImage(item: KnowledgeItem): Boolean {
    return item.contentType == ContentType.IMAGE_SCREENSHOT || item.contentType == ContentType.WEB_ARTICLE
}

@Composable
private fun DetailCourtHeader(
    topic: Topic,
    itemCount: Int,
    activeFilter: ContentType,
    onBack: () -> Unit,
    onAddItemClick: () -> Unit,
    onFilterSelected: (ContentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = DetailPalaceGold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailIconAction(
                    onClick = onAddItemClick,
                    testTag = "detail-add-item-button",
                    contentDescription = "新增资料",
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DetailPalaceGreenMid)
                .border(1.dp, DetailLine),
        ) {
            DecorativeDetailImage(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
                    .size(108.dp),
                alpha = 0.16f,
                tint = DetailPalaceGold,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "尚书档案",
                    style = MaterialTheme.typography.labelLarge,
                    color = DetailPalaceGold.copy(alpha = 0.78f),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = DetailPalaceGold,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "一篇一折 · 共 $itemCount 篇",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImperialUmber.copy(alpha = 0.72f),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("detail-tabs"),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            DetailTabTypes.forEach { type ->
                DetailFilterSeal(
                    label = type.label,
                    selected = activeFilter == type,
                    onClick = { onFilterSelected(type) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DetailIconAction(
    onClick: () -> Unit,
    testTag: String,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(DetailPalaceGoldBlock)
            .border(1.dp, DetailPalaceGold)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = contentDescription,
            tint = DetailPalaceGreenDark,
        )
    }
}

@Composable
private fun DetailFilterSeal(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) DetailPalaceGoldBlock else DetailPalaceGreenMid
    val content = if (selected) DetailPalaceGreenDark else DetailPalaceGold
    Box(
        modifier = modifier
            .height(42.dp)
            .background(background)
            .border(1.dp, DetailLine)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = content,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun MemorialArticleCard(
    item: KnowledgeItem,
    visual: ArticleVisual,
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showHighlight = searchQuery.isNotBlank()
    Column(
        modifier = modifier
            .background(DetailPaperDeep, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, DetailPalaceGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("knowledge-card-${item.id}"),
    ) {
        visual.imageRes?.let { imageRes ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ImperialIvory)
                    .padding(10.dp),
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(visual.aspectRatio),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DetailPaperDeep)
                .padding(
                    start = 16.dp,
                    top = if (visual.imageRes == null) 18.dp else 10.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
        ) {
            Image(
                painter = painterResource(id = R.drawable.memorial_xuan_paper),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.68f,
            )
            PaperVeil(modifier = Modifier.matchParentSize())
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${item.contentType.label} · ${friendlyTime(item.createdAtEpochMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DetailCinnabar,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = "尚书收",
                        style = MaterialTheme.typography.labelMedium,
                        color = DetailCinnabar.copy(alpha = 0.82f),
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    text = if (showHighlight) {
                        buildHighlightedText(
                            text = item.title,
                            query = searchQuery,
                            highlightColor = DetailCinnabar,
                            highlightBgColor = DetailCinnabar.copy(alpha = 0.16f),
                        )
                    } else {
                        androidx.compose.ui.text.AnnotatedString(item.title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = DetailInk,
                    fontWeight = FontWeight.Normal,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (showHighlight) {
                        buildHighlightedText(
                            text = item.summary.ifBlank { item.fullText },
                            query = searchQuery,
                            highlightColor = DetailCinnabar,
                            highlightBgColor = DetailCinnabar.copy(alpha = 0.16f),
                        )
                    } else {
                        androidx.compose.ui.text.AnnotatedString(item.summary.ifBlank { item.fullText })
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = DetailInk.copy(alpha = 0.74f),
                    maxLines = if (visual.imageRes == null) 6 else 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.fileName ?: item.sourceUrl?.take(28).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = DetailInk.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 32.dp)
                            .border(1.5.dp, DetailCinnabar.copy(alpha = 0.62f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "已藏",
                            style = MaterialTheme.typography.labelLarge,
                            color = DetailCinnabar.copy(alpha = 0.82f),
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMemorialShelf(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(DetailPaperDeep)
            .border(1.dp, DetailPalaceGold.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.memorial_xuan_paper),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.68f,
        )
        PaperVeil(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier.padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = DetailCinnabar.copy(alpha = 0.76f),
            )
            Text(
                text = "此档暂无奏折",
                style = MaterialTheme.typography.headlineSmall,
                color = DetailInk,
                fontWeight = FontWeight.Normal,
            )
            Text(
                text = "可由中书录入，门下筛选后归入尚书档案。",
                style = MaterialTheme.typography.bodyMedium,
                color = DetailInk.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun PaperVeil(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        ImperialIvory.copy(alpha = 0.16f),
                        DetailPaper.copy(alpha = 0.42f),
                        ImperialUmber.copy(alpha = 0.05f),
                    ),
                ),
            ),
    )
}

@Composable
private fun DecorativeCorner(
    modifier: Modifier = Modifier,
    rotation: Float,
) {
    Image(
        painter = painterResource(id = R.drawable.memorial_cover_corner),
        contentDescription = null,
        modifier = modifier
            .padding(8.dp)
            .size(30.dp)
            .graphicsLayer(rotationZ = rotation),
        contentScale = ContentScale.Fit,
        alpha = 0.58f,
        colorFilter = ColorFilter.tint(DetailCinnabar.copy(alpha = 0.76f)),
    )
}

@Composable
private fun DecorativeDetailImage(
    modifier: Modifier = Modifier,
    alpha: Float,
    tint: Color,
) {
    Image(
        painter = painterResource(id = R.drawable.imperial_ornament_pattern),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        alpha = alpha,
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, ContentType, String?, String, Boolean, DocumentFormat?, String?) -> Unit,
    validationMessage: String?,
    initialItem: KnowledgeItem? = null,
    prefill: AddItemDialogPrefill? = null,
    topics: List<Topic> = emptyList(),
    initialTopicId: String? = null,
) {
    val isEditMode = initialItem != null
    val effectivePrefill = prefill?.takeIf { initialItem == null }
    val lockContentType = effectivePrefill?.lockContentType == true
    val hideSourceFilePicker = effectivePrefill != null
    val availableContentTypes = effectivePrefill?.availableContentTypes
        ?: listOf(ContentType.WEB_ARTICLE, ContentType.IMAGE_SCREENSHOT, ContentType.DOCUMENT)
    var topicMenuExpanded by remember { mutableStateOf(false) }
    var selectedDialogTopicId by remember(initialItem, initialTopicId, topics) {
        mutableStateOf(
            when {
                initialItem != null -> initialItem.topicId
                initialTopicId != null && topics.any { it.id == initialTopicId } -> initialTopicId
                else -> topics.firstOrNull()?.id.orEmpty()
            }
        )
    }
    val selectedDialogTopic = topics.firstOrNull { it.id == selectedDialogTopicId }
    var title by remember(initialItem, effectivePrefill) { mutableStateOf(initialItem?.title ?: effectivePrefill?.title ?: "") }
    var selectedContentType by remember(initialItem, effectivePrefill) {
        mutableStateOf(initialItem?.contentType ?: effectivePrefill?.contentType ?: ContentType.WEB_ARTICLE)
    }
    var url by remember(initialItem, effectivePrefill) {
        mutableStateOf(
            when {
                initialItem?.contentType == ContentType.WEB_ARTICLE -> initialItem.sourceUrl ?: ""
                effectivePrefill?.contentType == ContentType.WEB_ARTICLE -> effectivePrefill.sourceUrl ?: ""
                else -> ""
            }
        )
    }
    var textContent by remember(effectivePrefill) { mutableStateOf(effectivePrefill?.textContent.orEmpty()) }
    var selectedFileUri by remember(initialItem, effectivePrefill) {
        mutableStateOf(
            when {
                initialItem != null && initialItem.contentType != ContentType.WEB_ARTICLE && initialItem.sourceUrl != null ->
                    Uri.parse(initialItem.sourceUrl)
                effectivePrefill != null && effectivePrefill.contentType != ContentType.WEB_ARTICLE && effectivePrefill.sourceUrl != null ->
                    Uri.parse(effectivePrefill.sourceUrl)
                else -> null
            }
        )
    }
    var summary by remember(initialItem) { mutableStateOf(initialItem?.summary ?: "") }
    var selectedDocumentFormat by remember(initialItem, effectivePrefill) {
        mutableStateOf(initialItem?.documentFormat ?: effectivePrefill?.documentFormat)
    }
    var selectedFileName by remember(initialItem, effectivePrefill) {
        mutableStateOf(initialItem?.fileName ?: effectivePrefill?.fileName)
    }
    var selectedImageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var selectedLocalFilePath by remember(initialItem) {
        mutableStateOf(
            if (initialItem != null && initialItem.contentType != ContentType.WEB_ARTICLE)
                initialItem.sourceUrl else null
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(selectedFileUri) {
        val uri = selectedFileUri
        if (uri != null && (uri.scheme == "content" || uri.scheme == "file")) {
            val file = withContext(Dispatchers.IO) {
                try {
                    val itemsDir = File(context.filesDir, "items").also { it.mkdirs() }
                    val ext = selectedFileName?.substringAfterLast('.', "")
                        ?.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""
                    val dest = uniqueImportFile(itemsDir, importFileName(selectedFileName, ext))
                    if (copyUriToFile(context, uri, dest)) dest else null
                } catch (_: Exception) {
                    null
                }
            }
            selectedLocalFilePath = file?.absolutePath
            selectedImageBitmap = if (file != null && selectedContentType == ContentType.IMAGE_SCREENSHOT) {
                withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                }
            } else {
                null
            }
        } else {
            selectedLocalFilePath = null
            selectedImageBitmap = null
        }
    }

    fun detectDocumentFormat(fileName: String?): DocumentFormat? {
        val ext = fileName?.substringAfterLast('.', "")?.lowercase() ?: return null
        return DocumentFormat.entries.firstOrNull { it.extension.equals(".$ext", ignoreCase = true) }
            ?.takeUnless { it == DocumentFormat.UNKNOWN }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        selectedFileName = uri?.let { resolveDisplayName(context, it) }
        if (selectedContentType == ContentType.DOCUMENT) {
            selectedDocumentFormat = detectDocumentFormat(selectedFileName)
        }
    }

    fun selectType(type: ContentType) {
        if (lockContentType && type != selectedContentType) return
        selectedContentType = type
        url = if (type == ContentType.WEB_ARTICLE) {
            url.ifBlank { effectivePrefill?.sourceUrl.orEmpty() }
        } else if (effectivePrefill?.sourceUrl != null) {
            url
        } else {
            ""
        }
        selectedFileUri = null
        selectedLocalFilePath = null
        selectedDocumentFormat = null
        selectedFileName = null
        if (
            type == ContentType.DOCUMENT &&
            (effectivePrefill?.title?.isNotBlank() == true || textContent.isNotBlank())
        ) {
            selectedDocumentFormat = DocumentFormat.MARKDOWN
            selectedFileName = markdownFileName(title)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .testTag("add-item-dialog")
                .widthIn(max = 600.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isEditMode) "修改资料" else "新增资料",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("add-item-dialog-close"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (!isEditMode) {
                    ExposedDropdownMenuBox(
                        expanded = topicMenuExpanded,
                        onExpandedChange = { topicMenuExpanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add-item-topic-selector"),
                    ) {
                        OutlinedTextField(
                            value = selectedDialogTopic?.title ?: "选择主题",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("归属主题") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = topicMenuExpanded,
                                )
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = topicMenuExpanded,
                            onDismissRequest = { topicMenuExpanded = false },
                        ) {
                            topics.forEach { topic ->
                                DropdownMenuItem(
                                    text = { Text(topic.title) },
                                    onClick = {
                                        selectedDialogTopicId = topic.id
                                        topicMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth().testTag("add-item-title"),
                    label = { Text("标题") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    modifier = Modifier.fillMaxWidth().testTag("add-item-summary"),
                    label = { Text("摘要（选填）") },
                    minLines = 3,
                    maxLines = 6,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )

                Text(
                    text = "类型",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    availableContentTypes.forEach { type ->
                        FilterChip(
                            label = type.label,
                            selected = selectedContentType == type,
                            enabled = !lockContentType,
                            onClick = { selectType(type) },
                        )
                    }
                }

                when (selectedContentType) {
                    ContentType.WEB_ARTICLE -> {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.fillMaxWidth().testTag("add-item-url"),
                            label = { Text("链接") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            ),
                        )
                    }

                    ContentType.IMAGE_SCREENSHOT -> {
                        selectedImageBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap,
                                contentDescription = "已选图片预览",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        if (!hideSourceFilePicker) {
                            ActionButton(
                                label = "选择图像文件",
                                onClick = { filePickerLauncher.launch("image/*") },
                                testTag = "add-item-pick-image",
                            )
                        }
                        selectedFileUri?.let { uri ->
                            Text(
                                text = "已选择: ${selectedFileName ?: resolveDisplayName(context, uri)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    ContentType.DOCUMENT -> {
                        if (!hideSourceFilePicker) {
                            ActionButton(
                                label = "选择文档",
                                onClick = { filePickerLauncher.launch("*/*") },
                                testTag = "add-item-pick-document",
                            )
                        }
                        selectedFileUri?.let { uri ->
                            Text(
                                text = "已选择: ${selectedFileName ?: resolveDisplayName(context, uri)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (effectivePrefill?.textContent != null) {
                            OutlinedTextField(
                                value = textContent,
                                onValueChange = { textContent = it },
                                modifier = Modifier.fillMaxWidth().testTag("add-item-text-content"),
                                label = { Text("文本内容") },
                                minLines = 3,
                                maxLines = 8,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                ),
                            )
                        }
                        if (selectedDocumentFormat != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "识别格式：",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(
                                        text = selectedDocumentFormat!!.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }

                    else -> {}
                }

                if (validationMessage != null) {
                    Text(
                        text = validationMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextActionButton(
                        label = "取消",
                        onClick = onDismiss,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ActionButton(
                        label = if (isEditMode) "保存" else "确认",
                        onClick = {
                            val textDocumentContent = textContent.takeIf { it.isNotBlank() } ?: effectivePrefill
                                ?.takeIf { it.sourceUrl == null && it.title.isNotBlank() }
                                ?.title
                            val textDocumentFile = if (
                                selectedContentType == ContentType.DOCUMENT &&
                                selectedFileUri == null &&
                                textDocumentContent != null
                            ) {
                                writeMarkdownPrefillFile(context, title, textDocumentContent)
                            } else {
                                null
                            }
                            val sourceUrl = when (selectedContentType) {
                                ContentType.WEB_ARTICLE -> url.takeIf { it.isNotBlank() }
                                else -> textDocumentFile?.absolutePath ?: selectedLocalFilePath
                            }
                            val docFormat = if (selectedContentType == ContentType.DOCUMENT) {
                                if (textDocumentFile != null) DocumentFormat.MARKDOWN else selectedDocumentFormat ?: DocumentFormat.UNKNOWN
                            } else null
                            val fileName = if (textDocumentFile != null) textDocumentFile.name else selectedFileName
                            val topicId = if (isEditMode) initialItem?.topicId.orEmpty() else selectedDialogTopicId
                            onConfirm(topicId, title, selectedContentType, sourceUrl, summary, false, docFormat, fileName)
                        },
                        testTag = "add-item-confirm",
                    )
                }
            }
        }
    }
}

@Composable
fun CardModal(
    item: KnowledgeItem,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .testTag("card-modal")
                .widthIn(max = 600.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.contentType.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("card-modal-close"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (item.contentType == ContentType.IMAGE_SCREENSHOT) {
                    if (item.sourceUrl != null) {
                        val path = item.sourceUrl!!
                        var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                        LaunchedEffect(path) {
                            bitmap = withContext(Dispatchers.IO) {
                                try {
                                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                                } catch (_: Exception) {
                                    null
                                }
                            }
                        }
                        bitmap?.let { bmp ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Image(
                                bitmap = bmp,
                                contentDescription = item.title,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (!item.sourceUrl.isNullOrBlank()) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Text(
                        text = item.sourceUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            openKnowledgeItemSource(context, item)
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (item.fullText.isNotBlank() && item.fullText != item.summary) {
                    Text(
                        text = item.fullText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextActionButton(
                        label = "删除",
                        onClick = onDelete,
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ActionButton(
                        label = "修改",
                        onClick = onEdit,
                    )
                }
            }
        }
    }
}

private fun openKnowledgeItemSource(
    context: android.content.Context,
    item: KnowledgeItem,
) {
    val rawSource = item.sourceUrl?.trim().takeUnless { it.isNullOrBlank() } ?: return
    if (item.contentType == ContentType.WEB_ARTICLE) {
        openWebSource(context, rawSource)
    } else {
        openLocalSource(context, item, rawSource)
    }
}

private fun openWebSource(
    context: android.content.Context,
    rawSource: String,
) {
    try {
        val url = if (rawSource.startsWith("http://") || rawSource.startsWith("https://")) {
            rawSource
        } else {
            "https://$rawSource"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("DetailPane", "Failed to open URL: $rawSource", e)
        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
    }
}

private fun openLocalSource(
    context: android.content.Context,
    item: KnowledgeItem,
    rawSource: String,
) {
    val file = File(rawSource)
    if (!file.exists()) {
        Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}$FileProviderAuthoritySuffix",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeTypeFor(item, file))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(intent, "打开文件")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        Log.e("DetailPane", "No app can open file: $rawSource", e)
        Toast.makeText(context, "无法打开文件", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("DetailPane", "Failed to open file: $rawSource", e)
        Toast.makeText(context, "无法打开文件", Toast.LENGTH_SHORT).show()
    }
}

private fun mimeTypeFor(
    item: KnowledgeItem,
    file: File,
): String {
    val fileName = item.fileName ?: file.name
    val extension = fileName.substringAfterLast('.', "")
        .lowercase()
        .takeIf { it.isNotBlank() }
    val mimeFromExtension = extension?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
    return mimeFromExtension ?: when (item.documentFormat) {
        DocumentFormat.PDF -> "application/pdf"
        DocumentFormat.MARKDOWN -> "text/markdown"
        DocumentFormat.TXT -> "text/plain"
        DocumentFormat.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        else -> if (item.contentType == ContentType.IMAGE_SCREENSHOT) "image/*" else "*/*"
    }
}

@Composable
fun ClipboardDialog(
    content: String,
    imageUri: String? = null,
    sourceLabel: String? = null,
    sourceContentType: ContentType? = null,
    sourceDocumentFormat: DocumentFormat? = null,
    sourceFileName: String? = null,
    onSummarize: () -> Unit,
    onManualCreate: () -> Unit,
    onDismiss: () -> Unit,
    isSmartSummarizing: Boolean = false,
    smartSummarizationMessage: String? = null,
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val hasContent = content.isNotBlank()

    LaunchedEffect(imageUri) {
        val uriStr = imageUri
        if (uriStr != null) {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(uriStr)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    null
                }
            }
            imageBitmap = bitmap
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (sourceLabel != null) "检测到${sourceLabel}内容" else "检测到剪切板内容") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                imageBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = "剪切板图片",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                if (hasContent) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!hasContent && imageBitmap == null && (sourceFileName != null || sourceDocumentFormat != null)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            sourceFileName?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            val typeLabel = when {
                                sourceDocumentFormat != null && sourceDocumentFormat != DocumentFormat.UNKNOWN ->
                                    "${sourceDocumentFormat.label} ${ContentType.DOCUMENT.label}"
                                sourceContentType != null -> sourceContentType.label
                                else -> null
                            }
                            typeLabel?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                if (smartSummarizationMessage != null) {
                    Text(
                        text = smartSummarizationMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("忽略")
                }
                TextButton(onClick = onManualCreate) {
                    Text("手动归纳")
                }
                TextButton(
                    onClick = onSummarize,
                    enabled = !isSmartSummarizing,
                ) {
                    Text(if (isSmartSummarizing) "归纳中…" else "智能归纳")
                }
            }
        },
    )
}

@Composable
fun DeleteItemConfirmDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("确定要删除资料 \"$itemTitle\" 吗？") },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onConfirm,
            ) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss,
            ) {
                Text("取消")
            }
        },
    )
}
