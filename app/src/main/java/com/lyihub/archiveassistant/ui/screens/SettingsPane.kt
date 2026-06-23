package com.lyihub.archiveassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lyihub.archiveassistant.data.AiEndpointLatencyResult
import com.lyihub.archiveassistant.data.AiEnginePresetPreferences.presetFromCurrentSettings
import com.lyihub.archiveassistant.data.AiEnginePresetPreferences.toSettings
import com.lyihub.archiveassistant.data.testAiEndpointLatency
import com.lyihub.archiveassistant.domain.AiEnginePreset
import com.lyihub.archiveassistant.domain.AiEngineSettings
import com.lyihub.archiveassistant.domain.AiEngineType
import com.lyihub.archiveassistant.domain.BenchResult
import com.lyihub.archiveassistant.domain.GEMMA_4_E4B_IT
import com.lyihub.archiveassistant.domain.InferenceBackend
import com.lyihub.archiveassistant.domain.LocalModelState
import com.lyihub.archiveassistant.domain.LocalModelStatus
import com.lyihub.archiveassistant.ui.components.PaneContainer
import com.lyihub.archiveassistant.ui.components.PaneContentPadding
import com.lyihub.archiveassistant.ui.components.PaneDivider
import com.lyihub.archiveassistant.ui.components.PaneHeader
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPane(
    aiSettings: AiEngineSettings,
    onAiSettingsChanged: (AiEngineSettings) -> Unit,
    onBack: () -> Unit,
    presets: List<AiEnginePreset> = emptyList(),
    onPresetsChanged: (List<AiEnginePreset>) -> Unit = {},
    modifier: Modifier = Modifier,
    testLatency: suspend (AiEngineSettings, String) -> AiEndpointLatencyResult = ::testAiEndpointLatency,
    onDownloadModel: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onStartModel: () -> Unit = {},
    onStopModel: () -> Unit = {},
    onBackendPreferenceChange: (InferenceBackend) -> Unit = {},
    onRunBenchmark: () -> Unit = {},
    localModelState: LocalModelState = LocalModelState(LocalModelStatus.NOT_DOWNLOADED),
    benchmarkResult: BenchResult? = null,
    isBenchmarkRunning: Boolean = false,
) {
    var engineTypeExpanded by remember { mutableStateOf(false) }
    var presetExpanded by remember { mutableStateOf(false) }
    var savePresetDialogVisible by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var savePresetError by remember { mutableStateOf<String?>(null) }
    var latencyResultText by remember { mutableStateOf<String?>(null) }
    var isTestingLatency by remember { mutableStateOf(false) }
    var backendExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val selectedPresetName: String? = remember(presets, aiSettings) {
        presets.firstOrNull { preset ->
            preset.toSettings() == aiSettings
        }?.name
    }
    val selectedPresetLabel = selectedPresetName ?: "预设"

    PaneContainer(modifier = modifier.testTag("settings-pane")) {
        PaneHeader(
            title = "设置",
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
        )
        PaneDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            PaneContentPadding {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "AI 推理引擎配置",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        ExposedDropdownMenuBox(
                            expanded = presetExpanded,
                            onExpandedChange = { presetExpanded = it },
                            modifier = Modifier.width(160.dp),
                        ) {
                            OutlinedTextField(
                                value = selectedPresetLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("预设") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = presetExpanded,
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("preset-selector"),
                            )
                            ExposedDropdownMenu(
                                expanded = presetExpanded,
                                onDismissRequest = { presetExpanded = false },
                            ) {
                                if (presets.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("暂无预设") },
                                        onClick = { presetExpanded = false },
                                    )
                                } else {
                                        presets.forEach { preset ->
                                        DropdownMenuItem(
                                            text = { Text(preset.name) },
                                            onClick = {
                                                onAiSettingsChanged(preset.toSettings())
                                                presetExpanded = false
                                            },
                                        )
                                    }
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("保存当前为预设") },
                                    onClick = {
                                        newPresetName = ""
                                        savePresetError = null
                                        savePresetDialogVisible = true
                                        presetExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("删除当前预设") },
                                    enabled = selectedPresetName != null && presets.any { it.name == selectedPresetName },
                                    onClick = {
                                        val name = selectedPresetName ?: return@DropdownMenuItem
                                        onPresetsChanged(presets.filterNot { it.name == name })
                                        presetExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = engineTypeExpanded,
                        onExpandedChange = { engineTypeExpanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("engine-type-selector"),
                    ) {
                        OutlinedTextField(
                            value = when (aiSettings.engineType) {
                                AiEngineType.OPENAI_COMPATIBLE -> "OpenAI-Compatible"
                                AiEngineType.OPENAI_RESPONSES -> "OpenAI-Responses"
                                AiEngineType.ANTHROPIC -> "Anthropic"
                                AiEngineType.GEMINI -> "Gemini"
                                AiEngineType.LOCAL_MODEL -> "本地模型"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("引擎类型") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = engineTypeExpanded,
                                )
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = engineTypeExpanded,
                            onDismissRequest = { engineTypeExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("OpenAI-Compatible") },
                                onClick = {
                                    onAiSettingsChanged(
                                        aiSettings.copy(engineType = AiEngineType.OPENAI_COMPATIBLE),
                                    )
                                    engineTypeExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("OpenAI-Responses") },
                                onClick = {
                                    onAiSettingsChanged(
                                        aiSettings.copy(engineType = AiEngineType.OPENAI_RESPONSES),
                                    )
                                    engineTypeExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Anthropic") },
                                onClick = {
                                    onAiSettingsChanged(
                                        aiSettings.copy(engineType = AiEngineType.ANTHROPIC),
                                    )
                                    engineTypeExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Gemini") },
                                onClick = {
                                    onAiSettingsChanged(
                                        aiSettings.copy(engineType = AiEngineType.GEMINI),
                                    )
                                    engineTypeExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("本地模型") },
                                onClick = {
                                    onAiSettingsChanged(
                                        aiSettings.copy(engineType = AiEngineType.LOCAL_MODEL),
                                    )
                                    engineTypeExpanded = false
                                },
                            )
                        }
                    }

                    if (aiSettings.engineType != AiEngineType.LOCAL_MODEL) {
                        OutlinedTextField(
                            value = aiSettings.baseUrl,
                            onValueChange = {
                                onAiSettingsChanged(aiSettings.copy(baseUrl = it))
                            },
                            label = { Text("Base URL") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cloud-base-url-input"),
                        )
                        OutlinedTextField(
                            value = aiSettings.apiKey,
                            onValueChange = {
                                onAiSettingsChanged(aiSettings.copy(apiKey = it))
                            },
                            label = { Text("API 密钥") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api-key-input"),
                        )
                        OutlinedTextField(
                            value = aiSettings.modelName,
                            onValueChange = {
                                onAiSettingsChanged(aiSettings.copy(modelName = it))
                            },
                            label = { Text("模型名称") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cloud-model-input"),
                        )
                    }

                    if (aiSettings.engineType == AiEngineType.LOCAL_MODEL) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("local-model-panel"),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = GEMMA_4_E4B_IT.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "大小: %.2f GB".format(
                                            GEMMA_4_E4B_IT.sizeBytes / (1024f * 1024f * 1024f),
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "状态: ${when (localModelState.status) {
                                            LocalModelStatus.NOT_DOWNLOADED -> "未下载"
                                            LocalModelStatus.DOWNLOADING -> "下载中"
                                            LocalModelStatus.DOWNLOADED -> "已下载"
                                            LocalModelStatus.INITIALIZING -> "初始化中"
                                            LocalModelStatus.READY -> "就绪"
                                            LocalModelStatus.INFERENCING -> "推理中"
                                            LocalModelStatus.ERROR -> "错误"
                                            LocalModelStatus.STOPPING -> "停止中"
                                        }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("model-status-text"),
                                    )
                                }
                            }

                            when (localModelState.status) {
                                LocalModelStatus.NOT_DOWNLOADED, LocalModelStatus.ERROR -> {
                                    Button(
                                        onClick = onDownloadModel,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("download-model-button"),
                                    ) {
                                        Text("下载模型")
                                    }
                                }
                                LocalModelStatus.DOWNLOADING -> {
                                    val clampedProgress = localModelState.downloadProgress.coerceIn(0f, 1f)
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { clampedProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("download-progress-bar"),
                                        )
                                        Text(
                                            text = "${(clampedProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.testTag("download-progress-text"),
                                        )
                                        TextButton(
                                            onClick = onCancelDownload,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("cancel-download-button"),
                                        ) {
                                            Text("取消")
                                        }
                                    }
                                }
                                LocalModelStatus.DOWNLOADED -> {
                                    Button(
                                        onClick = onStartModel,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("start-model-button"),
                                    ) {
                                        Text("开启模型")
                                    }
                                }
                                LocalModelStatus.READY, LocalModelStatus.INFERENCING -> {
                                    Button(
                                        onClick = onStopModel,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("stop-model-button"),
                                    ) {
                                        Text("停止模型")
                                    }
                                }
                                else -> {}
                            }

                            ExposedDropdownMenuBox(
                                expanded = backendExpanded,
                                onExpandedChange = { backendExpanded = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("backend-preference-selector"),
                            ) {
                                OutlinedTextField(
                                    value = if (localModelState.activeBackend != InferenceBackend.UNKNOWN) {
                                        when (localModelState.activeBackend) {
                                            InferenceBackend.NPU -> "NPU"
                                            InferenceBackend.GPU -> "GPU"
                                            InferenceBackend.CPU -> "CPU"
                                            else -> "NPU"
                                        }
                                    } else {
                                        when (aiSettings.localBackendPreference) {
                                            InferenceBackend.NPU -> "NPU"
                                            InferenceBackend.GPU -> "GPU"
                                            InferenceBackend.CPU -> "CPU"
                                            else -> "NPU"
                                        }
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("推理后端") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = backendExpanded,
                                        )
                                    },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth(),
                                )
                                ExposedDropdownMenu(
                                    expanded = backendExpanded,
                                    onDismissRequest = { backendExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("NPU") },
                                        onClick = {
                                            onBackendPreferenceChange(InferenceBackend.NPU)
                                            backendExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("GPU") },
                                        onClick = {
                                            onBackendPreferenceChange(InferenceBackend.GPU)
                                            backendExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("CPU") },
                                        onClick = {
                                            onBackendPreferenceChange(InferenceBackend.CPU)
                                            backendExpanded = false
                                        },
                                    )
                                }
                            }

                            if (localModelState.status == LocalModelStatus.READY || localModelState.status == LocalModelStatus.INFERENCING) {
                                Button(
                                    onClick = onRunBenchmark,
                                    enabled = !isBenchmarkRunning,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("benchmark-button"),
                                ) {
                                    if (isBenchmarkRunning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .testTag("benchmark-loading-indicator"),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                    Text("测试推理速度")
                                }

                                benchmarkResult?.let { result ->
                                    Text(
                                        text = "Prefill: %.1f tk/s | Decode: %.1f tk/s | 总耗时: %dms | 后端: %s".format(
                                            result.prefillTokensPerSecond,
                                            result.decodeTokensPerSecond,
                                            result.totalTimeMs,
                                            when (result.backend) {
                                                InferenceBackend.NPU -> "NPU"
                                                InferenceBackend.GPU -> "GPU"
                                                InferenceBackend.CPU -> "CPU"
                                                else -> "UNKNOWN"
                                            },
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("benchmark-result-text"),
                                    )
                                }
                            }

                            if (localModelState.status == LocalModelStatus.ERROR) {
                                localModelState.errorMessage?.let { msg ->
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("model-error-text"),
                                    )
                                }
                                Button(
                                    onClick = onDownloadModel,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("retry-button"),
                                ) {
                                    Text("重试")
                                }
                            }

                        }
                    }

                    Button(
                        onClick = {
                            latencyResultText = null
                            isTestingLatency = true
                            coroutineScope.launch {
                                val result = testLatency(aiSettings, aiSettings.apiKey)
                                latencyResultText = when (result) {
                                    is AiEndpointLatencyResult.Success -> "延迟：${result.elapsedMillis} ms"
                                    is AiEndpointLatencyResult.Failure -> "测试失败：${result.message}"
                                }
                                isTestingLatency = false
                            }
                        },
                        enabled = !isTestingLatency,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test-api-latency-button"),
                    ) {
                        if (isTestingLatency) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        }
                        Text("测试 API 延迟")
                    }

                    latencyResultText?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                text.startsWith("延迟：") -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api-latency-result"),
                        )
                    }
                }
            }
        }
    }

    if (savePresetDialogVisible) {
        AlertDialog(
            onDismissRequest = { savePresetDialogVisible = false },
            title = { Text("保存预设") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = {
                            newPresetName = it
                            savePresetError = null
                        },
                        label = { Text("预设名称") },
                        singleLine = true,
                        isError = savePresetError != null,
                        supportingText = savePresetError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newPresetName.trim()
                        when {
                            name.isBlank() -> savePresetError = "请输入预设名称"
                            else -> {
                                val newPreset = presetFromCurrentSettings(
                                    name = name,
                                    settings = aiSettings,
                                    rawApiKey = aiSettings.apiKey,
                                )
                                val updated = presets.filterNot { it.name == name } + newPreset
                                onPresetsChanged(updated)
                                savePresetDialogVisible = false
                            }
                        }
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { savePresetDialogVisible = false },
                ) {
                    Text("取消")
                }
            },
        )
    }
}
