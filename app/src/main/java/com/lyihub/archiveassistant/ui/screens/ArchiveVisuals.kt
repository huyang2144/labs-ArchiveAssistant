package com.lyihub.archiveassistant.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.lyihub.archiveassistant.R
import com.lyihub.archiveassistant.ui.theme.ImperialBronze
import com.lyihub.archiveassistant.ui.theme.ImperialCinnabar
import com.lyihub.archiveassistant.ui.theme.ImperialIvory
import com.lyihub.archiveassistant.ui.theme.ImperialLightGold
import com.lyihub.archiveassistant.ui.theme.ImperialParchment
import com.lyihub.archiveassistant.ui.theme.ImperialUmber

internal data class MinistryVisual(
    val title: String,
    val duty: String,
    val description: String,
    @param:DrawableRes val imageRes: Int,
    val background: Color,
    val accent: Color,
)

internal data class ArticleVisual(
    @param:DrawableRes val imageRes: Int?,
    val aspectRatio: Float,
)

internal val MinistryVisuals = listOf(
    MinistryVisual(
        title = "吏部",
        duty = "人物与组织",
        description = "沉淀作者、机构、岗位与人物线索",
        imageRes = R.drawable.tsieina_department_li,
        background = ImperialParchment,
        accent = ImperialUmber,
    ),
    MinistryVisual(
        title = "户部",
        duty = "商业与资源",
        description = "收入、市场、预算与行业资料归档",
        imageRes = R.drawable.tsieina_department_hu,
        background = ImperialLightGold,
        accent = ImperialCinnabar,
    ),
    MinistryVisual(
        title = "礼部",
        duty = "文化与教育",
        description = "政策、审美、课程与公共表达",
        imageRes = R.drawable.tsieina_department_li2,
        background = ImperialIvory,
        accent = ImperialBronze,
    ),
    MinistryVisual(
        title = "兵部",
        duty = "技术与工具",
        description = "模型、工程、硬件与开源方案",
        imageRes = R.drawable.tsieina_department_bing,
        background = ImperialParchment,
        accent = ImperialCinnabar,
    ),
    MinistryVisual(
        title = "刑部",
        duty = "风险与判断",
        description = "待审、争议、合规与负面信号",
        imageRes = R.drawable.tsieina_department_xing,
        background = ImperialIvory,
        accent = ImperialUmber,
    ),
    MinistryVisual(
        title = "工部",
        duty = "产品与制作",
        description = "设计实现、视频素材与交互工程",
        imageRes = R.drawable.tsieina_department_gong,
        background = ImperialLightGold,
        accent = ImperialBronze,
    ),
)

internal val MemorialCoverResources = listOf(
    R.drawable.memorial_cover_pattern,
    R.drawable.memorial_cover_02,
    R.drawable.memorial_cover_03,
    R.drawable.memorial_cover_04,
    R.drawable.memorial_cover_05,
    R.drawable.memorial_cover_06,
    R.drawable.memorial_cover_07,
    R.drawable.memorial_cover_08,
    R.drawable.memorial_cover_09,
    R.drawable.memorial_cover_10,
    R.drawable.memorial_cover_11,
    R.drawable.memorial_cover_12,
    R.drawable.memorial_cover_13,
    R.drawable.memorial_cover_14,
    R.drawable.memorial_cover_15,
    R.drawable.memorial_cover_16,
    R.drawable.memorial_cover_17,
    R.drawable.memorial_cover_18,
    R.drawable.memorial_cover_19,
    R.drawable.memorial_cover_20,
    R.drawable.memorial_cover_21,
    R.drawable.memorial_cover_22,
    R.drawable.memorial_cover_23,
)

private val ArticleIllustrations = listOf(
    R.drawable.tsieina_article_01,
    R.drawable.tsieina_article_02,
    R.drawable.tsieina_article_03,
    R.drawable.tsieina_article_04,
    R.drawable.tsieina_article_05,
    R.drawable.tsieina_article_06,
)

internal fun ministryVisual(index: Int): MinistryVisual = MinistryVisuals[index % MinistryVisuals.size]

internal fun articleVisual(index: Int, hasImage: Boolean): ArticleVisual {
    if (!hasImage) return ArticleVisual(imageRes = null, aspectRatio = 1f)
    val image = ArticleIllustrations[index % ArticleIllustrations.size]
    val ratio = when (index % ArticleIllustrations.size) {
        1 -> 0.86f
        2 -> 1.04f
        3 -> 1f
        else -> 0.78f
    }
    return ArticleVisual(imageRes = image, aspectRatio = ratio)
}
