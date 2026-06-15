package com.lyihub.archiveassistant.ui.screens

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lyihub.archiveassistant.domain.ContentType
import com.lyihub.archiveassistant.domain.DocumentFormat
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.Topic
import com.lyihub.archiveassistant.ui.components.ActionButton
import com.lyihub.archiveassistant.ui.components.PaneContainer
import com.lyihub.archiveassistant.ui.components.PaneContentPadding
import com.lyihub.archiveassistant.ui.components.PaneDivider
import com.lyihub.archiveassistant.ui.components.PaneHeader
import com.lyihub.archiveassistant.ui.components.TextActionButton

private val DetailTabTypes = listOf(
    ContentType.ALL,
    ContentType.WEB_ARTICLE,
    ContentType.IMAGE_SCREENSHOT,
    ContentType.DOCUMENT,
)

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
    PaneContainer(modifier = modifier.testTag("detail-pane")) {
        PaneHeader(
            title = topic.title,
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = onAddItemClick,
                    modifier = Modifier.testTag("detail-add-item-button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新增资料",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        PaneDivider()
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            item {
                PaneContentPadding {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "筛选",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.testTag("detail-tabs"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DetailTabTypes.forEach { type ->
                                FilterChip(
                                    label = type.label,
                                    selected = activeFilter == type,
                                    onClick = { onFilterSelected(type) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "共 ${items.size} 条资料",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "该分类下暂无资料",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(items, key = { it.id }) { item ->
                    KnowledgeItemRow(
                        item = item,
                        searchQuery = searchQuery,
                        onClick = { onItemClick(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
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
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun KnowledgeItemRow(
    item: KnowledgeItem,
    searchQuery: String,
    onClick: () -> Unit,
) {
    val showHighlight = searchQuery.isNotBlank()
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("knowledge-card-${item.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (showHighlight) {
                    buildHighlightedText(
                        text = item.title,
                        query = searchQuery,
                        highlightColor = MaterialTheme.colorScheme.primary,
                        highlightBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    )
                } else {
                    androidx.compose.ui.text.AnnotatedString(item.title)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (showHighlight) {
                    buildHighlightedText(
                        text = item.summary,
                        query = searchQuery,
                        highlightColor = MaterialTheme.colorScheme.primary,
                        highlightBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    )
                } else {
                    androidx.compose.ui.text.AnnotatedString(item.summary)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.contentType == ContentType.IMAGE_SCREENSHOT && item.imageResName != null) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val resId = context.resources.getIdentifier(
                    item.imageResName, "drawable", context.packageName
                )
                if (resId != 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        painter = painterResource(resId),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(32f / 9f)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, ContentType, String?, String, Boolean, DocumentFormat?, String?) -> Unit,
    validationMessage: String?,
) {
    var title by remember { mutableStateOf("") }
    var selectedContentType by remember { mutableStateOf(ContentType.WEB_ARTICLE) }
    var url by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var summary by remember { mutableStateOf("") }
    var useAiSummary by remember { mutableStateOf(false) }
    var selectedDocumentFormat by remember { mutableStateOf<DocumentFormat?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        selectedFileName = uri?.lastPathSegment
    }

    fun selectType(type: ContentType) {
        selectedContentType = type
        url = ""
        selectedFileUri = null
        selectedDocumentFormat = null
        selectedFileName = null
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
                        text = "新增资料",
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

                Text(
                    text = "类型",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(ContentType.WEB_ARTICLE, ContentType.IMAGE_SCREENSHOT, ContentType.DOCUMENT).forEach { type ->
                        FilterChip(
                            label = type.label,
                            selected = selectedContentType == type,
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
                        ActionButton(
                            label = "选择图像文件",
                            onClick = { filePickerLauncher.launch("image/*") },
                            testTag = "add-item-pick-image",
                        )
                        selectedFileUri?.let { uri ->
                            Text(
                                text = "已选择: ${uri.lastPathSegment ?: uri.toString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    ContentType.DOCUMENT -> {
                        Text(
                            text = "文档格式",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(DocumentFormat.PDF, DocumentFormat.MARKDOWN, DocumentFormat.TXT, DocumentFormat.DOCX).forEach { format ->
                                FilterChip(
                                    label = format.label,
                                    selected = selectedDocumentFormat == format,
                                    onClick = { selectedDocumentFormat = format },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        ActionButton(
                            label = "选择文档",
                            onClick = { filePickerLauncher.launch("*/*") },
                            testTag = "add-item-pick-document",
                        )
                        selectedFileUri?.let { uri ->
                            Text(
                                text = "已选择: ${uri.lastPathSegment ?: uri.toString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    else -> {}
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useAiSummary = !useAiSummary },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = useAiSummary,
                        onCheckedChange = { useAiSummary = it },
                    )
                    Text(
                        text = "AI总结",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (!useAiSummary) {
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
                        label = "确认",
                        onClick = {
                            val sourceUrl = when (selectedContentType) {
                                ContentType.WEB_ARTICLE -> url.takeIf { it.isNotBlank() }
                                else -> selectedFileUri?.toString()
                            }
                            val docFormat = if (selectedContentType == ContentType.DOCUMENT) {
                                selectedDocumentFormat ?: DocumentFormat.UNKNOWN
                            } else null
                            onConfirm(title, selectedContentType, sourceUrl, summary, useAiSummary, docFormat, selectedFileName)
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
                        text = item.tag,
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
                if (item.contentType == ContentType.IMAGE_SCREENSHOT && item.imageResName != null) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val resId = context.resources.getIdentifier(
                        item.imageResName, "drawable", context.packageName
                    )
                    if (resId != 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Image(
                            painter = painterResource(resId),
                            contentDescription = item.title,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                        )
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
                            try {
                                val rawUrl = item.sourceUrl ?: return@clickable
                                val url = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                                    rawUrl
                                } else {
                                    "https://$rawUrl"
                                }
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("DetailPane", "Failed to open URL: ${item.sourceUrl}", e)
                                Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                            }
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
            }
        }
    }
}
