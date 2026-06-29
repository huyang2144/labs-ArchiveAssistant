package com.lyihub.archiveassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyihub.archiveassistant.domain.KnowledgeItem
import com.lyihub.archiveassistant.domain.Topic
import com.lyihub.archiveassistant.state.TopicNameDialogMode
import com.lyihub.archiveassistant.ui.components.ArchiveDialog
import com.lyihub.archiveassistant.ui.components.ArchiveDialogAction
import com.lyihub.archiveassistant.ui.components.PaneContainer
import com.lyihub.archiveassistant.ui.components.PaneDivider
import com.lyihub.archiveassistant.ui.components.PaneHeader

@Composable
fun ManagePane(
  topics: List<Topic>,
  itemsByTopic: Map<String, List<KnowledgeItem>>,
  onBack: () -> Unit,
  onTopicSelected: (String) -> Unit,
  onCreateTopic: () -> Unit,
  onRenameTopic: (String) -> Unit,
  onDeleteTopic: (String) -> Unit,
  onConfirmCreateTopic: (String) -> Unit,
  onConfirmRenameTopic: (String) -> Unit,
  onConfirmDeleteTopic: () -> Unit,
  onCloseTopicNameDialog: () -> Unit,
  onCloseDeleteConfirmDialog: () -> Unit,
  topicNameDialogMode: TopicNameDialogMode?,
  topicNameDialogTopicId: String?,
  topicValidationMessage: String?,
  deleteConfirmTopicId: String?,
  modifier: Modifier = Modifier,
) {
  PaneContainer(modifier = modifier.testTag("manage-pane")) {
    PaneHeader(
      title = "全部主题",
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
          onClick = onCreateTopic,
          modifier = Modifier.testTag("create-topic-button"),
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "新建主题",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
    )
    PaneDivider()
    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
      if (topics.isEmpty()) {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "暂无主题",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      items(topics, key = { it.id }) { topic ->
        ManageTopicRow(
          topic = topic,
          itemCount = itemsByTopic[topic.id]?.size ?: 0,
          onClick = { onTopicSelected(topic.id) },
          onRename = { onRenameTopic(topic.id) },
          onDelete = { onDeleteTopic(topic.id) },
        )
      }
    }
  }

  TopicManagementDialogs(
    topics = topics,
    topicNameDialogMode = topicNameDialogMode,
    topicNameDialogTopicId = topicNameDialogTopicId,
    topicValidationMessage = topicValidationMessage,
    deleteConfirmTopicId = deleteConfirmTopicId,
    onConfirmCreateTopic = onConfirmCreateTopic,
    onConfirmRenameTopic = onConfirmRenameTopic,
    onConfirmDeleteTopic = onConfirmDeleteTopic,
    onCloseTopicNameDialog = onCloseTopicNameDialog,
    onCloseDeleteConfirmDialog = onCloseDeleteConfirmDialog,
  )
}

@Composable
fun TopicManagementDialogs(
  topics: List<Topic>,
  topicNameDialogMode: TopicNameDialogMode?,
  topicNameDialogTopicId: String?,
  topicValidationMessage: String?,
  deleteConfirmTopicId: String?,
  onConfirmCreateTopic: (String) -> Unit,
  onConfirmRenameTopic: (String) -> Unit,
  onConfirmDeleteTopic: () -> Unit,
  onCloseTopicNameDialog: () -> Unit,
  onCloseDeleteConfirmDialog: () -> Unit,
) {
  val renamingTopic = topics.firstOrNull { it.id == topicNameDialogTopicId }
  if (topicNameDialogMode != null) {
    TopicNameDialog(
      mode = topicNameDialogMode,
      initialName = renamingTopic?.title ?: "",
      validationMessage = topicValidationMessage,
      onConfirm = { name ->
        when (topicNameDialogMode) {
          TopicNameDialogMode.CREATE -> onConfirmCreateTopic(name)
          TopicNameDialogMode.RENAME -> onConfirmRenameTopic(name)
        }
      },
      onDismiss = onCloseTopicNameDialog,
    )
  }

  if (deleteConfirmTopicId != null) {
    val deletingTopic = topics.firstOrNull { it.id == deleteConfirmTopicId }
    DeleteConfirmDialog(
      topicTitle = deletingTopic?.title ?: "",
      onConfirm = onConfirmDeleteTopic,
      onDismiss = onCloseDeleteConfirmDialog,
    )
  }
}

@Composable
private fun ManageTopicRow(
  topic: Topic,
  itemCount: Int,
  onClick: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      TopicIcon(title = topic.title, iconColor = topic.iconColor)
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
      IconButton(
        onClick = onRename,
        modifier = Modifier.testTag("rename-topic-button-${topic.id}"),
      ) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = "重命名",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(
        onClick = onDelete,
        modifier = Modifier.testTag("delete-topic-button-${topic.id}"),
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "删除",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@Composable
private fun TopicIcon(
  title: String,
  iconColor: String,
) {
  val firstChar = title.firstOrNull()?.toString() ?: "?"
  val color =
    try {
      Color(android.graphics.Color.parseColor(iconColor))
    } catch (_: Exception) {
      MaterialTheme.colorScheme.primary
    }
  Box(
    modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = firstChar,
      style = MaterialTheme.typography.titleMedium,
      color = color,
    )
  }
}

@Composable
private fun TopicNameDialog(
  mode: TopicNameDialogMode,
  initialName: String,
  validationMessage: String?,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var text by remember { mutableStateOf(initialName) }
  val title =
    when (mode) {
      TopicNameDialogMode.CREATE -> "建立新主题"
      TopicNameDialogMode.RENAME -> "重命名主题"
    }

  ArchiveDialog(
    title = title,
    onDismissRequest = onDismiss,
    testTag = "topic-name-dialog",
    actions = {
      ArchiveDialogAction(
        label = "取消",
        onClick = onDismiss,
        testTag = "topic-name-dialog-dismiss",
      )
      ArchiveDialogAction(
        label = "确定",
        onClick = { onConfirm(text) },
        primary = true,
        testTag = "topic-name-dialog-confirm",
      )
    },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("主题名称") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Text(
        text = "${text.length} / 20",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (validationMessage != null) {
        Text(
          text = validationMessage,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@Composable
private fun DeleteConfirmDialog(
  topicTitle: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  ArchiveDialog(
    title = "确认删除",
    onDismissRequest = onDismiss,
    testTag = "delete-confirm-dialog",
    actions = {
      ArchiveDialogAction(
        label = "取消",
        onClick = onDismiss,
        testTag = "delete-confirm-dialog-dismiss",
      )
      ArchiveDialogAction(
        label = "删除",
        onClick = onConfirm,
        destructive = true,
        testTag = "delete-confirm-dialog-confirm",
      )
    },
  ) {
    Text(
      text = "确定要删除主题 \"$topicTitle\" 吗？该主题下的所有内容也将被删除。",
      style = MaterialTheme.typography.bodyMedium,
      color = Color.Black,
    )
  }
}
