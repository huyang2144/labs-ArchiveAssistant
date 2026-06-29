package com.lyihub.archiveassistant

import android.content.Context
import android.os.Bundle
import android.view.DragAndDropPermissions
import android.view.DragEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.datastore.preferences.preferencesDataStore
import com.lyihub.archiveassistant.app.ArchiveAssistantApp
import com.lyihub.archiveassistant.app.extractDragPayload
import com.lyihub.archiveassistant.app.isMimeAllowed
import com.lyihub.archiveassistant.data.AiEnginePresetRepository
import com.lyihub.archiveassistant.data.AiEngineSettingsRepository
import com.lyihub.archiveassistant.data.AppDataRepository
import com.lyihub.archiveassistant.data.OkHttpModelDownloadManager
import com.lyihub.archiveassistant.service.LocalInferenceConnection
import com.lyihub.archiveassistant.state.ArchiveAssistantStateStore
import com.lyihub.archiveassistant.ui.theme.ArchiveAssistantTheme

private val Context.aiEngineSettingsDataStore by preferencesDataStore(name = "ai_engine_settings")
private val Context.appDataStore by preferencesDataStore(name = "app_data")

class MainActivity : ComponentActivity() {
  private lateinit var stateStore: ArchiveAssistantStateStore
  private var dragDropPermissions: DragAndDropPermissions? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val aiSettingsRepository = AiEngineSettingsRepository(aiEngineSettingsDataStore)
    val inferenceConnection = LocalInferenceConnection(this)
    stateStore =
      ArchiveAssistantStateStore(
        appDataRepository = AppDataRepository(appDataStore),
        aiSettingsRepository = aiSettingsRepository,
        modelDownloadManager = OkHttpModelDownloadManager(this),
        inferenceConnection = inferenceConnection,
        androidContext = this,
      )
    window.decorView.setOnDragListener { _, event ->
      handleDragEvent(event)
    }
    setContent {
      val aiSettingsRepository = remember {
        aiSettingsRepository
      }
      val aiPresetRepository = remember {
        AiEnginePresetRepository(aiEngineSettingsDataStore)
      }
      val appDataRepository = remember {
        AppDataRepository(appDataStore)
      }
      ArchiveAssistantTheme {
        ArchiveAssistantApp(
          stateStore = stateStore,
          aiSettingsRepository = aiSettingsRepository,
          aiPresetRepository = aiPresetRepository,
          appDataRepository = appDataRepository,
        )
      }
    }
  }

  private fun handleDragEvent(event: DragEvent): Boolean {
    return when (event.action) {
      DragEvent.ACTION_DRAG_STARTED -> {
        val clipDescription = event.clipDescription ?: return false
        val mimeTypes =
          Array(clipDescription.mimeTypeCount) { index ->
            clipDescription.getMimeType(index)
          }
        isMimeAllowed(mimeTypes)
      }
      DragEvent.ACTION_DROP -> handleDrop(event)
      DragEvent.ACTION_DRAG_ENDED -> true
      else -> true
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    dragDropPermissions?.release()
    dragDropPermissions = null
    stateStore.releaseDragPermission = null
  }

  private fun handleDrop(event: DragEvent): Boolean {
    val state = stateStore.state
    if (state.addItemDialogVisible || state.editingItem != null || state.showClipboardDialog) {
      Toast.makeText(this, "请先关闭当前弹窗后再拖拽", Toast.LENGTH_SHORT).show()
      return false
    }

    dragDropPermissions = requestDragAndDropPermissions(event)

    val payload = extractDragPayload(this, event.clipData)
    if (payload == null) {
      Toast.makeText(this, "不支持的文件类型", Toast.LENGTH_SHORT).show()
      dragDropPermissions?.release()
      dragDropPermissions = null
      return false
    }

    stateStore.releaseDragPermission = {
      dragDropPermissions?.release()
      dragDropPermissions = null
      stateStore.releaseDragPermission = null
    }

    stateStore.showClipboard(
      content = payload.content ?: "",
      imageUri = payload.imageUri,
      sourceUri = payload.sourceUri,
      sourceContentType = payload.sourceContentType,
      sourceDocumentFormat = payload.sourceDocumentFormat,
      sourceFileName = payload.sourceFileName,
      sourceLabel = payload.sourceLabel,
    )

    if (payload.ignoredItemCount > 0) {
      Toast.makeText(this, "已处理第一个文件，忽略了 ${payload.ignoredItemCount} 个其他文件", Toast.LENGTH_SHORT)
        .show()
    }

    return true
  }
}
