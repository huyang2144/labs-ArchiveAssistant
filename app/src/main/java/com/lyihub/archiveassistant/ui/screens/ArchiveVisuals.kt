package com.lyihub.archiveassistant.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.lyihub.archiveassistant.R
import com.lyihub.archiveassistant.ui.theme.ImperialBronze
import com.lyihub.archiveassistant.ui.theme.ImperialCinnabar
import com.lyihub.archiveassistant.ui.theme.ImperialIvory
import com.lyihub.archiveassistant.ui.theme.ImperialLightGold
import com.lyihub.archiveassistant.ui.theme.ImperialParchment
import com.lyihub.archiveassistant.ui.theme.ImperialUmber

internal data class FolderVisual(
  val description: String,
  @param:DrawableRes val imageRes: Int,
  val background: Color,
  val accent: Color,
)

internal data class ArchiveTileVisual(
  @param:DrawableRes val backgroundRes: Int,
  val borderColor: Color,
)

internal val ArchiveCutCornerShape: Shape = FixedCutCornerShape(10)

private class FixedCutCornerShape(private val notchDp: Int) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
  ): Outline {
    val notch = with(density) { notchDp.dp.toPx() }.coerceAtMost(size.minDimension * 0.28f)
    val path =
      Path().apply {
        moveTo(notch, 0f)
        lineTo(size.width - notch, 0f)
        quadraticTo(size.width - notch, notch, size.width, notch)
        lineTo(size.width, size.height - notch)
        quadraticTo(size.width - notch, size.height - notch, size.width - notch, size.height)
        lineTo(notch, size.height)
        quadraticTo(notch, size.height - notch, 0f, size.height - notch)
        lineTo(0f, notch)
        quadraticTo(notch, notch, notch, 0f)
        close()
      }
    return Outline.Generic(path)
  }
}

internal val ZhongshuTileVisual =
  ArchiveTileVisual(
    backgroundRes = R.drawable.home_zhongshu_tile,
    borderColor = Color(0xFF9C4A37),
  )

internal val MenxiaTileVisual =
  ArchiveTileVisual(
    backgroundRes = R.drawable.home_menxia_tile,
    borderColor = Color(0xFFDEC59E),
  )

internal val MemorialTileVisual =
  ArchiveTileVisual(
    backgroundRes = R.drawable.home_memorial_tile,
    borderColor = Color(0xFFAFD9BD),
  )

internal val ClipboardTileVisual =
  ArchiveTileVisual(
    backgroundRes = R.drawable.home_clipboard_tile,
    borderColor = Color(0xFF78ABCC),
  )

internal val SearchTileVisual =
  ArchiveTileVisual(
    backgroundRes = R.drawable.home_search_new_tile,
    borderColor = Color(0xFF3E3E46),
  )

internal val HomeTileVisuals =
  listOf(
    ZhongshuTileVisual,
    MenxiaTileVisual,
    MemorialTileVisual,
    ClipboardTileVisual,
    SearchTileVisual,
  )

internal fun homeTileVisual(index: Int): ArchiveTileVisual {
  return HomeTileVisuals[index.floorMod(HomeTileVisuals.size)]
}

internal val FolderVisuals =
  listOf(
    FolderVisual(
      description = "近期收藏与重点资料归档",
      imageRes = R.drawable.tsieina_department_li,
      background = ImperialParchment,
      accent = ImperialUmber,
    ),
    FolderVisual(
      description = "按主题收束同类资料",
      imageRes = R.drawable.tsieina_department_hu,
      background = ImperialLightGold,
      accent = ImperialCinnabar,
    ),
    FolderVisual(
      description = "保留可复查的摘录与来源",
      imageRes = R.drawable.tsieina_department_li2,
      background = ImperialIvory,
      accent = ImperialBronze,
    ),
    FolderVisual(
      description = "聚合技术、工具与实现线索",
      imageRes = R.drawable.tsieina_department_bing,
      background = ImperialParchment,
      accent = ImperialCinnabar,
    ),
    FolderVisual(
      description = "沉淀判断、风险与待复核内容",
      imageRes = R.drawable.tsieina_department_xing,
      background = ImperialIvory,
      accent = ImperialUmber,
    ),
    FolderVisual(
      description = "整理产品、设计与制作材料",
      imageRes = R.drawable.tsieina_department_gong,
      background = ImperialLightGold,
      accent = ImperialBronze,
    ),
  )

internal val MemorialCoverResources =
  listOf(
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

internal fun folderVisual(index: Int): FolderVisual = FolderVisuals[index % FolderVisuals.size]

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
