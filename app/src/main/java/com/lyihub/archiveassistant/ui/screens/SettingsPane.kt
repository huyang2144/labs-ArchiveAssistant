package com.lyihub.archiveassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lyihub.archiveassistant.domain.AiEngineSettings
import com.lyihub.archiveassistant.domain.AiEngineType
import com.lyihub.archiveassistant.ui.components.PaneContainer
import com.lyihub.archiveassistant.ui.components.PaneContentPadding
import com.lyihub.archiveassistant.ui.components.PaneDivider
import com.lyihub.archiveassistant.ui.components.PaneHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPane(
    aiSettings: AiEngineSettings,
    onAiSettingsChanged: (AiEngineSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rawApiKey by remember { mutableStateOf("") }
    var engineTypeExpanded by remember { mutableStateOf(false) }

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
                    Text(
                        text = "AI 推理引擎配置",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    ExposedDropdownMenuBox(
                        expanded = engineTypeExpanded,
                        onExpandedChange = { engineTypeExpanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("engine-type-selector"),
                    ) {
                        OutlinedTextField(
                            value = when (aiSettings.engineType) {
                                AiEngineType.CLOUD_API -> "API"
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
                                text = { Text("API") },
                                onClick = {
                                    onAiSettingsChanged(
                                        aiSettings.copy(engineType = AiEngineType.CLOUD_API),
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

                    if (aiSettings.engineType == AiEngineType.CLOUD_API) {
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
                            value = rawApiKey,
                            onValueChange = { rawApiKey = it },
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
                }
            }
        }
    }
}
