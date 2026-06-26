package com.lyihub.archiveassistant.domain

object SampleKnowledgeData {
    const val DefaultTopicId = SixMinistryCatalog.GongTopicId

    val topics: List<Topic> = SixMinistryCatalog.topics

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
            topicId = SixMinistryCatalog.LiRitesTopicId,
            contentType = ContentType.IMAGE_SCREENSHOT,
            title = "奏折阅读排版参考",
            summary = "用于折叠屏双页阅读、批阅按钮和信息层级的界面参考。",
            fullText = "",
            sourceUrl = null,
            createdAtEpochMillis = 1_714_086_400_000,
        ),
        KnowledgeItem(
            id = "item-anthropology-note",
            topicId = SixMinistryCatalog.XingTopicId,
            contentType = ContentType.DOCUMENT,
            documentFormat = DocumentFormat.PDF,
            title = "事实核验剪报",
            summary = "用于门下省复核的争议事实、来源证据与待确认线索。",
            fullText = "争议事实、来源证据与待确认线索之间的关系。",
            sourceUrl = null,
            createdAtEpochMillis = 1_713_086_400_000,
        ),
        KnowledgeItem(
            id = "item-market-travel",
            topicId = SixMinistryCatalog.HuTopicId,
            contentType = ContentType.WEB_ARTICLE,
            title = "折叠屏消费趋势观察",
            summary = "记录折叠屏用户在旅行、消费和高密度信息处理场景中的行为变化。",
            fullText = "https://example.com/foldable-consumption-trend",
            sourceUrl = "https://example.com/foldable-consumption-trend",
            createdAtEpochMillis = 1_714_604_800_000,
        ),
        KnowledgeItem(
            id = "item-strategy-brief",
            topicId = SixMinistryCatalog.BingTopicId,
            contentType = ContentType.DOCUMENT,
            documentFormat = DocumentFormat.MARKDOWN,
            title = "AI 信息流竞争态势",
            summary = "比较传统推荐算法与个人知识库驱动推荐的策略差异。",
            fullText = "传统推荐算法依赖平台大数据，个人知识库推荐依赖用户主动沉淀的信息主权。",
            sourceUrl = null,
            createdAtEpochMillis = 1_714_777_600_000,
        ),
    )

    val defaultAiEngineSettings = AiEngineSettings(localModelId = GEMMA_4_E4B_IT.id)
}
