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
    val imageResName: String? = null,
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
    CLOUD_API,
    LOCAL_MODEL,
}

data class AiEngineSettings(
    val engineType: AiEngineType = AiEngineType.CLOUD_API,
    val baseUrl: String = "https://api.example.com/v1",
    val modelName: String = "mock-knowledge-classifier",
    val apiKeyAlias: String = "default",
    val localEndpoint: String = "http://127.0.0.1:11434",
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
}
