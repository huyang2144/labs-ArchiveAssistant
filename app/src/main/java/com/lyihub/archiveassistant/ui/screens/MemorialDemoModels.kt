package com.lyihub.archiveassistant.ui.screens

import android.graphics.Color as AndroidColor

internal enum class MemorialPageType {
  Cover,
  Directory,
  BodyLeft,
  BodyRight,
  End,
}

internal data class MemorialPage(
  val type: MemorialPageType,
  val pageNumber: String,
)

internal data class DirectoryItem(
  val title: String,
  val meta: String,
  val excerpt: String,
)

internal data class ArticleLayout(
  val page: MemorialPage,
  val pageIndex: Int,
  val left: Float,
  val top: Float,
  val width: Float,
  val height: Float,
)

internal data class FoldTransform(
  val rotationY: Float,
  val pivotX: Float,
  val shadingAlpha: Float,
  val edgeShadowProgress: Float,
  val visible: Boolean,
)

internal data class OpeningPlacement(
  val article: ArticleLayout,
  val left: Float,
  val pivotX: Float,
  val rotationY: Float,
  val foldAmount: Float,
)

internal enum class MemorialStage {
  CoverOnly,
  Opening,
  Expanded,
  Closing,
  Completed,
}

internal enum class MemorialStamp(val label: String) {
  Approve("准"),
  Reject("驳"),
  Keep("留中"),
  Like("朕喜欢"),
  Dislike("朕不喜欢"),
  Collapse("收起"),
}

internal enum class StampCompletion {
  ResetCover,
  AutoDismiss,
}

internal enum class CoverVerdictMotion {
  Gesture,
  Button,
}

internal val STAMP_RED: Int = AndroidColor.rgb(178, 37, 31)
internal val APP_XUAN_PAPER_BASE: Int = AndroidColor.rgb(252, 251, 246)
internal val STAMP_PAPER: Int = AndroidColor.rgb(247, 240, 219)
internal val IMPERIAL_GOLD: Int = AndroidColor.rgb(166, 126, 45)
internal val IMPERIAL_GOLD_DARK: Int = AndroidColor.rgb(104, 75, 26)
internal val MEMORIAL_INK_BROWN: Int = AndroidColor.rgb(78, 52, 31)

internal const val TOTAL_PENDING_MEMORIALS = 6
internal const val MEMORIAL_OPEN_CLOSE_DURATION_MS = 1_600L
internal const val COVER_VERDICT_DURATION_MS = 1_500L
internal const val SUMMARY_FADE_DURATION_MS = 150L

internal data class PendingMemorialSummary(
  val title: String,
  val source: String,
  val summary: String,
)

internal val pendingMemorialSummaries: List<PendingMemorialSummary> =
  listOf(
    PendingMemorialSummary(
      title = "江南水患赈灾折",
      source = "工部侍郎 张廷玉",
      summary = "江南诸府雨水连绵，田庐多毁，乞速拨银粮，以济流民。",
    ),
    PendingMemorialSummary(
      title = "西北边务军情折",
      source = "兵部尚书",
      summary = "边外游骑频现，关防需整，候旨调兵备饷。",
    ),
    PendingMemorialSummary(
      title = "漕运改海折",
      source = "漕运总督",
      summary = "运河淤阻，漕船迟滞，奏请酌改海运，以通南北。",
    ),
    PendingMemorialSummary(
      title = "盐课亏空清查折",
      source = "户部侍郎",
      summary = "两淮盐课亏空渐巨，请遣员清核账册，严惩侵蚀。",
    ),
    PendingMemorialSummary(
      title = "书院修缮请款折",
      source = "礼部尚书",
      summary = "各省书院屋宇倾圮，士子肄业受阻，请拨帑修葺。",
    ),
    PendingMemorialSummary(
      title = "河工岁修用银折",
      source = "河道总督",
      summary = "黄河岁修将届，料物工价俱涨，乞准预拨工银。",
    ),
  )

internal val memorialPages: List<MemorialPage> =
  listOf(
    MemorialPage(MemorialPageType.Cover, "壹"),
    MemorialPage(MemorialPageType.Directory, "贰"),
    MemorialPage(MemorialPageType.BodyLeft, "叁"),
    MemorialPage(MemorialPageType.BodyRight, "肆"),
    MemorialPage(MemorialPageType.End, "终"),
  )

internal val directoryItems: List<DirectoryItem> =
  listOf(
    DirectoryItem(
      title = "江南水患赈灾折",
      meta = "工部侍郎 · 三日前",
      excerpt = "臣谨奏：今岁江南秋雨连绵，淮水泛溢，下河州县多被淹没，灾民数十万，恳请拨银五十万两赈济...",
    ),
    DirectoryItem(
      title = "西北边务军情折",
      meta = "兵部尚书 · 五日前",
      excerpt = "臣叩首谨奏：准甘陕总督急报，准噶尔部异动频繁，嘉峪关外三十里发现游骑，请旨定夺...",
    ),
    DirectoryItem(
      title = "漕运改海折",
      meta = "漕运总督 · 七日前",
      excerpt = "臣稽首上言：运河年久淤塞，漕船阻滞，请酌量改行海运，以通南北之货，利国便民...",
    ),
  )
