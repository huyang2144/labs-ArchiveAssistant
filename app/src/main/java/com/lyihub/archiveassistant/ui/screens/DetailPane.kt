package com.lyihub.archiveassistant.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val DetailTabTypes = listOf(
    ContentType.ALL,
    ContentType.WEB_ARTICLE,
    ContentType.IMAGE_SCREENSHOT,
    ContentType.DOCUMENT,
)

private const val FileProviderAuthoritySuffix = ".fileprovider"

private fun importFileName(displayName: String?, fallbackExtension: String): String {
    val sanitizedName = displayName
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.replace('\u0000', '_')
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    return sanitizedName ?: "imported-file$fallbackExtension"
}

private fun uniqueImportFile(itemsDir: File, fileName: String): File {
    val initialFile = File(itemsDir, fileName)
    if (!initialFile.exists()) return initialFile

    val dotIndex = fileName.lastIndexOf('.').takeIf { it > 0 }
    val baseName = dotIndex?.let { fileName.substring(0, it) } ?: fileName
    val extension = dotIndex?.let { fileName.substring(it) } ?: ""
    var suffix = 1

    while (true) {
        val candidate = File(itemsDir, "$baseName ($suffix)$extension")
        if (!candidate.exists()) return candidate
        suffix += 1
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
            if (item.contentType == ContentType.IMAGE_SCREENSHOT && item.sourceUrl != null) {
                val path = item.sourceUrl!!
                var thumbBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                LaunchedEffect(path) {
                    thumbBitmap = withContext(Dispatchers.IO) {
                        try {
                            BitmapFactory.decodeFile(path)?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
                thumbBitmap?.let { bmp ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = bmp,
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
    initialItem: KnowledgeItem? = null,
) {
    val isEditMode = initialItem != null
    var title by remember(initialItem) { mutableStateOf(initialItem?.title ?: "") }
    var selectedContentType by remember(initialItem) { mutableStateOf(initialItem?.contentType ?: ContentType.WEB_ARTICLE) }
    var url by remember(initialItem) {
        mutableStateOf(
            if (initialItem?.contentType == ContentType.WEB_ARTICLE) initialItem.sourceUrl ?: "" else ""
        )
    }
    var selectedFileUri by remember(initialItem) {
        mutableStateOf(
            if (initialItem != null && initialItem.contentType != ContentType.WEB_ARTICLE && initialItem.sourceUrl != null)
                Uri.parse(initialItem.sourceUrl) else null
        )
    }
    var summary by remember(initialItem) { mutableStateOf(initialItem?.summary ?: "") }
    var useAiSummary by remember(initialItem) { mutableStateOf(initialItem?.summary.isNullOrBlank() == true) }
    var selectedDocumentFormat by remember(initialItem) { mutableStateOf(initialItem?.documentFormat) }
    var selectedFileName by remember(initialItem) { mutableStateOf(initialItem?.fileName) }
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
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    dest
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

    fun displayNameFor(uri: Uri): String {
        val resolver = context.contentResolver
        if (uri.scheme == "content") {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        }
        return uri.lastPathSegment ?: uri.toString()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        selectedFileName = uri?.let(::displayNameFor)
        if (selectedContentType == ContentType.DOCUMENT) {
            selectedDocumentFormat = detectDocumentFormat(selectedFileName)
        }
    }

    fun selectType(type: ContentType) {
        selectedContentType = type
        url = ""
        selectedFileUri = null
        selectedLocalFilePath = null
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
                        ActionButton(
                            label = "选择图像文件",
                            onClick = { filePickerLauncher.launch("image/*") },
                            testTag = "add-item-pick-image",
                        )
                        selectedFileUri?.let { uri ->
                            Text(
                                text = "已选择: ${selectedFileName ?: displayNameFor(uri)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    ContentType.DOCUMENT -> {
                        ActionButton(
                            label = "选择文档",
                            onClick = { filePickerLauncher.launch("*/*") },
                            testTag = "add-item-pick-document",
                        )
                        selectedFileUri?.let { uri ->
                            Text(
                                text = "已选择: ${selectedFileName ?: displayNameFor(uri)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        label = if (isEditMode) "保存" else "确认",
                        onClick = {
                            val sourceUrl = when (selectedContentType) {
                                ContentType.WEB_ARTICLE -> url.takeIf { it.isNotBlank() }
                                else -> selectedLocalFilePath
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
    onSummarize: () -> Unit,
    onDismiss: () -> Unit,
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
        title = { Text("检测到剪切板内容") },
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
            }
        },
        confirmButton = {
            TextButton(onClick = onSummarize) {
                Text("智能归纳")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("忽略")
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
