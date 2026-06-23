package com.lyihub.archiveassistant.domain

data class Topic(
    val id: String,
    val title: String,
    val iconName: String,
    val iconColor: String,
    val updatedAtEpochMillis: Long,
)

data class KnowledgeItem(
    val id: String,
    val topicId: String,
    val contentType: ContentType,
    val tag: String,
    val title: String,
    val summary: String,
    val fullText: String,
    val sourceUrl: String?,
    val documentFormat: DocumentFormat? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val createdAtEpochMillis: Long,
)

enum class ContentType(val label: String) {
    ALL("全部"),
    WEB_ARTICLE("网页"),
    IMAGE_SCREENSHOT("图像"),
    DOCUMENT("文档"),
}

enum class DocumentFormat(val label: String, val extension: String) {
    PDF("PDF", ".pdf"),
    MARKDOWN("Markdown", ".md"),
    TXT("纯文本", ".txt"),
    DOCX("Word", ".docx"),
    UNKNOWN("未知文档", ""),
}

enum class AiEngineType {
    OPENAI_COMPATIBLE,
    OPENAI_RESPONSES,
    ANTHROPIC,
    GEMINI,
    LOCAL_MODEL,
}

enum class LocalModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    INITIALIZING,
    READY,
    INFERENCING,
    ERROR,
    STOPPING,
}

enum class InferenceBackend {
    NPU,
    GPU,
    CPU,
    UNKNOWN,
}

data class LocalModelInfo(
    val id: String,
    val displayName: String,
    val fileName: String,
    val downloadUrl: String,
    val expectedSha256: String,
    val sizeBytes: Long,
)

data class LocalModelState(
    val status: LocalModelStatus = LocalModelStatus.NOT_DOWNLOADED,
    val downloadProgress: Float = 0f,
    val downloadBytes: Long = 0,
    val totalBytes: Long = 0,
    val activeBackend: InferenceBackend = InferenceBackend.UNKNOWN,
    val errorMessage: String? = null,
    val modelPath: String? = null,
)

data class BenchResult(
    val promptTokens: Int,
    val generateTokens: Int,
    val prefillTokensPerSecond: Float,
    val decodeTokensPerSecond: Float,
    val totalTimeMs: Long,
    val backend: InferenceBackend,
)

data class AiEngineSettings(
    val engineType: AiEngineType = AiEngineType.OPENAI_COMPATIBLE,
    val baseUrl: String = "https://api.example.com/v1",
    val modelName: String = "mock-knowledge-classifier",
    val apiKeyAlias: String = "default",
    val apiKey: String = "",
    @Deprecated("Replaced by in-process LiteRT-LM, kept for migration")
    val localEndpoint: String = "http://127.0.0.1:11434",
    val localModelId: String? = null,
    val localBackendPreference: InferenceBackend = InferenceBackend.NPU,
)

data class AiEnginePreset(
    val name: String,
    val engineType: AiEngineType = AiEngineType.OPENAI_COMPATIBLE,
    val baseUrl: String = "",
    val modelName: String = "",
    val apiKey: String = "",
    val localEndpoint: String = "",
)

enum class AppPane {
    TOPICS,
    DETAIL,
    SETTINGS,
    CLASSIFICATION_REVIEW,
    CARD_DETAIL,
    MANAGE,
}

data class ClassificationPayload(
    val topicId: String,
    val contentType: ContentType,
    val tag: String,
    val title: String,
    val summary: String,
    val rawInput: String,
    val documentFormat: DocumentFormat? = null,
)

sealed interface ClassificationResult {
    data class Classified(val payload: ClassificationPayload) : ClassificationResult

    data class BlankInput(val message: String = "请输入要归档的内容") : ClassificationResult

    data object Unknown : ClassificationResult
}
