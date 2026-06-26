package com.lyihub.archiveassistant.domain

object SampleKnowledgeData {
    const val DefaultTopicId = "topic-ai-architecture"

    val topics: List<Topic> = listOf(
        Topic(
            id = DefaultTopicId,
            title = "大模型架构研究",
            iconName = "folder-spark",
            iconColor = "#b85c38",
            updatedAtEpochMillis = 1_715_000_000_000,
        ),
        Topic(
            id = "topic-ui-inspiration",
            title = "UX/UI 灵感板",
            iconName = "folder-spark",
            iconColor = "#3898ec",
            updatedAtEpochMillis = 1_714_000_000_000,
        ),
        Topic(
            id = "topic-anthropology-clips",
            title = "阅读剪报：人类学",
            iconName = "folder-bookmark",
            iconColor = "#5e5d59",
            updatedAtEpochMillis = 1_713_000_000_000,
        ),
        Topic(
            id = "topic-hidden-travel",
            title = "冷门旅行地参考",
            iconName = "folder-spark",
            iconColor = "#3f3a34",
            updatedAtEpochMillis = 1_712_000_000_000,
        ),
        Topic(
            id = "topic-open-source-tools",
            title = "开源工具收藏",
            iconName = "folder-bookmark",
            iconColor = "#2d8a6e",
            updatedAtEpochMillis = 1_711_000_000_000,
        ),
    )

    val items: List<KnowledgeItem> = listOf(
        KnowledgeItem(
            id = "item-attention-pdf",
            topicId = DefaultTopicId,
            contentType = ContentType.DOCUMENT,
            documentFormat = DocumentFormat.PDF,
            title = "Attention 机制",
            summary = "Transformer 抛弃 RNN 结构后的核心注意力机制摘要。",
            fullText = "Attention 机制说明：Transformer 通过自注意力捕捉长距离依赖。",
            sourceUrl = null,
            createdAtEpochMillis = 1_715_086_400_000,
        ),
        KnowledgeItem(
            id = "item-transformer-article",
            topicId = DefaultTopicId,
            contentType = ContentType.WEB_ARTICLE,
            title = "Scaling Transformer 札记",
            summary = "关于大模型架构扩展规律的网页剪报。",
            fullText = "https://example.com/transformer-scaling article notes",
            sourceUrl = "https://example.com/transformer-scaling",
            createdAtEpochMillis = 1_715_000_000_000,
        ),
        KnowledgeItem(
            id = "item-transformer-diagram",
            topicId = DefaultTopicId,
            contentType = ContentType.IMAGE_SCREENSHOT,
            title = "Transformer 架构全景图",
            summary = "原始 Transformer 编码器-解码器结构的总览截屏。",
            fullText = "",
            sourceUrl = null,
            createdAtEpochMillis = 1_715_200_000_000,
        ),
        KnowledgeItem(
            id = "item-ui-screenshot",
            topicId = "topic-ui-inspiration",
            contentType = ContentType.IMAGE_SCREENSHOT,
            title = "设置页信息层级截图",
            summary = "用于后续卡片弹窗和详情筛选的界面参考。",
            fullText = "",
            sourceUrl = null,
            createdAtEpochMillis = 1_714_086_400_000,
        ),
        KnowledgeItem(
            id = "item-anthropology-note",
            topicId = "topic-anthropology-clips",
            contentType = ContentType.DOCUMENT,
            documentFormat = DocumentFormat.PDF,
            title = "田野笔记片段",
            summary = "阅读剪报中的短文本摘录。",
            fullText = "仪式、交换与地方知识之间的关系。",
            sourceUrl = null,
            createdAtEpochMillis = 1_713_086_400_000,
        ),
    )

    val defaultAiEngineSettings = AiEngineSettings(localModelId = GEMMA_4_E4B_IT.id)
}
