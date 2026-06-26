package com.lyihub.archiveassistant.ui.screens

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.InputDevice
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.widget.OverScroller
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.lyihub.archiveassistant.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class MemorialPageType {
    Cover,
    Directory,
    BodyLeft,
    BodyRight,
    End,
}

private data class MemorialPage(
    val type: MemorialPageType,
    val pageNumber: String,
)

private data class DirectoryItem(
    val title: String,
    val meta: String,
    val excerpt: String,
)

private data class ArticleLayout(
    val page: MemorialPage,
    val pageIndex: Int,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

private data class FoldTransform(
    val rotationY: Float,
    val pivotX: Float,
    val shadingAlpha: Float,
    val edgeShadowProgress: Float,
    val visible: Boolean,
)

private data class OpeningPlacement(
    val article: ArticleLayout,
    val left: Float,
    val pivotX: Float,
    val rotationY: Float,
    val foldAmount: Float,
)

private enum class MemorialStage {
    CoverOnly,
    Opening,
    Expanded,
    Closing,
    Completed,
}

private enum class MemorialStamp(val label: String) {
    Approve("准"),
    Reject("驳"),
    Keep("留中"),
    Like("朕喜欢"),
    Dislike("朕不喜欢"),
    Collapse("收起"),
}

private enum class StampCompletion {
    ResetCover,
    AutoDismiss,
}

private val STAMP_RED = AndroidColor.rgb(178, 37, 31)
private val STAMP_PAPER = AndroidColor.rgb(247, 240, 219)
private val IMPERIAL_GOLD = AndroidColor.rgb(166, 126, 45)
private val IMPERIAL_GOLD_DARK = AndroidColor.rgb(104, 75, 26)
private val MEMORIAL_INK_BROWN = AndroidColor.rgb(78, 52, 31)
private const val TOTAL_PENDING_MEMORIALS = 6
private const val MEMORIAL_OPEN_CLOSE_DURATION_MS = 1_600L
private const val COVER_VERDICT_DURATION_MS = 1_500L
private const val COVER_SWITCH_DURATION_MS = 360L
private const val SUMMARY_FADE_DURATION_MS = 150L

private data class PendingMemorialSummary(
    val title: String,
    val source: String,
    val summary: String,
)

private val pendingMemorialSummaries = listOf(
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

private val memorialPages = listOf(
    MemorialPage(MemorialPageType.Cover, "壹"),
    MemorialPage(MemorialPageType.Directory, "贰"),
    MemorialPage(MemorialPageType.BodyLeft, "叁"),
    MemorialPage(MemorialPageType.BodyRight, "肆"),
    MemorialPage(MemorialPageType.End, "终"),
)

private val directoryItems = listOf(
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

@Composable
fun MemorialDemoOverlay(onDismiss: () -> Unit) {
    val foldView = remember { mutableStateOf<FoldScrollNativeView?>(null) }
    val dismissStarted = remember { mutableStateOf(false) }
    val hostView = LocalView.current
    val completeDismiss = {
        onDismiss()
    }
    val autoDismiss = {
        if (!dismissStarted.value) {
            dismissStarted.value = true
            onDismiss()
        }
    }
    val requestDismiss = {
        if (!dismissStarted.value) {
            dismissStarted.value = true
            foldView.value?.closeWithAnimation(completeDismiss) ?: completeDismiss()
        }
    }

    BackHandler(onBack = requestDismiss)
    DisposableEffect(hostView) {
        val window = (hostView.context as? android.app.Activity)?.window
        val previousSystemUiVisibility = window?.decorView?.systemUiVisibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window?.decorView?.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window?.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
            if (previousSystemUiVisibility != null) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = previousSystemUiVisibility
            }
            foldView.value = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F2))
            .testTag("memorial-demo-overlay"),
    ) {
        AndroidView(
            factory = { context ->
                FoldScrollNativeView(context).apply {
                    foldView.value = this
                    setAutoDismissHandler(autoDismiss)
                    setPages(memorialPages)
                }
            },
            update = { view ->
                foldView.value = view
                view.setAutoDismissHandler(autoDismiss)
                view.setPages(memorialPages)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private class FoldScrollNativeView(context: Context) : View(context) {
    private val displayDensity = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity
    private val scroller = OverScroller(context)
    private val camera = Camera()
    private val matrix = Matrix()
    private val articleRect = RectF()
    private val config = ViewConfiguration.get(context)
    private val touchSlop = config.scaledTouchSlop
    private val minimumVelocity = config.scaledMinimumFlingVelocity
    private val maximumVelocity = config.scaledMaximumFlingVelocity

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = AndroidColor.rgb(242, 242, 242)
    }
    private val articlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.WHITE
    }
    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.BLACK
    }
    private val foldShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paperAgingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val paperTexture: Bitmap? = BitmapFactory.decodeResource(
        resources,
        R.drawable.memorial_xuan_paper,
    )
    private val fallbackCoverTexture: Bitmap? = BitmapFactory.decodeResource(
        resources,
        R.drawable.memorial_cover_pattern,
    )
    private val coverTextures: List<Bitmap?> = listOf(
        BitmapFactory.decodeResource(resources, R.drawable.memorial_cover_01),
        BitmapFactory.decodeResource(resources, R.drawable.memorial_cover_02),
        BitmapFactory.decodeResource(resources, R.drawable.memorial_cover_03),
        BitmapFactory.decodeResource(resources, R.drawable.memorial_cover_04),
        BitmapFactory.decodeResource(resources, R.drawable.memorial_cover_05),
    )
    private val buttonTexture: Bitmap? = BitmapFactory.decodeResource(
        resources,
        R.drawable.memorial_button_bg,
    )
    private val buttonAspectRatio: Float = buttonTexture?.let { texture ->
        texture.width.toFloat() / texture.height.toFloat()
    } ?: (413f / 141f)
    private val completionTexture: Bitmap? = BitmapFactory.decodeResource(
        resources,
        R.drawable.memorial_completion_bg,
    )
    private val coverCornerTexture: Bitmap? = BitmapFactory.decodeResource(
        resources,
        R.drawable.memorial_cover_corner,
    )
    private val heritageTypeface: Typeface = runCatching {
        Typeface.createFromAsset(context.assets, "fonts/ma_shan_zheng_regular.ttf")
    }.getOrElse {
        Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    private val stampLikeTexture: Bitmap? = BitmapFactory.decodeResource(
        resources,
        R.drawable.memorial_stamp_like,
    )?.let(::buildCinnabarStampTexture)
    private val stampDislikeTexture: Bitmap? = BitmapFactory.decodeResource(
        resources,
        R.drawable.memorial_stamp_dislike,
    )?.let(::buildCinnabarStampTexture)
    private val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = 138
    }
    private val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val buttonImagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = 232
    }
    private val completionImagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val coverCornerPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = AndroidColor.rgb(221, 221, 221)
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }
    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val creasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = max(0.45f, displayDensity * 0.32f)
        color = AndroidColor.argb(12, 126, 111, 92)
    }
    private val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(51, 51, 51)
        textSize = sp(17f)
        typeface = heritageTypeface
    }
    private val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(102, 102, 102)
        textSize = sp(12f)
        typeface = heritageTypeface
    }
    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(26, 26, 26)
        textSize = sp(24f)
        typeface = heritageTypeface
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }
    private val itemTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(26, 26, 26)
        textSize = sp(18f)
        typeface = heritageTypeface
        textAlign = Paint.Align.LEFT
    }
    private val itemMetaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(154, 126, 74)
        textSize = sp(12f)
        typeface = heritageTypeface
        textAlign = Paint.Align.LEFT
    }
    private val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(154, 126, 74)
        textSize = sp(13f)
        typeface = heritageTypeface
        textAlign = Paint.Align.CENTER
    }
    private val cinnabarPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(184, 62, 47)
        textSize = sp(17f)
        typeface = heritageTypeface
    }
    private val coverTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(26, 26, 26)
        textSize = sp(58f)
        typeface = heritageTypeface
        textAlign = Paint.Align.CENTER
    }
    private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * displayDensity
        color = IMPERIAL_GOLD
    }
    private val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * displayDensity
        color = AndroidColor.rgb(184, 62, 47)
    }
    private val sealTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(184, 62, 47)
        textSize = sp(22f)
        typeface = heritageTypeface
        textAlign = Paint.Align.CENTER
    }
    private val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * displayDensity
        color = STAMP_RED
    }
    private val stampImagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val stampErasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
    private val stampTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = STAMP_PAPER
        textSize = sp(38f)
        typeface = heritageTypeface
        textAlign = Paint.Align.CENTER
    }
    private val toolbarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.argb(214, 255, 246, 221)
    }
    private val toolbarStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * displayDensity
        color = AndroidColor.argb(178, AndroidColor.red(IMPERIAL_GOLD), AndroidColor.green(IMPERIAL_GOLD), AndroidColor.blue(IMPERIAL_GOLD))
    }
    private val layerAlphaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val toolbarTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MEMORIAL_INK_BROWN
        textSize = sp(18f)
        typeface = heritageTypeface
        textAlign = Paint.Align.CENTER
    }
    private val toolbarButtonRect1 = RectF()
    private val toolbarButtonRect2 = RectF()
    private val collapseButtonRect = RectF()
    private val coverActionLeftRect = RectF()
    private val coverActionRightRect = RectF()
    private val coverActionKeepRect = RectF()
    private val coverActionSummaryRect = RectF()
    private val coverActionOpenRect = RectF()
    private val alphaLayerRect = RectF()
    private val paperClipPath = Path()

    private var velocityTracker: VelocityTracker? = null
    private var lastTouchX = 0f
    private var downTouchX = 0f
    private var downTouchY = 0f
    private var coverGestureStartAvgX = 0f
    private var coverGestureStartAvgY = 0f
    private var coverGestureLastAvgX = 0f
    private var coverGestureLastAvgY = 0f
    private var activePointerCount = 0
    private var coverDragX = 0f
    private var coverDragY = 0f
    private var coverSwipeAnimator: ValueAnimator? = null
    private var coverStackAnimator: ValueAnimator? = null
    private var coverPreviewStamp: MemorialStamp? = null
    private var coverPreviewStampStrength = 0f
    private var coverPreviewStampTargetStrength = 0f
    private var coverFinalStamp: MemorialStamp? = null
    private var coverFinalStampStrength = 0f
    private var coverFinalStampFromButton = false
    private var summaryAlpha = 0f
    private var summaryAnimator: ValueAnimator? = null
    private var completedAlpha = 0f
    private var completedAnimator: ValueAnimator? = null
    private var coverStackIndex = 0
    private var coverStackLiftProgress = 1f
    private var summaryVisible = false
    private var summaryWasShown = false
    private var summaryPinnedByTap = false
    private var summaryPinnedAtDown = false
    private var summaryShownByHold = false
    private var coverSummaryPressed = false
    private var coverTouchStartedOnCover = false
    private var summaryTouchX = 0f
    private var summaryTouchY = 0f
    private var isDragging = false
    private var pendingSpreadSnap = false
    private var foldScrollX = 0f
    private var maxScrollX = 0f
    private var articleWidth = 0f
    private var foldLeft = 0f
    private var foldRight = 0f
    private var foldTop = 0f
    private var foldBottom = 0f
    private var pages: List<MemorialPage> = emptyList()
    private var articles: List<ArticleLayout> = emptyList()
    private var openProgress = 1f
    private var transitionAnimator: ValueAnimator? = null
    private var scrollReturnAnimator: ValueAnimator? = null
    private var stampAnimator: ValueAnimator? = null
    private var stage = MemorialStage.CoverOnly
    private var stampProgress = 0f
    private var currentStamp: MemorialStamp? = null
    private var stampCompletion = StampCompletion.AutoDismiss
    private var toolbarPressedStamp: MemorialStamp? = null
    private var hideReadingControlsDuringClose = false
    private var onAutoDismiss: (() -> Unit)? = null
    private var hasPlayedOpenAnimation = false
    private val showSummaryRunnable = Runnable {
        if (
            stage == MemorialStage.CoverOnly &&
            coverSwipeAnimator == null &&
            coverStackAnimator == null &&
            currentStamp == null &&
            (coverTouchStartedOnCover || coverSummaryPressed)
        ) {
            coverPreviewStampTargetStrength = 0f
            coverPreviewStampStrength = 0f
            coverPreviewStamp = null
            showSummary(animated = true)
            summaryWasShown = true
            summaryShownByHold = true
            summaryPinnedByTap = false
            invalidate()
        }
    }

    init {
        isClickable = true
        isFocusable = true
        overScrollMode = OVER_SCROLL_NEVER
    }

    fun setPages(nextPages: List<MemorialPage>) {
        if (pages == nextPages) {
            invalidate()
            return
        }
        pages = nextPages
        foldScrollX = 0f
        openProgress = 0f
        completedAlpha = 0f
        coverStackAnimator?.cancel()
        coverStackAnimator = null
        summaryAnimator?.cancel()
        summaryAnimator = null
        summaryAlpha = 0f
        completedAnimator?.cancel()
        completedAnimator = null
        stage = MemorialStage.CoverOnly
        hasPlayedOpenAnimation = false
        rebuildLayout(width, height)
        invalidate()
    }

    fun setAutoDismissHandler(handler: () -> Unit) {
        onAutoDismiss = handler
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        rebuildLayout(width, height)
        foldScrollX = foldScrollX.coerceIn(0f, maxScrollX)
        startOpenAnimationIfReady()
    }

    override fun onDetachedFromWindow() {
        transitionAnimator?.cancel()
        transitionAnimator = null
        scrollReturnAnimator?.cancel()
        scrollReturnAnimator = null
        coverSwipeAnimator?.cancel()
        coverSwipeAnimator = null
        coverStackAnimator?.cancel()
        coverStackAnimator = null
        summaryAnimator?.cancel()
        summaryAnimator = null
        stampAnimator?.cancel()
        stampAnimator = null
        completedAnimator?.cancel()
        completedAnimator = null
        removeCallbacks(showSummaryRunnable)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val viewportWidth = foldRight - foldLeft
        if (viewportWidth <= 0f) return

        canvas.save()
        canvas.clipRect(
            foldLeft - dp(34f),
            foldTop - dp(34f),
            foldRight + dp(34f),
            foldBottom + dp(34f),
        )
        when (stage) {
            MemorialStage.CoverOnly -> drawCoverOnly(canvas)
            MemorialStage.Opening, MemorialStage.Closing -> {
                drawCoverStackLayer(canvas, coverStackAlpha())
                drawOpeningAnimation(canvas, viewportWidth)
                drawCoverControlsLayer(canvas, coverControlsAlpha())
                drawReadingControls(canvas, readingControlsAlpha())
            }
            MemorialStage.Expanded -> {
                drawReadingState(canvas, viewportWidth)
                drawReadingControls(canvas, readingControlsAlpha())
            }
            MemorialStage.Completed -> drawCompletedState(canvas, completedAlpha)
        }
        drawStampOverlay(canvas)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (articles.isEmpty()) return false

        if (stage == MemorialStage.Completed) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                performClick()
                dismissCompletedState()
            }
            return true
        }

        if (stage == MemorialStage.CoverOnly) {
            return handleCoverTouch(event)
        }

        if (stage != MemorialStage.Expanded) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                scroller.abortAnimation()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                lastTouchX = event.x
                downTouchX = event.x
                downTouchY = event.y
                toolbarPressedStamp = toolbarHitTest(event.x, event.y)
                if (toolbarPressedStamp == null && collapseButtonRect.contains(event.x, event.y)) {
                    toolbarPressedStamp = MemorialStamp.Collapse
                }
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val dx = lastTouchX - event.x
                if (!isDragging && abs(dx) > touchSlop) {
                    isDragging = true
                }
                if (isDragging) {
                    scrollByAmount(dx)
                    lastTouchX = event.x
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.addMovement(event)
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    val pressedStamp = toolbarPressedStamp
                    val releasedStamp = toolbarHitTest(event.x, event.y)
                    val releasedCollapse = collapseButtonRect.contains(event.x, event.y)
                    if (!isDragging && pressedStamp == MemorialStamp.Collapse && releasedCollapse) {
                        collapseToStack()
                    } else if (!isDragging && pressedStamp != null && pressedStamp == releasedStamp) {
                        startStampAndDismiss(pressedStamp)
                    } else {
                        velocityTracker?.computeCurrentVelocity(1000, maximumVelocity.toFloat())
                        val velocityX = velocityTracker?.xVelocity ?: 0f
                        if (abs(velocityX) > minimumVelocity) {
                            scroller.fling(
                                foldScrollX.roundToInt(),
                                0,
                                (-velocityX).roundToInt(),
                                0,
                                0,
                                maxScrollX.roundToInt(),
                                0,
                                0,
                            )
                            pendingSpreadSnap = true
                            postInvalidateOnAnimation()
                        } else if (isDragging) {
                            snapToNearestSpread()
                        }
                        performClick()
                    }
                }
                velocityTracker?.recycle()
                velocityTracker = null
                isDragging = false
                toolbarPressedStamp = null
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (stage != MemorialStage.Expanded) return true
        if (
            event.action == MotionEvent.ACTION_SCROLL &&
            event.isFromSource(InputDevice.SOURCE_CLASS_POINTER)
        ) {
            val horizontal = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
            val vertical = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val primary = if (abs(horizontal) >= abs(vertical)) {
                -horizontal
            } else {
                -vertical
            }
            if (primary != 0f) {
                scrollByAmount(primary * dp(96f))
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun handleCoverTouch(event: MotionEvent): Boolean {
        if (
            currentStamp != null ||
            coverFinalStamp != null ||
            coverSwipeAnimator != null ||
            coverStackAnimator != null
        ) {
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                coverSwipeAnimator?.cancel()
                removeCallbacks(showSummaryRunnable)
                val cover = articles.firstOrNull()
                val coverLeft = cover?.let { coverStackLeft(it) } ?: 0f
                coverTouchStartedOnCover = cover?.let {
                    RectF(coverLeft, it.top, coverLeft + it.width, it.top + it.height).contains(event.x, event.y)
                } ?: false
                activePointerCount = event.pointerCount
                coverGestureStartAvgX = averagePointerX(event)
                coverGestureStartAvgY = averagePointerY(event)
                coverGestureLastAvgX = coverGestureStartAvgX
                coverGestureLastAvgY = coverGestureStartAvgY
                downTouchX = event.x
                downTouchY = event.y
                toolbarPressedStamp = coverActionHitTest(event.x, event.y)
                summaryTouchX = event.x
                summaryTouchY = event.y
                summaryPinnedAtDown = summaryPinnedByTap
                summaryShownByHold = false
                if (!summaryPinnedByTap) {
                    hideSummary(animated = false)
                }
                summaryWasShown = false
                coverSummaryPressed = coverActionSummaryRect.contains(event.x, event.y)
                if (coverSummaryPressed) {
                    postDelayed(showSummaryRunnable, 120L)
                } else if (coverTouchStartedOnCover && toolbarPressedStamp == null) {
                    postDelayed(showSummaryRunnable, 420L)
                }
                coverDragX = 0f
                coverDragY = 0f
                coverPreviewStamp = null
                coverPreviewStampStrength = 0f
                coverPreviewStampTargetStrength = 0f
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                activePointerCount = max(activePointerCount, event.pointerCount)
                coverGestureLastAvgX = averagePointerX(event)
                coverGestureLastAvgY = averagePointerY(event)
                coverDragX = coverGestureLastAvgX - coverGestureStartAvgX
                coverDragY = (coverGestureLastAvgY - coverGestureStartAvgY) * 0.28f
                coverFinalStamp = null
                coverFinalStampFromButton = false
                coverFinalStampStrength = 0f
                summaryTouchX = coverGestureLastAvgX
                summaryTouchY = coverGestureLastAvgY
                if (
                    abs(coverGestureLastAvgX - coverGestureStartAvgX) > touchSlop ||
                    abs(coverGestureLastAvgY - coverGestureStartAvgY) > touchSlop
                ) {
                    isDragging = true
                    if (!summaryPinnedByTap) {
                        hideSummary(animated = true)
                    }
                    removeCallbacks(showSummaryRunnable)
                    if (coverTouchStartedOnCover || coverSummaryPressed) {
                        postDelayed(showSummaryRunnable, 360L)
                    }
                }
                updateCoverPreviewStamp()
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                removeCallbacks(showSummaryRunnable)
                val consumedLongPress = summaryWasShown
                if (!summaryPinnedByTap) {
                    hideSummary(animated = true)
                }
                val dx = coverGestureLastAvgX - coverGestureStartAvgX
                val dy = coverGestureLastAvgY - coverGestureStartAvgY
                val twoFingerKeep = activePointerCount >= 2 && -dy > dp(54f) && abs(dy) > abs(dx) * 1.2f
                val horizontalVerdict = abs(dx) > dp(64f) && abs(dx) > abs(dy) * 1.25f
                val pressedAction = toolbarPressedStamp
                val releasedAction = coverActionHitTest(event.x, event.y)
                val releasedSummary = coverActionSummaryRect.contains(event.x, event.y)
                when {
                    !isDragging && coverSummaryPressed && releasedSummary -> {
                        val shouldPin = !summaryPinnedAtDown && !summaryShownByHold
                        summaryPinnedByTap = shouldPin
                        setSummaryVisible(shouldPin, animated = true)
                        summaryWasShown = false
                        summaryShownByHold = false
                        coverDragX = 0f
                        coverDragY = 0f
                        coverPreviewStamp = null
                        invalidate()
                    }
                    !isDragging && pressedAction == MemorialStamp.Reject && releasedAction == pressedAction ->
                        triggerCoverAction(MemorialStamp.Reject)
                    !isDragging && pressedAction == MemorialStamp.Approve && releasedAction == pressedAction ->
                        triggerCoverAction(MemorialStamp.Approve)
                    !isDragging && pressedAction == MemorialStamp.Keep && releasedAction == pressedAction ->
                        triggerCoverAction(MemorialStamp.Keep)
                    !isDragging && pressedAction == MemorialStamp.Collapse && releasedAction == pressedAction -> {
                        coverDragX = 0f
                        coverDragY = 0f
                        coverPreviewStamp = null
                        summaryPinnedByTap = false
                        performClick()
                        expandFromCover()
                    }
                    twoFingerKeep -> startCoverVerdict(MemorialStamp.Keep, dx, -1f)
                    horizontalVerdict && dx > 0f -> startCoverVerdict(MemorialStamp.Approve, dx, 0f)
                    horizontalVerdict && dx < 0f -> startCoverVerdict(MemorialStamp.Reject, dx, 0f)
                    consumedLongPress -> animateCoverBack()
                    !isDragging &&
                        coverTouchStartedOnCover &&
                        abs(event.x - downTouchX) < touchSlop &&
                        abs(event.y - downTouchY) < touchSlop -> {
                        coverDragX = 0f
                        coverDragY = 0f
                        coverPreviewStamp = null
                        coverPreviewStampStrength = 0f
                        coverPreviewStampTargetStrength = 0f
                        summaryPinnedByTap = false
                        performClick()
                        expandFromCover()
                    }
                    !isDragging && toolbarPressedStamp == null && !coverTouchStartedOnCover -> {
                        hideSummary(animated = true)
                        summaryPinnedByTap = false
                        summaryPinnedAtDown = false
                        summaryShownByHold = false
                        performClick()
                        onAutoDismiss?.invoke()
                    }
                    else -> animateCoverBack()
                }
                activePointerCount = 0
                isDragging = false
                summaryWasShown = false
                summaryShownByHold = false
                coverSummaryPressed = false
                coverTouchStartedOnCover = false
                toolbarPressedStamp = null
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(showSummaryRunnable)
                hideSummary(animated = true)
                summaryWasShown = false
                summaryPinnedByTap = false
                summaryPinnedAtDown = false
                summaryShownByHold = false
                coverSummaryPressed = false
                activePointerCount = 0
                isDragging = false
                toolbarPressedStamp = null
                coverFinalStamp = null
                coverFinalStampFromButton = false
                coverFinalStampStrength = 0f
                coverPreviewStampStrength = 0f
                coverPreviewStampTargetStrength = 0f
                coverTouchStartedOnCover = false
                animateCoverBack()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            foldScrollX = scroller.currX.toFloat().coerceIn(0f, maxScrollX)
            postInvalidateOnAnimation()
        } else if (pendingSpreadSnap) {
            pendingSpreadSnap = false
            snapToNearestSpread()
        }
    }

    private fun drawCoverOnly(canvas: Canvas) {
        val cover = articles.firstOrNull() ?: return
        val coverLeft = coverStackLeft(cover)
        drawCoverStackLayer(canvas, coverStackAlpha())
        val rotation = (coverDragX / max(1f, foldRight - foldLeft)).coerceIn(-1f, 1f) * 10f
        val isRevealingNextCover = currentStamp == null &&
            coverFinalStamp == null &&
            coverDragX == 0f &&
            coverDragY == 0f &&
            coverStackLiftProgress < 1f
        val revealRest = 1f - coverStackLiftProgress.coerceIn(0f, 1f)
        val revealOffsetX = if (isRevealingNextCover) stackOffsetX(1) * revealRest else 0f
        val revealOffsetY = if (isRevealingNextCover) stackOffsetY(1) * revealRest else 0f
        val revealScale = if (isRevealingNextCover) {
            lerp(1f, stackScale(1), revealRest)
        } else {
            1f
        }
        val revealRotation = if (isRevealingNextCover) stackRotation(1) * revealRest else 0f
        canvas.save()
        if (isRevealingNextCover) {
            canvas.translate(revealOffsetX, revealOffsetY)
            canvas.rotate(revealRotation, coverLeft + cover.width / 2f, cover.top + cover.height / 2f)
            canvas.scale(revealScale, revealScale, coverLeft + cover.width / 2f, cover.top + cover.height / 2f)
        }
        if (coverDragX != 0f || coverDragY != 0f) {
            canvas.rotate(rotation, coverLeft + cover.width / 2f, cover.top + cover.height * 0.62f)
            canvas.translate(coverDragX, coverDragY)
        }
        drawArticle(
            canvas = canvas,
            article = cover,
            left = coverLeft,
            coverSequenceIndex = coverStackIndex,
            transform = FoldTransform(
                rotationY = 0f,
                pivotX = coverLeft + cover.width,
                shadingAlpha = 0f,
                edgeShadowProgress = 0f,
                visible = true,
            ),
        )
        drawCoverSwipePreview(canvas, coverLeft, cover)
        canvas.restore()
        drawSummaryBubble(canvas, coverLeft, cover)
        drawCoverControlsLayer(canvas, coverControlsAlpha())
    }

    private fun coverStackLeft(cover: ArticleLayout): Float {
        return foldLeft + ((foldRight - foldLeft) - cover.width) / 2f
    }

    private fun openingAlignmentOffset(): Float {
        val cover = articles.firstOrNull() ?: return 0f
        val centeredLeft = coverStackLeft(cover)
        return centeredLeft - foldLeft
    }

    private fun openingBaseLeft(): Float {
        return foldLeft + openingAlignmentOffset() * (1f - openProgress.coerceIn(0f, 1f))
    }

    private fun drawCoverStackLayer(canvas: Canvas, alpha: Float) {
        val cover = articles.firstOrNull() ?: return
        if (alpha <= 0.01f) return
        drawWithTransitionAlpha(canvas, alpha) {
            drawCoverStack(canvas, cover, coverStackLeft(cover))
        }
    }

    private fun drawCoverControlsLayer(canvas: Canvas, alpha: Float) {
        if (alpha <= 0.01f) return
        drawWithTransitionAlpha(canvas, alpha) {
            drawCoverGestureHint(canvas)
        }
    }

    private fun drawReadingControls(canvas: Canvas, alpha: Float) {
        if (alpha <= 0.01f) return
        drawWithTransitionAlpha(canvas, alpha) {
            drawCollapseButton(canvas)
            drawVerdictToolbar(canvas)
        }
    }

    private fun drawWithTransitionAlpha(canvas: Canvas, alpha: Float, block: () -> Unit) {
        val clamped = alpha.coerceIn(0f, 1f)
        if (clamped <= 0.01f) return
        if (clamped >= 0.995f) {
            block()
            return
        }

        alphaLayerRect.set(
            foldLeft - dp(72f),
            foldTop - dp(72f),
            foldRight + dp(72f),
            foldBottom + dp(72f),
        )
        val previousAlpha = layerAlphaPaint.alpha
        layerAlphaPaint.alpha = (255f * clamped).roundToInt().coerceIn(0, 255)
        val layer = canvas.saveLayer(alphaLayerRect, layerAlphaPaint)
        block()
        canvas.restoreToCount(layer)
        layerAlphaPaint.alpha = previousAlpha
    }

    private fun coverStackAlpha(): Float {
        return when (stage) {
            MemorialStage.CoverOnly -> 1f
            MemorialStage.Opening,
            MemorialStage.Closing -> 1f - smoothStep(0.06f, 0.32f, openProgress)
            else -> 0f
        }
    }

    private fun coverControlsAlpha(): Float {
        return when (stage) {
            MemorialStage.CoverOnly -> 1f
            MemorialStage.Opening,
            MemorialStage.Closing -> 1f - smoothStep(0.04f, 0.24f, openProgress)
            else -> 0f
        }
    }

    private fun readingControlsAlpha(): Float {
        return when (stage) {
            MemorialStage.Opening,
            MemorialStage.Closing -> if (hideReadingControlsDuringClose) {
                0f
            } else {
                smoothStep(0.62f, 0.88f, openProgress)
            }
            MemorialStage.Expanded -> if (currentStamp != null) {
                1f - smoothStep(0.08f, 0.42f, stampProgress)
            } else {
                1f
            }
            else -> 0f
        }
    }

    private fun drawCoverStack(canvas: Canvas, cover: ArticleLayout, coverLeft: Float) {
        val remaining = (TOTAL_PENDING_MEMORIALS - coverStackIndex - 1).coerceAtLeast(0)
        val stackCount = min(remaining, 5)
        val isVerdictLeaving = coverFinalStamp != null || currentStamp != null
        val isLiftingTopCover = currentStamp == null &&
            !isVerdictLeaving &&
            abs(coverDragX) < 0.5f &&
            abs(coverDragY) < 0.5f &&
            coverStackLiftProgress < 1f
        for (level in stackCount downTo 1) {
            if (isLiftingTopCover && level == 1) continue
            val lift = if (level == 1 && isVerdictLeaving) coverStackLiftProgress.coerceIn(0f, 1f) else 0f
            val offsetX = stackOffsetX(level) * (1f - lift)
            val offsetY = stackOffsetY(level) * (1f - lift)
            val scale = lerp(stackScale(level), 1f, lift)
            val rotation = stackRotation(level) * (1f - lift)
            val stackRect = RectF(
                coverLeft + offsetX,
                cover.top + offsetY,
                coverLeft + offsetX + cover.width,
                cover.top + offsetY + cover.height,
            )
            canvas.save()
            canvas.rotate(rotation, stackRect.centerX(), stackRect.centerY())
            canvas.scale(scale, scale, stackRect.centerX(), stackRect.centerY())
            drawArticle(
                canvas = canvas,
                article = cover,
                left = stackRect.left,
                coverSequenceIndex = coverStackIndex + level,
                transform = FoldTransform(
                    rotationY = 0f,
                    pivotX = stackRect.right,
                    shadingAlpha = 0f,
                    edgeShadowProgress = 0f,
                    visible = true,
                ),
            )
            canvas.restore()
        }
    }

    private fun stackOffsetX(level: Int): Float {
        return when (level) {
            1 -> dp(8f)
            2 -> -dp(7f)
            3 -> dp(13f)
            4 -> -dp(11f)
            else -> dp(5f)
        }
    }

    private fun stackOffsetY(level: Int): Float {
        return dp(7f + level * 6.5f)
    }

    private fun stackScale(level: Int): Float {
        return 1f - level * 0.014f
    }

    private fun stackRotation(level: Int): Float {
        return when (level) {
            1 -> -1.1f
            2 -> 1.35f
            3 -> -0.8f
            4 -> 1.05f
            else -> -0.55f
        }
    }

    private fun drawCoverGestureHint(canvas: Canvas) {
        val bottomGap = dp(12f)
        val buttonHeight = dp(48f)
        val bottomTop = foldBottom - buttonHeight - dp(18f)
        layoutButtonRow(
            rects = listOf(coverActionLeftRect, coverActionRightRect, coverActionKeepRect),
            centerX = foldLeft + (foldRight - foldLeft) / 2f,
            top = bottomTop,
            height = buttonHeight,
            desiredGap = bottomGap,
            maxWidth = foldRight - foldLeft - dp(32f),
        )

        val topTotalWidth = min(dp(430f), foldRight - foldLeft - dp(32f))
        val topLeft = foldLeft + ((foldRight - foldLeft) - topTotalWidth) / 2f
        val top = foldTop + dp(18f)
        val topButtonWidth = min(buttonHeight * buttonAspectRatio, topTotalWidth * 0.36f)
        val topButtonCenterInset = topButtonWidth / 2f
        layoutAspectButton(
            rect = coverActionSummaryRect,
            centerX = topLeft + topButtonCenterInset,
            top = top,
            height = buttonHeight,
            maxWidth = topButtonWidth,
        )
        layoutAspectButton(
            rect = coverActionOpenRect,
            centerX = topLeft + topTotalWidth - topButtonCenterInset,
            top = top,
            height = buttonHeight,
            maxWidth = topButtonWidth,
        )

        drawToolbarButton(canvas, coverActionSummaryRect, "长按简览")
        drawToolbarButton(canvas, coverActionOpenRect, "点击展开")
        drawToolbarButton(canvas, coverActionLeftRect, "左滑驳回")
        drawToolbarButton(canvas, coverActionRightRect, "右滑准奏")
        drawToolbarButton(canvas, coverActionKeepRect, "上滑留中")

        val counter = "第 ${coverStackIndex + 1}/$TOTAL_PENDING_MEMORIALS 封"
        drawCenteredText(
            canvas,
            counter,
            topLeft + topTotalWidth / 2f,
            coverActionSummaryRect.centerY() + textCenterOffset(toolbarTextPaint),
            toolbarTextPaint,
        )
    }

    private fun coverActionHitTest(x: Float, y: Float): MemorialStamp? {
        return when {
            coverActionLeftRect.contains(x, y) -> MemorialStamp.Reject
            coverActionRightRect.contains(x, y) -> MemorialStamp.Approve
            coverActionKeepRect.contains(x, y) -> MemorialStamp.Keep
            coverActionOpenRect.contains(x, y) -> MemorialStamp.Collapse
            else -> null
        }
    }

    private fun showSummary(animated: Boolean) {
        setSummaryVisible(visible = true, animated = animated)
    }

    private fun hideSummary(animated: Boolean) {
        setSummaryVisible(visible = false, animated = animated)
    }

    private fun setSummaryVisible(visible: Boolean, animated: Boolean) {
        if (summaryVisible == visible && ((visible && summaryAlpha >= 1f) || (!visible && summaryAlpha <= 0f))) {
            return
        }
        summaryAnimator?.cancel()
        summaryVisible = visible
        val target = if (visible) 1f else 0f
        if (!animated) {
            summaryAlpha = target
            invalidate()
            return
        }

        summaryAnimator = ValueAnimator.ofFloat(summaryAlpha.coerceIn(0f, 1f), target).apply {
            duration = SUMMARY_FADE_DURATION_MS
            interpolator = PathInterpolator(0.22f, 0f, 0.2f, 1f)
            addUpdateListener { animator ->
                summaryAlpha = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (summaryAnimator == animation) {
                        summaryAnimator = null
                    }
                    if (!canceled) {
                        summaryAlpha = target
                        summaryVisible = visible
                        invalidate()
                    }
                }
            })
            start()
        }
    }

    private fun updateCoverPreviewStamp() {
        if (!coverTouchStartedOnCover || summaryAlpha > 0.05f) {
            coverPreviewStamp = null
            coverPreviewStampTargetStrength = 0f
            coverPreviewStampStrength = lerp(coverPreviewStampStrength, 0f, 0.32f)
            if (coverPreviewStampStrength < 0.02f) {
                coverPreviewStampStrength = 0f
            }
            return
        }

        val horizontalThreshold = dp(46f)
        val horizontalFull = dp(86f)
        val keepThreshold = dp(34f)
        val keepFull = dp(72f)
        val targetStamp = when {
            activePointerCount >= 2 && -coverDragY > keepThreshold -> MemorialStamp.Keep
            coverDragX > horizontalThreshold -> MemorialStamp.Approve
            coverDragX < -horizontalThreshold -> MemorialStamp.Reject
            else -> null
        }
        val targetStrength = when (targetStamp) {
            MemorialStamp.Keep -> smoothStep(keepThreshold, keepFull, -coverDragY)
            MemorialStamp.Approve,
            MemorialStamp.Reject -> smoothStep(horizontalThreshold, horizontalFull, abs(coverDragX))
            else -> 0f
        }

        if (targetStamp != null) {
            coverPreviewStamp = targetStamp
        }
        coverPreviewStampTargetStrength = targetStrength
        coverPreviewStampStrength = lerp(coverPreviewStampStrength, coverPreviewStampTargetStrength, 0.36f)
        if (targetStamp == null && coverPreviewStampStrength < 0.02f) {
            coverPreviewStamp = null
            coverPreviewStampStrength = 0f
        }
    }

    private fun drawSummaryBubble(canvas: Canvas, coverLeft: Float, cover: ArticleLayout) {
        if ((!summaryVisible && summaryAlpha <= 0.01f) || coverPreviewStamp != null || coverFinalStamp != null) return
        val summary = pendingMemorialSummaries[coverStackIndex.coerceIn(0, pendingMemorialSummaries.lastIndex)]
        val side = min(
            min(dp(388f), cover.width * 0.9f),
            min(foldRight - foldLeft - dp(40f), foldBottom - foldTop - dp(104f)),
        )
        if (side <= 0f) return
        val centerX = foldLeft + (foldRight - foldLeft) / 2f
        val centerY = cover.top + cover.height * 0.48f
        val left = (centerX - side / 2f).coerceIn(foldLeft + dp(20f), foldRight - dp(20f) - side)
        val top = (centerY - side / 2f).coerceIn(foldTop + dp(22f), foldBottom - dp(22f) - side)
        val rect = RectF(left, top, left + side, top + side)

        val alpha = summaryAlpha.coerceIn(0f, 1f)
        val layerAlpha = (255f * alpha).roundToInt().coerceIn(0, 255)
        if (layerAlpha <= 0) return
        val previousAlpha = layerAlphaPaint.alpha
        layerAlphaPaint.alpha = layerAlpha
        val layer = canvas.saveLayer(rect, layerAlphaPaint)
        val scale = lerp(0.985f, 1f, smoothStep(0f, 1f, alpha))
        canvas.scale(scale, scale, rect.centerX(), rect.centerY())
        drawPaperPanel(
            canvas = canvas,
            rect = rect,
            pageIndex = 37 + coverStackIndex,
            clippedCircle = false,
        )
        drawDoubleGoldFrame(canvas, rect, outerInset = dp(15f), innerInset = dp(24f))
        val ornamentRect = RectF(rect).apply { inset(dp(24f), dp(24f)) }
        drawCoverCornerOrnaments(canvas, ornamentRect, sizeScale = 0.14f)

        val titlePaint = TextPaint(itemTitlePaint).apply {
            textSize = sp(26f)
            textAlign = Paint.Align.LEFT
            color = MEMORIAL_INK_BROWN
        }
        val sourcePaint = TextPaint(itemMetaPaint).apply {
            textSize = sp(18f)
            textAlign = Paint.Align.LEFT
            color = IMPERIAL_GOLD_DARK
        }
        val bodyPaint = TextPaint(authorPaint).apply {
            textSize = sp(20f)
            textAlign = Paint.Align.LEFT
            color = MEMORIAL_INK_BROWN
        }

        val contentWidth = (rect.width() * 0.74f).roundToInt().coerceAtLeast(1)
        val contentLeft = rect.centerX() - contentWidth / 2f
        val titleLayout = buildEllipsizedTextLayout(summary.title, titlePaint, contentWidth, 1.18f, 1)
        val sourceLayout = buildEllipsizedTextLayout(summary.source, sourcePaint, contentWidth, 1.18f, 1)
        val bodyLayout = buildEllipsizedTextLayout(summary.summary, bodyPaint, contentWidth, 1.42f, 4)
        val gap1 = dp(13f)
        val gap2 = dp(18f)
        val totalHeight = titleLayout.height + gap1 + sourceLayout.height + gap2 + bodyLayout.height
        var y = rect.centerY() - totalHeight / 2f
        drawStaticLayout(canvas, titleLayout, contentLeft, y)
        y += titleLayout.height + gap1
        drawStaticLayout(canvas, sourceLayout, contentLeft, y)
        y += sourceLayout.height + gap2
        drawStaticLayout(canvas, bodyLayout, contentLeft, y)
        canvas.restoreToCount(layer)
        layerAlphaPaint.alpha = previousAlpha
    }

    private fun drawCompletedState(canvas: Canvas, alpha: Float) {
        if (alpha <= 0.01f) return
        val side = min(
            min(dp(480f), foldRight - foldLeft - dp(52f)),
            foldBottom - foldTop - dp(56f),
        )
        val left = foldLeft + ((foldRight - foldLeft) - side) / 2f
        val top = foldTop + ((foldBottom - foldTop) - side) / 2f
        val rect = RectF(left, top, left + side, top + side)
        layerAlphaPaint.alpha = (255f * alpha.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
        val layer = canvas.saveLayer(rect, layerAlphaPaint)

        completionTexture?.let { texture ->
            canvas.drawBitmap(texture, null, rect, completionImagePaint)
        } ?: drawPaperPanel(
            canvas = canvas,
            rect = rect,
            pageIndex = 61,
            clippedCircle = true,
        )

        val completionTitlePaint = TextPaint(titlePaint).apply {
            textSize = sp(33f)
            letterSpacing = 0.08f
        }
        val completionBodyPaint = TextPaint(quotePaint).apply {
            textSize = sp(22f)
            color = AndroidColor.rgb(72, 43, 31)
            textAlign = Paint.Align.LEFT
        }
        val completionHintPaint = TextPaint(metaPaint).apply {
            textSize = sp(21f)
            color = IMPERIAL_GOLD_DARK
        }

        val contentWidth = (rect.width() * 0.64f).roundToInt()
        val contentLeft = rect.centerX() - contentWidth / 2f
        drawCenteredText(canvas, "诸折既毕", rect.centerX(), rect.top + rect.height() * 0.31f, completionTitlePaint)
        drawTextBlock(
            canvas = canvas,
            text = "六奏皆经圣裁，御前案牍暂清。伏愿皇上少憩片刻，以养宸躬。",
            paint = completionBodyPaint,
            left = contentLeft,
            top = rect.top + rect.height() * 0.42f,
            width = contentWidth.toFloat(),
            lineHeightMultiplier = 1.7f,
            justify = false,
            alignment = Layout.Alignment.ALIGN_CENTER,
        )
        drawCenteredText(canvas, "轻点退朝", rect.centerX(), rect.bottom - rect.height() * 0.24f, completionHintPaint)
        canvas.restoreToCount(layer)
        layerAlphaPaint.alpha = 255
    }

    private fun drawCoverSwipePreview(canvas: Canvas, coverLeft: Float, cover: ArticleLayout) {
        val stamp = coverFinalStamp ?: coverPreviewStamp ?: return
        val strength = when (stamp) {
            coverFinalStamp -> coverFinalStampStrength.coerceIn(0f, 1f)
            MemorialStamp.Keep,
            MemorialStamp.Approve,
            MemorialStamp.Reject -> coverPreviewStampStrength.coerceIn(0f, 1f)
            else -> 0f
        }
        if (strength <= 0f) return

        val cx = when (stamp) {
            coverFinalStamp -> if (coverFinalStampFromButton) {
                coverLeft + cover.width * 0.5f
            } else {
                when (stamp) {
                    MemorialStamp.Approve -> coverLeft + cover.width * (1f / 3f)
                    MemorialStamp.Reject -> coverLeft + cover.width * (2f / 3f)
                    else -> coverLeft + cover.width * 0.5f
                }
            }
            MemorialStamp.Approve -> coverLeft + cover.width * (1f / 3f)
            MemorialStamp.Reject -> coverLeft + cover.width * (2f / 3f)
            else -> coverLeft + cover.width * 0.5f
        }
        val cy = when (stamp) {
            coverFinalStamp -> if (coverFinalStampFromButton) {
                cover.top + cover.height * 0.64f
            } else {
                when (stamp) {
                    MemorialStamp.Approve,
                    MemorialStamp.Reject -> cover.top + cover.height * 0.76f
                    MemorialStamp.Keep -> cover.top + cover.height * 0.72f
                    else -> cover.top + cover.height * 0.55f
                }
            }
            MemorialStamp.Approve,
            MemorialStamp.Reject -> cover.top + cover.height * 0.76f
            MemorialStamp.Keep -> cover.top + cover.height * 0.72f
            else -> cover.top + cover.height * 0.55f
        }
        drawRubberStamp(canvas, stamp, cx, cy, strength, preview = true)
    }

    private fun drawVerdictToolbar(canvas: Canvas) {
        val toolbarHeight = dp(52f)
        val toolbarTop = foldBottom - toolbarHeight - dp(18f)
        val gap = dp(18f)
        layoutButtonRow(
            rects = listOf(toolbarButtonRect1, toolbarButtonRect2),
            centerX = foldLeft + (foldRight - foldLeft) / 2f,
            top = toolbarTop,
            height = toolbarHeight,
            desiredGap = gap,
            maxWidth = foldRight - foldLeft - dp(72f),
        )
        drawToolbarButton(canvas, toolbarButtonRect1, "朕喜欢")
        drawToolbarButton(canvas, toolbarButtonRect2, "朕不喜欢")
    }

    private fun drawCollapseButton(canvas: Canvas) {
        val buttonHeight = dp(42f)
        val top = foldTop + dp(18f)
        layoutAspectButton(
            rect = collapseButtonRect,
            centerX = foldLeft + (foldRight - foldLeft) / 2f,
            top = top,
            height = buttonHeight,
            maxWidth = foldRight - foldLeft - dp(72f),
        )
        drawToolbarButton(canvas, collapseButtonRect, "收起")
    }

    private fun layoutButtonRow(
        rects: List<RectF>,
        centerX: Float,
        top: Float,
        height: Float,
        desiredGap: Float,
        maxWidth: Float,
    ) {
        if (rects.isEmpty()) return
        val desiredButtonWidth = height * buttonAspectRatio
        val desiredTotalWidth = desiredButtonWidth * rects.size + desiredGap * (rects.size - 1)
        val scale = if (desiredTotalWidth > maxWidth && desiredTotalWidth > 0f) {
            (maxWidth / desiredTotalWidth).coerceIn(0f, 1f)
        } else {
            1f
        }
        val buttonHeight = height * scale
        val buttonWidth = buttonHeight * buttonAspectRatio
        val gap = desiredGap * scale
        val totalWidth = buttonWidth * rects.size + gap * (rects.size - 1)
        val rectTop = top + (height - buttonHeight) / 2f
        var left = centerX - totalWidth / 2f
        rects.forEach { rect ->
            rect.set(left, rectTop, left + buttonWidth, rectTop + buttonHeight)
            left += buttonWidth + gap
        }
    }

    private fun layoutAspectButton(
        rect: RectF,
        centerX: Float,
        top: Float,
        height: Float,
        maxWidth: Float,
    ) {
        val desiredWidth = height * buttonAspectRatio
        val scale = if (desiredWidth > maxWidth && desiredWidth > 0f) {
            (maxWidth / desiredWidth).coerceIn(0f, 1f)
        } else {
            1f
        }
        val buttonHeight = height * scale
        val width = buttonHeight * buttonAspectRatio
        val left = centerX - width / 2f
        val rectTop = top + (height - buttonHeight) / 2f
        rect.set(left, rectTop, left + width, rectTop + buttonHeight)
    }

    private fun drawToolbarButton(canvas: Canvas, rect: RectF, label: String) {
        buttonTexture?.let { texture ->
            canvas.drawBitmap(texture, null, rect, buttonImagePaint)
        } ?: run {
            canvas.drawRoundRect(rect, dp(10f), dp(10f), toolbarPaint)
            canvas.drawRoundRect(rect, dp(10f), dp(10f), toolbarStrokePaint)
        }
        drawCenteredText(canvas, label, rect.centerX(), rect.centerY() + textCenterOffset(toolbarTextPaint), toolbarTextPaint)
    }

    private fun toolbarHitTest(x: Float, y: Float): MemorialStamp? {
        return when {
            toolbarButtonRect1.contains(x, y) -> MemorialStamp.Like
            toolbarButtonRect2.contains(x, y) -> MemorialStamp.Dislike
            else -> null
        }
    }

    private fun drawStampOverlay(canvas: Canvas) {
        val stamp = currentStamp ?: return
        if (stampProgress <= 0f) return

        val appear = smoothStep(0f, 0.32f, stampProgress)
        val settle = smoothStep(0.18f, 1f, stampProgress)
        val scale = 1.36f - 0.36f * settle
        val cx = foldLeft + (foldRight - foldLeft) / 2f
        val cy = foldTop + (foldBottom - foldTop) * 0.55f

        canvas.save()
        canvas.scale(scale, scale, cx, cy)
        drawRubberStamp(canvas, stamp, cx, cy, appear, preview = false)
        canvas.restore()
    }

    private fun drawRubberStamp(
        canvas: Canvas,
        stamp: MemorialStamp,
        cx: Float,
        cy: Float,
        strength: Float,
        preview: Boolean,
    ) {
        if (stamp == MemorialStamp.Collapse) return
        val alphaBase = if (preview) 255 else 255
        val alpha = (alphaBase * strength.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
        if (alpha <= 0) return

        val label = stamp.label
        stampTextPaint.color = STAMP_PAPER
        stampTextPaint.textSize = when (stamp) {
            MemorialStamp.Like -> sp(38f)
            MemorialStamp.Dislike -> sp(34f)
            MemorialStamp.Keep -> sp(37f)
            else -> sp(50f)
        }
        stampTextPaint.style = Paint.Style.FILL_AND_STROKE
        stampTextPaint.strokeWidth = dp(0.45f)
        val textMetrics = stampTextPaint.fontMetrics
        val charHeight = textMetrics.descent - textMetrics.ascent
        val charGap = when (stamp) {
            MemorialStamp.Like, MemorialStamp.Dislike -> dp(1f)
            else -> dp(2f)
        }
        val maxCharWidth = label.maxOf { stampTextPaint.measureText(it.toString()) }
        val textHeight = label.length * charHeight + (label.length - 1).coerceAtLeast(0) * charGap
        val rectWidth = max(dp(78f), maxCharWidth + dp(38f))
        val rectHeight = max(
            when (stamp) {
                MemorialStamp.Like -> dp(202f)
                MemorialStamp.Dislike -> dp(228f)
                MemorialStamp.Keep -> dp(132f)
                else -> dp(108f)
            },
            textHeight + if (stamp == MemorialStamp.Like || stamp == MemorialStamp.Dislike) dp(62f) else dp(42f),
        )
        val rect = RectF(cx - rectWidth / 2f, cy - rectHeight / 2f, cx + rectWidth / 2f, cy + rectHeight / 2f)
        val rotation = when (stamp) {
            MemorialStamp.Approve -> -13f
            MemorialStamp.Reject -> 8f
            MemorialStamp.Keep -> -6f
            MemorialStamp.Like -> -9f
            MemorialStamp.Dislike -> 7f
            MemorialStamp.Collapse -> 0f
        }

        canvas.save()
        canvas.rotate(rotation, cx, cy)
        val layerBounds = RectF(rect).apply {
            inset(-dp(6f), -dp(6f))
        }
        val layer = canvas.saveLayer(layerBounds, null)
        stampTextureFor(stamp)?.let { texture ->
            stampImagePaint.alpha = alpha.coerceIn(0, 255)
            canvas.drawBitmap(texture, null, rect, stampImagePaint)
        } ?: run {
            stampPaint.alpha = alpha.coerceIn(0, 255)
            canvas.drawRoundRect(rect, dp(7f), dp(7f), stampPaint)
        }
        stampTextPaint.alpha = alpha.coerceIn(0, 255)
        drawVerticalStampText(canvas, label, cx, cy, charHeight, charGap)
        drawStampDistress(canvas, rect, alpha, stamp)
        canvas.restoreToCount(layer)
        stampPaint.alpha = 255
        stampImagePaint.alpha = 255
        stampTextPaint.alpha = 255
        stampTextPaint.strokeWidth = 0f
        stampTextPaint.style = Paint.Style.FILL
        canvas.restore()
    }

    private fun drawVerticalStampText(
        canvas: Canvas,
        label: String,
        cx: Float,
        cy: Float,
        charHeight: Float,
        charGap: Float,
    ) {
        val textHeight = label.length * charHeight + (label.length - 1).coerceAtLeast(0) * charGap
        val startCenterY = cy - textHeight / 2f + charHeight / 2f
        val baselineOffset = -(stampTextPaint.fontMetrics.ascent + stampTextPaint.fontMetrics.descent) / 2f
        label.forEachIndexed { index, char ->
            val charCenterY = startCenterY + index * (charHeight + charGap)
            canvas.drawText(char.toString(), cx, charCenterY + baselineOffset, stampTextPaint)
        }
    }

    private fun drawStampDistress(canvas: Canvas, rect: RectF, alpha: Int, stamp: MemorialStamp) {
        val columns = 7
        val rows = 12
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val noise = stableNoise(stamp.ordinal, column, row)
                if (noise > 0.68f) {
                    val localX = (column + 0.25f + stableNoise(stamp.ordinal + 11, column, row) * 0.5f) / columns
                    val localY = (row + 0.18f + stableNoise(stamp.ordinal + 23, column, row) * 0.64f) / rows
                    val radius = dp(0.45f + stableNoise(stamp.ordinal + 31, column, row) * 1.25f)
                    stampErasePaint.alpha = (alpha * (0.12f + noise * 0.18f)).roundToInt().coerceIn(0, 92)
                    canvas.drawCircle(
                        rect.left + rect.width() * localX,
                        rect.top + rect.height() * localY,
                        radius,
                        stampErasePaint,
                    )
                }
            }
        }
        repeat(4) { index ->
            val noise = stableNoise(stamp.ordinal + 41, index, 3)
            stampErasePaint.alpha = (alpha * (0.07f + noise * 0.11f)).roundToInt().coerceIn(0, 58)
            val y = rect.top + rect.height() * (0.18f + index * 0.18f + noise * 0.04f)
            val x = rect.left + rect.width() * (0.18f + stableNoise(stamp.ordinal + 53, index, 5) * 0.32f)
            val scratchWidth = rect.width() * (0.18f + stableNoise(stamp.ordinal + 59, index, 7) * 0.18f)
            val scratchHeight = dp(0.55f + noise * 0.55f)
            canvas.drawRoundRect(
                RectF(x, y, x + scratchWidth, y + scratchHeight),
                scratchHeight,
                scratchHeight,
                stampErasePaint,
            )
        }
        stampErasePaint.alpha = 255
    }

    private fun stableNoise(seed: Int, x: Int, y: Int): Float {
        var value = seed * 7349 + x * 9157 + y * 6151
        value = value xor (value shl 13)
        val mixed = value * (value * value * 15731 + 789221) + 1376312589
        return ((mixed and 0x7fffffff) / 2147483647f).coerceIn(0f, 1f)
    }

    private fun stampTextureFor(stamp: MemorialStamp): Bitmap? {
        return when (stamp) {
            MemorialStamp.Approve,
            MemorialStamp.Keep,
            MemorialStamp.Like -> stampLikeTexture
            MemorialStamp.Reject,
            MemorialStamp.Dislike -> stampDislikeTexture
            MemorialStamp.Collapse -> null
        }
    }

    private fun buildCinnabarStampTexture(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val input = IntArray(width * height)
        val output = IntArray(width * height)
        source.getPixels(input, 0, width, 0, 0, width, height)
        for (index in input.indices) {
            val color = input[index]
            val red = AndroidColor.red(color)
            val green = AndroidColor.green(color)
            val blue = AndroidColor.blue(color)
            val luminance = (red * 0.299f + green * 0.587f + blue * 0.114f)
            val maskAlpha = ((luminance - 8f) / 247f * 255f).roundToInt().coerceIn(0, 255)
            output[index] = if (maskAlpha <= 2) {
                AndroidColor.TRANSPARENT
            } else {
                AndroidColor.argb(maskAlpha, AndroidColor.red(STAMP_RED), AndroidColor.green(STAMP_RED), AndroidColor.blue(STAMP_RED))
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun rebuildLayout(viewWidth: Int, viewHeight: Int) {
        if (viewWidth <= 0 || viewHeight <= 0) return

        foldLeft = dp(20f)
        foldRight = viewWidth - dp(20f)
        foldTop = dp(20f)
        foldBottom = viewHeight - dp(20f)

        val viewportWidth = (foldRight - foldLeft).coerceAtLeast(1f)
        val viewportHeight = (foldBottom - foldTop).coerceAtLeast(1f)
        articleWidth = if (viewportWidth >= dp(700f)) {
            (viewportWidth / 2f).coerceAtMost(dp(600f))
        } else {
            viewportWidth.coerceAtMost(dp(600f))
        }.coerceAtLeast(dp(280f))
        val articleHeight = min(dp(760f), viewportHeight * 0.78f).coerceAtLeast(dp(520f))
        val articleTop = foldTop + (viewportHeight - articleHeight) / 2f
        val contentWidth = (articleWidth - dp(80f)).roundToInt().coerceAtLeast(1)
        var nextLeft = 0f
        articles = pages.mapIndexed { index, page ->
            ArticleLayout(
                page = page,
                pageIndex = index,
                left = nextLeft,
                top = articleTop,
                width = articleWidth,
                height = articleHeight,
            ).also {
                nextLeft += articleWidth
            }
        }

        maxScrollX = max(0f, nextLeft - viewportWidth)
        foldScrollX = foldScrollX.coerceIn(0f, maxScrollX)
    }

    private fun calculateTransform(
        screenLeft: Float,
        actualWidth: Float,
        viewportWidth: Float,
    ): FoldTransform {
        val screenRight = screenLeft + actualWidth
        val overlapStart = max(foldLeft, screenLeft)
        val overlapEnd = min(foldRight, screenRight)

        if (overlapStart >= overlapEnd) {
            return FoldTransform(
                rotationY = 0f,
                pivotX = screenLeft,
                shadingAlpha = 0f,
                edgeShadowProgress = 0f,
                visible = false,
            )
        }

        val overlap = overlapEnd - overlapStart
        val visibleFraction = (overlap / actualWidth).coerceIn(0f, 1f)
        var rotation = 0f
        var shade = 0f
        val itemIsBeforeViewport = screenLeft < foldLeft
        val itemIsAfterViewport = screenRight > foldRight
        val pivotX = if (itemIsBeforeViewport) {
            screenLeft + actualWidth
        } else {
            screenLeft
        }

        if (visibleFraction < 1f && (itemIsBeforeViewport || itemIsAfterViewport)) {
            val direction = if (itemIsBeforeViewport) -1f else 1f
            val hiddenFraction = (1f - visibleFraction).coerceIn(0f, 1f)
            val easedHiddenFraction = foldProgress(hiddenFraction)
            val radians = easedHiddenFraction * (PI.toFloat() / 2f) * direction

            if (abs(radians) > PI.toFloat() / 2f) {
                return FoldTransform(
                    rotationY = 0f,
                    pivotX = pivotX,
                    shadingAlpha = 0f,
                    edgeShadowProgress = 0f,
                    visible = false,
                )
            }

            rotation = radians * 180f / PI.toFloat()
            shade = easedHiddenFraction
        }
        val centerX = foldLeft + viewportWidth / 2f
        val normalizedDistance = (abs(pivotX - centerX) / max(1f, viewportWidth / 2f)).coerceIn(0f, 1f)
        val edgeAcceleration = smoothStep(0.34f, 0.88f, normalizedDistance)
        val edgeShadowProgress = (shade * (0.18f + 0.82f * edgeAcceleration * edgeAcceleration))
            .coerceIn(0f, 1f)

        return FoldTransform(
            rotationY = rotation,
            pivotX = pivotX,
            shadingAlpha = shade,
            edgeShadowProgress = edgeShadowProgress,
            visible = true,
        )
    }

    private fun drawReadingState(canvas: Canvas, viewportWidth: Float) {
        for (article in articles) {
            val screenLeft = foldLeft + article.left - foldScrollX
            val transform = calculateTransform(
                screenLeft = screenLeft,
                actualWidth = article.width,
                viewportWidth = viewportWidth,
            )
            if (transform.visible) {
                canvas.save()
                if (transform.rotationY != 0f) {
                    applyFoldMatrix(
                        canvas = canvas,
                        rotationY = transform.rotationY,
                        pivotX = transform.pivotX,
                    )
                }
                drawArticle(
                    canvas = canvas,
                    article = article,
                    left = screenLeft,
                    transform = transform,
                )
                canvas.restore()
            }
        }
    }

    private fun drawOpeningAnimation(canvas: Canvas, viewportWidth: Float) {
        if (articles.isEmpty()) return

        val placements = buildOpeningPlacements()

        for (placement in placements.asReversed()) {
            val article = placement.article
            val panelRect = RectF(
                placement.left,
                article.top,
                placement.left + article.width,
                article.top + article.height,
            )
            val pivotsOnLeft = article.pageIndex % 2 == 1

            canvas.save()
            if (placement.rotationY != 0f) {
                applyFoldMatrix(
                    canvas = canvas,
                    rotationY = placement.rotationY,
                    pivotX = placement.pivotX,
                )
            }
            canvas.clipRect(panelRect)
            drawArticle(
                canvas = canvas,
                article = article,
                left = placement.left,
                transform = FoldTransform(
                    rotationY = placement.rotationY,
                    pivotX = placement.pivotX,
                    shadingAlpha = 0.1f * placement.foldAmount,
                    edgeShadowProgress = 0.48f * placement.foldAmount,
                    visible = true,
                ),
            )
            drawOpeningSegmentTone(canvas, panelRect, placement.foldAmount, !pivotsOnLeft)
            canvas.restore()
        }
    }

    private fun buildOpeningPlacements(): List<OpeningPlacement> {
        if (articles.isEmpty()) return emptyList()
        val placements = ArrayList<OpeningPlacement>(articles.size)
        var chainEdgeX = openingBaseLeft()

        articles.forEachIndexed { index, article ->
            val rotation = openingRotationFor(index)
            val pivotX = chainEdgeX
            val left = chainEdgeX
            placements += OpeningPlacement(
                article = article,
                left = left,
                pivotX = pivotX,
                rotationY = rotation,
                foldAmount = if (index == 0) 0f else abs(rotation) / 180f,
            )
            chainEdgeX = if (rotation == 0f) {
                left + article.width
            } else {
                pivotX + article.width * cos(rotation * PI.toFloat() / 180f)
            }
        }
        return placements
    }

    private fun openingRotationFor(pageIndex: Int): Float {
        if (pageIndex == 0) return 0f
        if (pageIndex % 2 == 0) return 0f
        val waveDelay = 0.035f * (pageIndex - 1)
        val localProgress = openingSegmentProgress(openProgress, waveDelay)
        val rotationMagnitude = 180f * (1f - localProgress)
        return -rotationMagnitude
    }

    private fun applyFoldMatrix(
        canvas: Canvas,
        rotationY: Float,
        pivotX: Float,
    ) {
        val pivotY = foldTop + (foldBottom - foldTop) / 2f
        matrix.reset()
        camera.save()
        camera.setLocation(0f, 0f, -dp(900f) / displayDensity)
        camera.rotateY(rotationY)
        camera.getMatrix(matrix)
        camera.restore()
        matrix.preTranslate(-pivotX, -pivotY)
        matrix.postTranslate(pivotX, pivotY)
        canvas.concat(matrix)
    }

    private fun drawArticle(
        canvas: Canvas,
        article: ArticleLayout,
        left: Float,
        coverSequenceIndex: Int = coverStackIndex,
        transform: FoldTransform,
    ) {
        articleRect.set(left, article.top, left + article.width, article.top + article.height)
        canvas.drawRect(articleRect, articlePaint)
        if (article.page.type == MemorialPageType.Cover) {
            drawCoverBackground(canvas, articleRect, coverSequenceIndex)
        } else {
            drawPaperBackground(canvas, articleRect, rotated = article.pageIndex % 2 == 1)
        }
        drawPaperAging(canvas, articleRect, article.pageIndex)
        drawArticleSurface(canvas, articleRect)
        drawPageContent(canvas, article, articleRect)
        drawRotationalVolumeShadow(
            canvas = canvas,
            rect = articleRect,
            pivotX = transform.pivotX,
            rotationY = transform.rotationY,
            progress = transform.edgeShadowProgress,
        )
        drawDynamicFoldShadow(canvas, articleRect, transform.pivotX, transform.edgeShadowProgress)
        drawFoldEdgeThickness(
            canvas = canvas,
            rect = articleRect,
            pivotX = transform.pivotX,
            progress = max(abs(transform.rotationY) / 90f, transform.edgeShadowProgress * 0.55f),
        )

        if (transform.shadingAlpha > 0f) {
            shadePaint.alpha = (255f * 0.08f * transform.shadingAlpha).roundToInt().coerceIn(0, 255)
            canvas.drawRect(articleRect, shadePaint)
            shadePaint.alpha = 255
        }
    }

    private fun drawCoverBackground(canvas: Canvas, rect: RectF, coverSequenceIndex: Int) {
        coverTextureFor(coverSequenceIndex)?.let { texture ->
            canvas.drawBitmap(texture, null, rect, coverPaint)
        } ?: run {
            articlePaint.color = AndroidColor.rgb(122, 62, 42)
            canvas.drawRect(rect, articlePaint)
            articlePaint.color = AndroidColor.WHITE
        }
        surfacePaint.shader = LinearGradient(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            intArrayOf(
                AndroidColor.argb(40, 255, 255, 255),
                AndroidColor.TRANSPARENT,
                AndroidColor.argb(70, 0, 0, 0),
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(rect, surfacePaint)
        surfacePaint.shader = null
    }

    private fun coverTextureFor(coverSequenceIndex: Int): Bitmap? {
        return if (coverTextures.isNotEmpty()) {
            coverTextures[positiveModulo(coverSequenceIndex, coverTextures.size)] ?: fallbackCoverTexture
        } else {
            fallbackCoverTexture
        }
    }

    private fun positiveModulo(value: Int, modulo: Int): Int {
        val remainder = value % modulo
        return if (remainder >= 0) remainder else remainder + modulo
    }

    private fun drawPaperBackground(canvas: Canvas, rect: RectF, rotated: Boolean) {
        paperTexture?.let { texture ->
            canvas.save()
            if (rotated) {
                canvas.rotate(180f, rect.centerX(), rect.centerY())
            }
            canvas.drawBitmap(texture, null, rect, paperPaint)
            canvas.restore()
        }
    }

    private fun drawPaperPanel(
        canvas: Canvas,
        rect: RectF,
        pageIndex: Int,
        clippedCircle: Boolean,
    ) {
        canvas.save()
        if (clippedCircle) {
            paperClipPath.reset()
            paperClipPath.addOval(rect, Path.Direction.CW)
            canvas.clipPath(paperClipPath)
        }
        articlePaint.color = STAMP_PAPER
        canvas.drawRect(rect, articlePaint)
        drawPaperBackground(canvas, rect, rotated = pageIndex % 2 == 1)
        drawPaperAging(canvas, rect, pageIndex)
        drawArticleSurface(canvas, rect)
        articlePaint.color = AndroidColor.WHITE
        canvas.restore()
    }

    private fun drawPaperAging(canvas: Canvas, rect: RectF, pageIndex: Int) {
        paperAgingPaint.shader = LinearGradient(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            intArrayOf(
                AndroidColor.argb(18, 124, 88, 34),
                AndroidColor.TRANSPARENT,
                AndroidColor.argb(15, 88, 57, 26),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(rect, paperAgingPaint)
        paperAgingPaint.shader = null

        paperAgingPaint.shader = LinearGradient(
            rect.left,
            0f,
            rect.right,
            0f,
            intArrayOf(
                AndroidColor.argb(20, 76, 48, 20),
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT,
                AndroidColor.argb(18, 76, 48, 20),
            ),
            floatArrayOf(0f, 0.09f, 0.9f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(rect, paperAgingPaint)
        paperAgingPaint.shader = null

        val random = Random(pageIndex * 1103515245 + 12345)
        repeat(26) {
            val radius = dp(random.nextInt(1, 4).toFloat()) * 0.45f
            val x = rect.left + random.nextFloat() * rect.width()
            val y = rect.top + random.nextFloat() * rect.height()
            val alpha = random.nextInt(5, 13)
            paperAgingPaint.color = AndroidColor.argb(alpha, 70, 46, 22)
            canvas.drawCircle(x, y, radius, paperAgingPaint)
        }
        paperAgingPaint.color = AndroidColor.TRANSPARENT
    }

    private fun drawDynamicFoldShadow(
        canvas: Canvas,
        rect: RectF,
        pivotX: Float,
        progress: Float,
    ) {
        if (progress <= 0f) return

        val clampedPivotX = pivotX.coerceIn(rect.left, rect.right)
        val overflow = when {
            pivotX < rect.left -> rect.left - pivotX
            pivotX > rect.right -> pivotX - rect.right
            else -> 0f
        }
        val edgeFade = (1f - overflow / dp(42f)).coerceIn(0f, 1f)
        if (edgeFade <= 0f) return
        val easedProgress = progress * progress * (3f - 2f * progress)
        val effectiveProgress = easedProgress * edgeFade
        val shadowWidth = dp(24f) + dp(42f) * easedProgress
        val peakAlpha = (5f + 22f * effectiveProgress).roundToInt().coerceIn(0, 40)
        val shadowLeft: Float
        val shadowRight: Float
        val colors: IntArray

        if (clampedPivotX <= rect.centerX()) {
            shadowLeft = clampedPivotX
            shadowRight = min(rect.right, clampedPivotX + shadowWidth)
            colors = intArrayOf(
                AndroidColor.argb(peakAlpha, 72, 48, 26),
                AndroidColor.argb((peakAlpha * 0.34f).roundToInt(), 90, 68, 38),
                AndroidColor.TRANSPARENT,
            )
        } else {
            shadowLeft = max(rect.left, clampedPivotX - shadowWidth)
            shadowRight = clampedPivotX
            colors = intArrayOf(
                AndroidColor.TRANSPARENT,
                AndroidColor.argb((peakAlpha * 0.34f).roundToInt(), 90, 68, 38),
                AndroidColor.argb(peakAlpha, 72, 48, 26),
            )
        }

        if (shadowRight <= shadowLeft) return

        foldShadowPaint.shader = LinearGradient(
            shadowLeft,
            0f,
            shadowRight,
            0f,
            colors,
            floatArrayOf(0f, 0.38f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(shadowLeft, rect.top, shadowRight, rect.bottom, foldShadowPaint)
        foldShadowPaint.shader = null
    }

    private fun drawFoldEdgeThickness(
        canvas: Canvas,
        rect: RectF,
        pivotX: Float,
        progress: Float,
    ) {
        val t = progress.coerceIn(0f, 1f)
        if (t <= 0.02f || pivotX < rect.left - dp(2f) || pivotX > rect.right + dp(2f)) return

        val edgeWidth = dp(1.2f) + dp(3.2f) * t
        val edgeLeft: Float
        val edgeRight: Float
        val colors: IntArray
        if (pivotX <= rect.centerX()) {
            edgeLeft = pivotX
            edgeRight = min(rect.right, pivotX + edgeWidth)
            colors = intArrayOf(
                AndroidColor.argb((30f * t).roundToInt(), 94, 65, 35),
                AndroidColor.argb((18f * t).roundToInt(), 210, 181, 124),
                AndroidColor.TRANSPARENT,
            )
        } else {
            edgeLeft = max(rect.left, pivotX - edgeWidth)
            edgeRight = pivotX
            colors = intArrayOf(
                AndroidColor.TRANSPARENT,
                AndroidColor.argb((18f * t).roundToInt(), 210, 181, 124),
                AndroidColor.argb((30f * t).roundToInt(), 94, 65, 35),
            )
        }
        if (edgeRight <= edgeLeft) return

        foldShadowPaint.shader = LinearGradient(
            edgeLeft,
            0f,
            edgeRight,
            0f,
            colors,
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(edgeLeft, rect.top, edgeRight, rect.bottom, foldShadowPaint)
        foldShadowPaint.shader = null

        // Keep the fold hint extremely subtle; the paper texture and shading should carry the volume.
    }

    private fun drawRotationalVolumeShadow(
        canvas: Canvas,
        rect: RectF,
        pivotX: Float,
        rotationY: Float,
        progress: Float,
    ) {
        val raw = max(abs(rotationY) / 90f, progress).coerceIn(0f, 1f)
        val t = raw * raw * (3f - 2f * raw)
        if (t <= 0.02f) return

        val hingeOnLeft = pivotX <= rect.centerX()
        val darkAlpha = (6f + 28f * t).roundToInt().coerceIn(0, 52)
        val midAlpha = (4f + 14f * t).roundToInt().coerceIn(0, 28)
        val lightAlpha = (4f + 12f * t).roundToInt().coerceIn(0, 24)
        val colors = if (hingeOnLeft) {
            intArrayOf(
                AndroidColor.argb(darkAlpha, 0, 0, 0),
                AndroidColor.argb(midAlpha, 0, 0, 0),
                AndroidColor.TRANSPARENT,
                AndroidColor.argb(lightAlpha, 255, 255, 255),
            )
        } else {
            intArrayOf(
                AndroidColor.argb(lightAlpha, 255, 255, 255),
                AndroidColor.TRANSPARENT,
                AndroidColor.argb(midAlpha, 0, 0, 0),
                AndroidColor.argb(darkAlpha, 0, 0, 0),
            )
        }

        surfacePaint.shader = LinearGradient(
            rect.left,
            0f,
            rect.right,
            0f,
            colors,
            floatArrayOf(0f, 0.22f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(rect, surfacePaint)
        surfacePaint.shader = null
    }

    private fun drawPageContent(canvas: Canvas, article: ArticleLayout, rect: RectF) {
        when (article.page.type) {
            MemorialPageType.Cover -> drawCoverContent(canvas, rect, article.page.pageNumber)
            MemorialPageType.Directory -> drawDirectoryContent(canvas, rect, article.page.pageNumber)
            MemorialPageType.BodyLeft -> drawBodyLeftContent(canvas, rect, article.page.pageNumber)
            MemorialPageType.BodyRight -> drawBodyRightContent(canvas, rect, article.page.pageNumber)
            MemorialPageType.End -> drawEndContent(canvas, rect, article.page.pageNumber)
        }
    }

    private fun drawCoverContent(canvas: Canvas, rect: RectF, pageNumber: String) {
        val labelWidth = rect.width() * 0.42f
        val labelHeight = rect.height() * 0.58f
        val label = RectF(
            rect.centerX() - labelWidth / 2f,
            rect.centerY() - labelHeight / 2f,
            rect.centerX() + labelWidth / 2f,
            rect.centerY() + labelHeight / 2f,
        )
        drawPaperPanel(
            canvas = canvas,
            rect = label,
            pageIndex = 73,
            clippedCircle = false,
        )
        drawInsideStrokeRect(canvas, label, goldPaint)
        val innerInset = dp(6f)
        val innerLabel = RectF(
            label.left + innerInset,
            label.top + innerInset,
            label.right - innerInset,
            label.bottom - innerInset,
        )
        drawInsideStrokeRect(canvas, innerLabel, goldPaint)
        drawCoverCornerOrnaments(canvas, innerLabel)

        drawVerticalText(
            canvas = canvas,
            text = "奏章",
            centerX = label.centerX(),
            top = label.top + dp(70f),
            paint = coverTitlePaint,
            lineGap = dp(18f),
        )
        drawCenteredText(canvas, "甲辰年冬月", label.centerX(), label.bottom - dp(26f), metaPaint)
        drawPageNumber(canvas, rect, pageNumber)
    }

    private fun drawInsideStrokeRect(canvas: Canvas, rect: RectF, paint: Paint) {
        val strokeInset = paint.strokeWidth / 2f
        val strokeRect = RectF(rect).apply {
            inset(strokeInset, strokeInset)
        }
        canvas.drawRect(strokeRect, paint)
    }

    private fun drawDoubleGoldFrame(canvas: Canvas, rect: RectF, outerInset: Float, innerInset: Float) {
        val outer = RectF(rect).apply {
            inset(outerInset, outerInset)
        }
        val inner = RectF(rect).apply {
            inset(innerInset, innerInset)
        }
        drawInsideStrokeRect(canvas, outer, goldPaint)
        drawInsideStrokeRect(canvas, inner, goldPaint)
    }

    private fun drawCoverCornerOrnaments(
        canvas: Canvas,
        innerLabel: RectF,
        sizeScale: Float = 0.22f,
    ) {
        val texture = coverCornerTexture ?: return
        val cornerSize = min(innerLabel.width(), innerLabel.height()) * sizeScale
        drawCoverCornerOrnament(
            canvas = canvas,
            texture = texture,
            dst = RectF(innerLabel.left, innerLabel.top, innerLabel.left + cornerSize, innerLabel.top + cornerSize),
            rotation = 0f,
        )
        drawCoverCornerOrnament(
            canvas = canvas,
            texture = texture,
            dst = RectF(innerLabel.right - cornerSize, innerLabel.top, innerLabel.right, innerLabel.top + cornerSize),
            rotation = 90f,
        )
        drawCoverCornerOrnament(
            canvas = canvas,
            texture = texture,
            dst = RectF(innerLabel.right - cornerSize, innerLabel.bottom - cornerSize, innerLabel.right, innerLabel.bottom),
            rotation = 180f,
        )
        drawCoverCornerOrnament(
            canvas = canvas,
            texture = texture,
            dst = RectF(innerLabel.left, innerLabel.bottom - cornerSize, innerLabel.left + cornerSize, innerLabel.bottom),
            rotation = 270f,
        )
    }

    private fun drawCoverCornerOrnament(canvas: Canvas, texture: Bitmap, dst: RectF, rotation: Float) {
        canvas.save()
        canvas.rotate(rotation, dst.centerX(), dst.centerY())
        canvas.drawBitmap(texture, null, dst, coverCornerPaint)
        canvas.restore()
    }

    private fun drawDirectoryContent(canvas: Canvas, rect: RectF, pageNumber: String) {
        var y = rect.top + dp(64f)
        drawCenteredText(canvas, "奏报清单", rect.centerX(), y, titlePaint)
        y += dp(30f)
        drawCenteredText(canvas, "恭呈御览 · 共参", rect.centerX(), y, metaPaint)
        y += dp(42f)

        val itemLeft = rect.left + dp(44f)
        val itemWidth = rect.width() - dp(88f)
        directoryItems.forEach { item ->
            drawTextBlock(canvas, item.title, itemTitlePaint, itemLeft, y, itemWidth, 1.35f, false)
            y += dp(30f)
            drawTextBlock(canvas, item.meta, itemMetaPaint, itemLeft, y, itemWidth, 1.35f, false)
            y += dp(28f)
            val excerptLayout = buildTextLayout(item.excerpt, authorPaint, itemWidth.roundToInt(), 1.7f, false)
            canvas.save()
            canvas.translate(itemLeft, y)
            excerptLayout.draw(canvas)
            canvas.restore()
            y += excerptLayout.height + dp(24f)
            canvas.drawLine(itemLeft, y, itemLeft + itemWidth, y, dashedPaint)
            y += dp(24f)
        }
        drawPageNumber(canvas, rect, pageNumber)
    }

    private fun drawBodyLeftContent(canvas: Canvas, rect: RectF, pageNumber: String) {
        var y = rect.top + dp(54f)
        drawCenteredText(canvas, "江南水患赈灾折", rect.centerX(), y, titlePaint)
        y += dp(30f)
        drawCenteredText(canvas, "工部侍郎 张廷玉谨奏", rect.centerX(), y, metaPaint)
        y += dp(42f)

        val left = rect.left + dp(42f)
        val width = rect.width() - dp(84f)
        y = drawParagraph(canvas, "臣张廷玉谨奏：为恭报江南水患灾情，恳恩赈济事。", left, y, width)
        y = drawParagraph(
            canvas,
            "今岁自入夏以来，江南地区秋雨连绵不止，淮水上游水量骤增，致下游各州县河堤多处溃决。据江宁、苏州、扬州各府急报，田亩淹没者逾十万顷，房屋倾圮者不可胜计，灾民流离失所，号泣于道者数十万人。",
            left,
            y,
            width,
        )
        drawParagraph(
            canvas,
            "臣已飞饬地方官吏，先行开仓放赈，安顿老弱。然库存有限，赈米仅敷半月之用。且天寒地冻，灾民衣不蔽体，疾疫恐将蔓延。",
            left,
            y,
            width,
        )
        drawPageNumber(canvas, rect, pageNumber)
    }

    private fun drawBodyRightContent(canvas: Canvas, rect: RectF, pageNumber: String) {
        var y = rect.top + dp(70f)
        val left = rect.left + dp(42f)
        val width = rect.width() - dp(84f)
        y = drawParagraph(
            canvas,
            "伏乞皇上圣鉴，着户部即刻拨银五十万两，粮三十万石，星夜运往灾区。地方官吏如有侵吞克扣者，斩立决。",
            left,
            y,
            width,
            cinnabarPaint,
        )

        val boxTop = y + dp(10f)
        val box = RectF(left, boxTop, left + width, boxTop + dp(134f))
        articlePaint.color = AndroidColor.argb(76, 232, 222, 192)
        canvas.drawRect(box, articlePaint)
        articlePaint.color = AndroidColor.WHITE
        canvas.drawRect(box, goldPaint)
        drawTextBlock(
            canvas,
            "朕览奏殊深轸念。江南百姓，朕之赤子。着即刻依议行，不得延误。钦差大臣即日南下督赈。",
            authorPaint,
            box.left + dp(22f),
            box.top + dp(24f),
            box.width() - dp(44f),
            1.9f,
            false,
        )
        y = box.bottom + dp(46f)
        val signPaint = TextPaint(authorPaint).apply {
            color = AndroidColor.rgb(74, 74, 74)
        }
        drawTextBlock(
            canvas = canvas,
            text = "臣 张廷玉 叩首\n甲辰年冬月 初八日",
            paint = signPaint,
            left = left,
            top = y,
            width = width,
            lineHeightMultiplier = 1.8f,
            justify = false,
            alignment = Layout.Alignment.ALIGN_OPPOSITE,
        )
        drawPageNumber(canvas, rect, pageNumber)
    }

    private fun drawEndContent(canvas: Canvas, rect: RectF, pageNumber: String) {
        val centerY = rect.centerY() - dp(54f)
        val endPaint = TextPaint(titlePaint).apply {
            textSize = sp(42f)
            letterSpacing = 0.16f
        }
        drawCenteredText(canvas, "钦此", rect.centerX(), centerY, endPaint)

        val sealSize = dp(94f)
        val seal = RectF(
            rect.centerX() - sealSize / 2f,
            centerY + dp(40f),
            rect.centerX() + sealSize / 2f,
            centerY + dp(40f) + sealSize,
        )
        canvas.save()
        canvas.rotate(-8f, seal.centerX(), seal.centerY())
        canvas.drawRect(seal, sealPaint)
        drawCenteredText(canvas, "御览", seal.centerX(), seal.centerY() + textCenterOffset(sealTextPaint), sealTextPaint)
        canvas.restore()

        drawCenteredText(canvas, "奏折已览毕", rect.centerX(), seal.bottom + dp(56f), authorPaint)
        drawCenteredText(canvas, "留中不发 / 发抄各部", rect.centerX(), seal.bottom + dp(86f), authorPaint)
        drawPageNumber(canvas, rect, pageNumber)
    }

    private fun drawParagraph(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        width: Float,
        paint: TextPaint = quotePaint,
    ): Float {
        val layout = buildTextLayout("　　$text", paint, width.roundToInt(), 2.0f, true)
        canvas.save()
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height + dp(20f)
    }

    private fun drawTextBlock(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        left: Float,
        top: Float,
        width: Float,
        lineHeightMultiplier: Float,
        justify: Boolean,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    ): Float {
        val layout = buildTextLayout(text, paint, width.roundToInt(), lineHeightMultiplier, justify, alignment)
        canvas.save()
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height
    }

    private fun drawStaticLayout(canvas: Canvas, layout: StaticLayout, left: Float, top: Float) {
        canvas.save()
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawPageNumber(canvas: Canvas, rect: RectF, pageNumber: String) {
        drawCenteredText(canvas, pageNumber, rect.right - dp(26f), rect.bottom - dp(22f), metaPaint)
    }

    private fun drawCenteredText(canvas: Canvas, text: String, x: Float, baseline: Float, paint: TextPaint) {
        canvas.drawText(text, x, baseline, paint)
    }

    private fun drawVerticalText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        top: Float,
        paint: TextPaint,
        lineGap: Float,
    ) {
        var y = top - paint.fontMetrics.ascent
        text.forEach { char ->
            canvas.drawText(char.toString(), centerX, y, paint)
            y += paint.textSize + lineGap
        }
    }

    private fun textCenterOffset(paint: Paint): Float {
        val metrics = paint.fontMetrics
        return -(metrics.ascent + metrics.descent) / 2f
    }

    private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
        val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun foldProgress(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        val smooth = t * t * (3f - 2f * t)
        val moderateEarlyResponse = 1f - (1f - t) * (1f - t)
        return (0.48f * moderateEarlyResponse + 0.52f * smooth).coerceIn(0f, 1f)
    }

    private fun openingSegmentProgress(globalProgress: Float, delay: Float): Float {
        val available = (1f - delay).coerceAtLeast(0.01f)
        val t = ((globalProgress - delay) / available).coerceIn(0f, 1f)
        return 1f - (1f - t) * (1f - t) * (1f - t)
    }

    private fun drawOpeningSegmentTone(
        canvas: Canvas,
        rect: RectF,
        foldAmount: Float,
        pivotsOnRight: Boolean,
    ) {
        if (foldAmount <= 0f) return

        val darkAlpha = (46f * foldAmount).roundToInt().coerceIn(0, 72)
        val lightAlpha = (24f * foldAmount).roundToInt().coerceIn(0, 48)
        val colors = if (pivotsOnRight) {
            intArrayOf(
                AndroidColor.argb(lightAlpha, 255, 255, 255),
                AndroidColor.TRANSPARENT,
                AndroidColor.argb(darkAlpha, 0, 0, 0),
            )
        } else {
            intArrayOf(
                AndroidColor.argb(darkAlpha, 0, 0, 0),
                AndroidColor.TRANSPARENT,
                AndroidColor.argb(lightAlpha, 255, 255, 255),
            )
        }
        surfacePaint.shader = LinearGradient(
            rect.left,
            0f,
            rect.right,
            0f,
            colors,
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(rect, surfacePaint)
        surfacePaint.shader = null
    }

    private fun drawArticleSurface(canvas: Canvas, rect: RectF) {
        surfacePaint.shader = LinearGradient(
            rect.left,
            0f,
            rect.right,
            0f,
            intArrayOf(
                AndroidColor.argb(6, 0, 0, 0),
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT,
                AndroidColor.argb(9, 0, 0, 0),
            ),
            floatArrayOf(0f, 0.08f, 0.92f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(rect, surfacePaint)
        surfacePaint.shader = null

        surfacePaint.shader = LinearGradient(
            rect.right - dp(18f),
            0f,
            rect.right,
            0f,
            intArrayOf(
                AndroidColor.TRANSPARENT,
                AndroidColor.argb(12, 0, 0, 0),
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(rect.right - dp(18f), rect.top, rect.right, rect.bottom, surfacePaint)
        surfacePaint.shader = null

        surfacePaint.shader = LinearGradient(
            rect.left,
            0f,
            rect.left + dp(12f),
            0f,
            intArrayOf(
                AndroidColor.argb(8, 255, 255, 255),
                AndroidColor.TRANSPARENT,
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(rect.left, rect.top, rect.left + dp(12f), rect.bottom, surfacePaint)
        surfacePaint.shader = null

        creasePaint.alpha = 18
        canvas.drawLine(rect.left, rect.top + dp(10f), rect.left, rect.bottom - dp(10f), creasePaint)
        canvas.drawLine(rect.right - 1f, rect.top + dp(10f), rect.right - 1f, rect.bottom - dp(10f), creasePaint)
        creasePaint.alpha = 255
        canvas.drawLine(rect.left, rect.bottom - 1f, rect.right, rect.bottom - 1f, dashedPaint)
    }

    private fun scrollByAmount(delta: Float) {
        if (openProgress < 1f) return
        val next = (foldScrollX + delta).coerceIn(0f, maxScrollX)
        if (next != foldScrollX) {
            foldScrollX = next
            invalidate()
        }
    }

    private fun snapToNearestSpread() {
        if (articleWidth <= 0f || maxScrollX <= 0f) return
        val target = (foldScrollX / articleWidth)
            .roundToInt()
            .times(articleWidth)
            .coerceIn(0f, maxScrollX)
        if (abs(target - foldScrollX) < 1f) {
            foldScrollX = target
            invalidate()
            return
        }
        scrollReturnAnimator?.cancel()
        val start = foldScrollX
        scrollReturnAnimator = ValueAnimator.ofFloat(start, target).apply {
            duration = ((180L + abs(target - start) / max(1f, articleWidth) * 150L).roundToInt())
                .coerceIn(180, 360)
                .toLong()
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener { animator ->
                foldScrollX = (animator.animatedValue as Float).coerceIn(0f, maxScrollX)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (scrollReturnAnimator == animation) {
                        scrollReturnAnimator = null
                    }
                    foldScrollX = target
                    invalidate()
                }
            })
            start()
        }
    }

    private fun buildTextLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        lineHeightMultiplier: Float,
        justify: Boolean,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    ): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setLineSpacing(0f, lineHeightMultiplier)
            .setIncludePad(false)
            .apply {
                if (justify) {
                    setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
                }
            }
            .build()
    }

    private fun buildEllipsizedTextLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        lineHeightMultiplier: Float,
        maxLines: Int,
    ): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, lineHeightMultiplier)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
    }

    private fun dp(value: Float): Float = value * displayDensity

    private fun sp(value: Float): Float = value * scaledDensity

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction.coerceIn(0f, 1f)
    }

    private fun startOpenAnimationIfReady() {
        if (hasPlayedOpenAnimation || width <= 0 || height <= 0 || articles.isEmpty()) return

        hasPlayedOpenAnimation = true
        stage = MemorialStage.CoverOnly
        foldScrollX = 0f
        openProgress = 0f
        invalidate()
    }

    private fun expandFromCover() {
        if (stage != MemorialStage.CoverOnly) return
        hasPlayedOpenAnimation = true
        hideReadingControlsDuringClose = false
        stage = MemorialStage.Opening
        foldScrollX = 0f
        animateOpenProgress(
            from = 0f,
            to = 1f,
            durationMs = MEMORIAL_OPEN_CLOSE_DURATION_MS,
            onFinished = {
                stage = MemorialStage.Expanded
                openProgress = 1f
                invalidate()
            },
        )
    }

    private fun startCoverVerdict(
        stamp: MemorialStamp,
        dx: Float,
        verticalDirection: Float,
        fromButton: Boolean = false,
    ) {
        val initialStampStrength = coverPreviewStampStrength.coerceIn(0f, 1f)
        coverPreviewStamp = stamp
        coverPreviewStampStrength = initialStampStrength
        coverPreviewStampTargetStrength = initialStampStrength
        coverFinalStamp = stamp
        coverFinalStampFromButton = fromButton
        coverFinalStampStrength = initialStampStrength
        coverStackLiftProgress = 0f
        val startX = coverDragX
        val startY = coverDragY
        val targetX = when {
            verticalDirection < 0f -> startX
            dx >= 0f -> width.toFloat() + articleWidth
            else -> -width.toFloat() - articleWidth
        }
        val targetY = if (verticalDirection < 0f) {
            -height.toFloat() - dp(80f)
        } else {
            startY
        }
        coverSwipeAnimator?.cancel()
        coverSwipeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = COVER_VERDICT_DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                val stampT = lerp(initialStampStrength, 1f, smoothStep(0f, 0.333f, t))
                val moveT = smoothStep(0.333f, 1f, t)
                coverFinalStampStrength = stampT
                coverPreviewStampStrength = stampT
                coverPreviewStampTargetStrength = stampT
                coverStackLiftProgress = 0f
                coverDragX = lerp(startX, targetX, moveT)
                coverDragY = lerp(startY, targetY, moveT)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (coverSwipeAnimator == animation) {
                        coverSwipeAnimator = null
                    }
                    if (!canceled) {
                        coverFinalStamp = null
                        coverFinalStampFromButton = false
                        coverFinalStampStrength = 0f
                        coverPreviewStampStrength = 0f
                        coverPreviewStampTargetStrength = 0f
                        advanceAfterVerdict()
                    }
                }
            })
            start()
        }
    }

    private fun triggerCoverAction(stamp: MemorialStamp) {
        when (stamp) {
            MemorialStamp.Reject -> {
                coverDragX = 0f
                coverDragY = 0f
                coverPreviewStamp = stamp
                coverPreviewStampStrength = 0f
                coverPreviewStampTargetStrength = 0f
                startCoverVerdict(stamp, dx = -dp(96f), verticalDirection = 0f, fromButton = true)
            }
            MemorialStamp.Approve -> {
                coverDragX = 0f
                coverDragY = 0f
                coverPreviewStamp = stamp
                coverPreviewStampStrength = 0f
                coverPreviewStampTargetStrength = 0f
                startCoverVerdict(stamp, dx = dp(96f), verticalDirection = 0f, fromButton = true)
            }
            MemorialStamp.Keep -> {
                coverDragX = 0f
                coverDragY = 0f
                coverPreviewStamp = stamp
                coverPreviewStampStrength = 0f
                coverPreviewStampTargetStrength = 0f
                startCoverVerdict(stamp, dx = 0f, verticalDirection = -1f, fromButton = true)
            }
            else -> Unit
        }
    }

    private fun animateCoverBack() {
        val startX = coverDragX
        val startY = coverDragY
        coverPreviewStamp = null
        coverPreviewStampStrength = 0f
        coverPreviewStampTargetStrength = 0f
        coverSwipeAnimator?.cancel()
        coverSwipeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260L
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                coverDragX = lerp(startX, 0f, t)
                coverDragY = lerp(startY, 0f, t)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (coverSwipeAnimator == animation) {
                        coverSwipeAnimator = null
                    }
                    coverDragX = 0f
                    coverDragY = 0f
                    coverFinalStamp = null
                    coverFinalStampFromButton = false
                    coverFinalStampStrength = 0f
                    invalidate()
                }
            })
            start()
        }
    }

    private fun showNextCoverFromStack() {
        stage = MemorialStage.CoverOnly
        openProgress = 0f
        foldScrollX = 0f
        coverStackIndex += 1
        coverDragX = 0f
        coverDragY = 0f
        coverPreviewStamp = null
        coverPreviewStampStrength = 0f
        coverPreviewStampTargetStrength = 0f
        coverFinalStamp = null
        coverFinalStampFromButton = false
        coverFinalStampStrength = 0f
        hideSummary(animated = false)
        summaryPinnedByTap = false
        summaryPinnedAtDown = false
        summaryShownByHold = false
        removeCallbacks(showSummaryRunnable)
        coverSwipeAnimator?.cancel()
        coverSwipeAnimator = null
        coverStackAnimator?.cancel()
        coverStackLiftProgress = 0f
        coverStackAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = COVER_SWITCH_DURATION_MS
            interpolator = PathInterpolator(0.24f, 0f, 0.18f, 1f)
            addUpdateListener { animator ->
                coverStackLiftProgress = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (coverStackAnimator == animation) {
                        coverStackAnimator = null
                    }
                    if (!canceled) {
                        coverStackLiftProgress = 1f
                        invalidate()
                    }
                }
            })
            start()
        }
    }

    private fun advanceAfterVerdict() {
        removeCallbacks(showSummaryRunnable)
        hideSummary(animated = false)
        summaryPinnedByTap = false
        summaryPinnedAtDown = false
        summaryShownByHold = false
        if (coverStackIndex >= TOTAL_PENDING_MEMORIALS - 1) {
            coverStackIndex = TOTAL_PENDING_MEMORIALS
            coverDragX = 0f
            coverDragY = 0f
            coverPreviewStamp = null
            coverFinalStamp = null
            coverFinalStampFromButton = false
            coverFinalStampStrength = 0f
            currentStamp = null
            stampProgress = 0f
            openProgress = 0f
            foldScrollX = 0f
            showCompletedState()
        } else {
            showNextCoverFromStack()
        }
    }

    private fun showCompletedState() {
        completedAnimator?.cancel()
        completedAlpha = 0f
        stage = MemorialStage.Completed
        completedAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 680L
            interpolator = PathInterpolator(0.18f, 0f, 0f, 1f)
            addUpdateListener { animator ->
                completedAlpha = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (completedAnimator == animation) {
                        completedAnimator = null
                    }
                    completedAlpha = 1f
                    invalidate()
                }
            })
            start()
        }
    }

    private fun dismissCompletedState() {
        if (stage != MemorialStage.Completed) {
            onAutoDismiss?.invoke()
            return
        }
        completedAnimator?.cancel()
        val startAlpha = completedAlpha.coerceIn(0f, 1f)
        completedAnimator = ValueAnimator.ofFloat(startAlpha, 0f).apply {
            duration = 360L
            interpolator = PathInterpolator(0.3f, 0f, 0.8f, 1f)
            addUpdateListener { animator ->
                completedAlpha = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (completedAnimator == animation) {
                        completedAnimator = null
                    }
                    if (!canceled) {
                        completedAlpha = 0f
                        onAutoDismiss?.invoke()
                    }
                }
            })
            start()
        }
    }

    private fun startStampAndDismiss(stamp: MemorialStamp) {
        if (currentStamp != null) return
        returnToFirstSpread {
            startStamp(stamp, StampCompletion.ResetCover)
        }
    }

    private fun startStamp(stamp: MemorialStamp, completion: StampCompletion) {
        if (currentStamp != null) return
        stampAnimator?.cancel()
        currentStamp = stamp
        stampCompletion = completion
        stampProgress = 0f
        stampAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 620L
            interpolator = PathInterpolator(0.16f, 0f, 0f, 1f)
            addUpdateListener { animator ->
                stampProgress = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (stampAnimator == animation) {
                        stampAnimator = null
                    }
                    if (!canceled) {
                        when (stampCompletion) {
                            StampCompletion.AutoDismiss -> postDelayed({
                                onAutoDismiss?.invoke()
                            }, 170L)

                            StampCompletion.ResetCover -> postDelayed({
                                currentStamp = null
                                stampProgress = 0f
                                coverPreviewStamp = null
                                returnToCoverStack(advanceStack = true)
                            }, 130L)
                        }
                    }
                }
            })
            start()
        }
    }

    private fun averagePointerX(event: MotionEvent): Float {
        var sum = 0f
        for (index in 0 until event.pointerCount) {
            sum += event.getX(index)
        }
        return sum / event.pointerCount.coerceAtLeast(1)
    }

    private fun averagePointerY(event: MotionEvent): Float {
        var sum = 0f
        for (index in 0 until event.pointerCount) {
            sum += event.getY(index)
        }
        return sum / event.pointerCount.coerceAtLeast(1)
    }

    fun collapseToStack() {
        returnToCoverStack(advanceStack = false)
    }

    private fun returnToCoverStack(advanceStack: Boolean) {
        if (articles.isEmpty()) return
        hideReadingControlsDuringClose = advanceStack
        val finishReturn = {
            if (advanceStack) {
                advanceAfterVerdict()
            } else {
                hideReadingControlsDuringClose = false
                stage = MemorialStage.CoverOnly
                openProgress = 0f
                foldScrollX = 0f
                coverDragX = 0f
                coverDragY = 0f
                coverPreviewStamp = null
                coverStackLiftProgress = 1f
                coverFinalStamp = null
                coverFinalStampFromButton = false
                coverFinalStampStrength = 0f
                invalidate()
            }
        }
        if (stage == MemorialStage.CoverOnly || openProgress <= 0f) {
            finishReturn()
            hideReadingControlsDuringClose = false
            return
        }
        closeWithAnimation {
            finishReturn()
            hideReadingControlsDuringClose = false
        }
    }

    fun closeWithAnimation(onFinished: () -> Unit) {
        if (articles.isEmpty()) {
            onFinished()
            return
        }

        scroller.abortAnimation()
        isDragging = false
        if (stage == MemorialStage.CoverOnly) {
            onFinished()
            return
        }
        val startScrollX = foldScrollX.coerceIn(0f, maxScrollX)
        if (startScrollX > 1f) {
            returnToFirstSpread {
                stage = MemorialStage.Closing
                animateOpenProgress(
                    from = openProgress.coerceIn(0f, 1f),
                    to = 0f,
                    durationMs = MEMORIAL_OPEN_CLOSE_DURATION_MS,
                    onFinished = onFinished,
                )
            }
            return
        }
        foldScrollX = 0f
        stage = MemorialStage.Closing
        animateOpenProgress(
            from = openProgress.coerceIn(0f, 1f),
            to = 0f,
            durationMs = MEMORIAL_OPEN_CLOSE_DURATION_MS,
            onFinished = onFinished,
        )
    }

    private fun returnToFirstSpread(onFinished: () -> Unit) {
        val startScrollX = foldScrollX.coerceIn(0f, maxScrollX)
        if (startScrollX <= 1f) {
            foldScrollX = 0f
            invalidate()
            onFinished()
            return
        }

        scroller.abortAnimation()
        pendingSpreadSnap = false
        scrollReturnAnimator?.cancel()
        scrollReturnAnimator = ValueAnimator.ofFloat(startScrollX, 0f).apply {
            duration = ((360L + startScrollX / max(1f, articleWidth) * 170L).roundToInt())
                .coerceIn(420, 760)
                .toLong()
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener { animator ->
                foldScrollX = (animator.animatedValue as Float).coerceIn(0f, maxScrollX)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (scrollReturnAnimator == animation) {
                        scrollReturnAnimator = null
                    }
                    if (!canceled) {
                        foldScrollX = 0f
                        invalidate()
                        onFinished()
                    }
                }
            })
            start()
        }
    }

    private fun animateOpenProgress(
        from: Float,
        to: Float,
        durationMs: Long,
        onFinished: (() -> Unit)?,
    ) {
        transitionAnimator?.cancel()
        openProgress = from.coerceIn(0f, 1f)
        transitionAnimator = ValueAnimator.ofFloat(openProgress, to.coerceIn(0f, 1f)).apply {
            duration = durationMs
            interpolator = PathInterpolator(0.42f, 0f, 0.58f, 1f)
            addUpdateListener { animator ->
                openProgress = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (transitionAnimator == animation) {
                        transitionAnimator = null
                    }
                    if (!canceled) {
                        openProgress = to.coerceIn(0f, 1f)
                        invalidate()
                        onFinished?.invoke()
                    }
                }
            })
            start()
        }
    }
}
