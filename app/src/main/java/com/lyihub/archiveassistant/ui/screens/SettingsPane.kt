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
import com.lyihub.archiveassistant.ui.components.PaneContainer
import com.lyihub.archiveassistant.ui.components.PaneContentPadding
import com.lyihub.archiveassistant.ui.components.PaneDivider
import com.lyihub.archiveassistant.ui.components.PaneHeader
import kotlinx.coroutines.launch

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
) {
    var engineTypeExpanded by remember { mutableStateOf(false) }
    var presetExpanded by remember { mutableStateOf(false) }
    var savePresetDialogVisible by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var savePresetError by remember { mutableStateOf<String?>(null) }
    var latencyResultText by remember { mutableStateOf<String?>(null) }
    var isTestingLatency by remember { mutableStateOf(false) }
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
                        OutlinedTextField(
                            value = aiSettings.localEndpoint,
                            onValueChange = {
                                onAiSettingsChanged(aiSettings.copy(localEndpoint = it))
                            },
                            label = { Text("本地服务地址") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("local-endpoint-input"),
                        )
                        OutlinedTextField(
                            value = aiSettings.modelName,
                            onValueChange = {
                                onAiSettingsChanged(aiSettings.copy(modelName = it))
                            },
                            label = { Text("本地模型") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("local-model-input"),
                        )
                        Text(
                            text = "本地模型将调用系统内置算力推演摘要。速度受限于设备硬件。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
