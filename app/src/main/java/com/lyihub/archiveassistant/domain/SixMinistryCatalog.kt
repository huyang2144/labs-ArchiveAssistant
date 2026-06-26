package com.lyihub.archiveassistant.domain

data class MinistryProfile(
    val topicId: String,
    val name: String,
    val domain: String,
    val responsibility: String,
    val colorHex: String,
    val updatedAtEpochMillis: Long,
    val keywords: List<String>,
) {
    val title: String = "$name · $domain"

    fun toTopic(): Topic = Topic(
        id = topicId,
        title = title,
        iconName = "folder-spark",
        iconColor = colorHex,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

object SixMinistryCatalog {
    const val LiTopicId = "topic-people-organizations"
    const val HuTopicId = "topic-hidden-travel"
    const val LiRitesTopicId = "topic-ui-inspiration"
    const val BingTopicId = "topic-open-source-tools"
    const val XingTopicId = "topic-anthropology-clips"
    const val GongTopicId = "topic-ai-architecture"

    val ministries: List<MinistryProfile> = listOf(
        MinistryProfile(
            topicId = LiTopicId,
            name = "吏部",
            domain = "人物组织",
            responsibility = "人物、机构、职业线索",
            colorHex = "#8B5A2B",
            updatedAtEpochMillis = 1_715_432_000_000,
            keywords = listOf("人物", "组织", "机构", "职业", "团队", "关系", "创作者", "person", "team", "career"),
        ),
        MinistryProfile(
            topicId = HuTopicId,
            name = "户部",
            domain = "商业经济",
            responsibility = "消费、商业、资产与市场",
            colorHex = "#A67C2D",
            updatedAtEpochMillis = 1_715_345_600_000,
            keywords = listOf("商业", "消费", "经济", "资产", "市场", "价格", "旅行", "travel", "market", "business"),
        ),
        MinistryProfile(
            topicId = LiRitesTopicId,
            name = "礼部",
            domain = "文化审美",
            responsibility = "教育、文化、设计与生活方式",
            colorHex = "#B66A45",
            updatedAtEpochMillis = 1_715_259_200_000,
            keywords = listOf("文化", "教育", "审美", "设计", "界面", "交互", "仪式", "ui", "ux", "design"),
        ),
        MinistryProfile(
            topicId = BingTopicId,
            name = "兵部",
            domain = "趋势战略",
            responsibility = "竞争、战略、风险与行业态势",
            colorHex = "#6F7663",
            updatedAtEpochMillis = 1_715_172_800_000,
            keywords = listOf("竞争", "战略", "风险", "趋势", "行业", "态势", "竞品", "strategy", "trend", "risk"),
        ),
        MinistryProfile(
            topicId = XingTopicId,
            name = "刑部",
            domain = "事实核验",
            responsibility = "争议、法规、事实与负面案例",
            colorHex = "#6B5F56",
            updatedAtEpochMillis = 1_715_086_400_000,
            keywords = listOf("事实", "核验", "法规", "争议", "谣言", "负面", "证伪", "法律", "law", "verify"),
        ),
        MinistryProfile(
            topicId = GongTopicId,
            name = "工部",
            domain = "技术产品",
            responsibility = "技术、工具、产品与工程实现",
            colorHex = "#2F7D72",
            updatedAtEpochMillis = 1_715_000_000_000,
            keywords = listOf("技术", "产品", "工具", "工程", "模型", "算法", "ai", "agent", "code", "tool"),
        ),
    )

    val topics: List<Topic> = ministries.map { it.toTopic() }
    val topicIds: Set<String> = ministries.mapTo(mutableSetOf()) { it.topicId }

    fun profileForTopicId(topicId: String): MinistryProfile? =
        ministries.firstOrNull { it.topicId == topicId }

    fun mergeInto(existingTopics: List<Topic>): List<Topic> {
        val existingById = existingTopics.associateBy { it.id }
        val ministryTopics = ministries.map { profile ->
            val existing = existingById[profile.topicId]
            profile.toTopic().copy(
                updatedAtEpochMillis = maxOf(
                    existing?.updatedAtEpochMillis ?: profile.updatedAtEpochMillis,
                    profile.updatedAtEpochMillis,
                ),
            )
        }
        val extras = existingTopics.filterNot { it.id in topicIds }
        return ministryTopics + extras
    }
}
