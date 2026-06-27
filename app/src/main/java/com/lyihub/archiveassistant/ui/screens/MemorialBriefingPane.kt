package com.lyihub.archiveassistant.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyihub.archiveassistant.R
import com.lyihub.archiveassistant.ui.theme.ImperialBronze
import com.lyihub.archiveassistant.ui.theme.ImperialCinnabar
import com.lyihub.archiveassistant.ui.theme.ImperialIvory
import com.lyihub.archiveassistant.ui.theme.ImperialParchment
import com.lyihub.archiveassistant.ui.theme.ImperialUmber
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MemorialCoverAspect = 10f / 22f
private const val MemorialWheelItemCount = 24
private const val MemorialActiveSlotDegrees = 225f
private const val MemorialWheelDragDegreesPerPixel = -0.18f
private const val MemorialWheelActiveScale = 1.58f
private const val MemorialWheelFocusHalfRangeDegrees = 24f

@Composable
fun MemorialBriefingPane(
    pendingCount: Int,
    onOpenMemorialDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ImperialIvory)
            .clickable(onClick = onOpenMemorialDemo),
    ) {
        val expanded = maxWidth >= 620.dp
        Image(
            painter = painterResource(id = R.drawable.memorial_xuan_paper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.36f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ImperialIvory.copy(alpha = 0.1f),
                            ImperialParchment.copy(alpha = 0.45f),
                            ImperialIvory.copy(alpha = 0.82f),
                        ),
                        center = Offset.Infinite,
                        radius = 900f,
                    ),
                ),
        )
        MemorialCoverWheel(
            coverResources = MemorialCoverResources,
            modifier = Modifier.fillMaxSize(),
        )
        BriefingCopy(
            pendingCount = pendingCount,
            expanded = expanded,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = if (expanded) 36.dp else 24.dp, top = if (expanded) 56.dp else 40.dp),
        )
    }
}

@Composable
private fun MemorialCoverWheel(
    coverResources: List<Int>,
    modifier: Modifier = Modifier,
) {
    var wheelRotation by remember { mutableFloatStateOf(0f) }
    val stepDegrees = 360f / MemorialWheelItemCount
    val animatedWheelRotation by animateFloatAsState(
        targetValue = wheelRotation,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = 260f,
        ),
        label = "memorialWheelRotation",
    )
    BoxWithConstraints(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        wheelRotation += dragAmount.y * MemorialWheelDragDegreesPerPixel
                    },
                    onDragEnd = {
                        val snappedRotation = (wheelRotation / stepDegrees).roundToInt() * stepDegrees
                        wheelRotation = snappedRotation
                    },
                    onDragCancel = {
                        val snappedRotation = (wheelRotation / stepDegrees).roundToInt() * stepDegrees
                        wheelRotation = snappedRotation
                    },
                )
            },
    ) {
        val expanded = maxWidth >= 620.dp
        val panelMin = min(maxWidth.value, maxHeight.value).dp
        val radius = panelMin * if (expanded) 0.68f else 0.66f
        val innerRadius = radius * 0.63f
        val centerX = maxWidth + panelMin * if (expanded) 0.02f else 0.0f
        val centerY = maxHeight * if (expanded) 0.56f else 0.55f
        val cardWidth = if (expanded) 94.dp else 72.dp
        val startDegrees = MemorialActiveSlotDegrees + animatedWheelRotation

        MemorialWheelInnerDisc(
            centerX = centerX,
            centerY = centerY,
            radius = innerRadius,
            expanded = expanded,
            modifier = Modifier.fillMaxSize(),
        )
        val wheelItems = remember(startDegrees, stepDegrees) {
            List(MemorialWheelItemCount) { index ->
                val degrees = startDegrees + index * stepDegrees
                WheelItemPlacement(
                    index = index,
                    degrees = degrees,
                    focus = wheelItemFocus(degrees),
                )
            }.sortedBy { it.focus }
        }
        wheelItems.forEach { item ->
            MemorialWheelCover(
                resId = coverResources[item.index % coverResources.size],
                index = item.index,
                degrees = item.degrees,
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                width = cardWidth,
                focus = item.focus,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private data class WheelItemPlacement(
    val index: Int,
    val degrees: Float,
    val focus: Float,
)

private fun wheelItemFocus(degrees: Float): Float {
    val distance = angularDistanceDegrees(degrees, MemorialActiveSlotDegrees)
    return (1f - distance / MemorialWheelFocusHalfRangeDegrees).coerceIn(0f, 1f)
}

private fun angularDistanceDegrees(a: Float, b: Float): Float {
    val diff = ((a - b + 540f) % 360f) - 180f
    return abs(diff)
}

@Composable
private fun MemorialWheelInnerDisc(
    centerX: Dp,
    centerY: Dp,
    radius: Dp,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val diameter = radius * 2f
    val iconSize = if (expanded) 72.dp else 56.dp
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .offset(x = centerX - radius, y = centerY - radius)
                .size(diameter)
                .background(ImperialParchment.copy(alpha = 0.42f), CircleShape)
                .border(1.dp, ImperialBronze.copy(alpha = 0.22f), CircleShape),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = radius * 0.18f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.imperial_ornament_jia_ma),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    alpha = 0.46f,
                    colorFilter = ColorFilter.tint(ImperialUmber),
                )
                Text(
                    text = "轻触阅读",
                    style = if (expanded) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    color = ImperialUmber.copy(alpha = 0.62f),
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "上下拨动奏章轮",
                    style = if (expanded) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    color = ImperialUmber.copy(alpha = 0.48f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MemorialWheelCover(
    resId: Int,
    index: Int,
    degrees: Float,
    centerX: Dp,
    centerY: Dp,
    radius: Dp,
    width: Dp,
    focus: Float,
    modifier: Modifier = Modifier,
) {
    val active = focus > 0.52f
    val scaledWidth = width * (1f + (MemorialWheelActiveScale - 1f) * focus)
    val animatedBorderAlpha by animateFloatAsState(
        targetValue = 0.46f + 0.24f * focus,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 360f,
        ),
        label = "memorialCoverBorderAlpha",
    )
    val radians = Math.toRadians(degrees.toDouble())
    val height = scaledWidth / MemorialCoverAspect
    val x = centerX + radius * cos(radians).toFloat() - scaledWidth / 2f
    val y = centerY + radius * sin(radians).toFloat() - height * 0.75f
    Box(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .offset(x = x, y = y)
                .width(scaledWidth)
                .aspectRatio(MemorialCoverAspect)
                .graphicsLayer(
                    rotationZ = degrees + 90f,
                    transformOrigin = TransformOrigin(0.5f, 0.75f),
                )
                .background(ImperialParchment, RoundedCornerShape(if (active) 5.dp else 3.dp))
                .border(
                    width = if (active) 1.4.dp else 0.8.dp,
                    color = if (active) {
                        ImperialCinnabar.copy(alpha = animatedBorderAlpha)
                    } else {
                        ImperialBronze.copy(alpha = animatedBorderAlpha)
                    },
                    shape = RoundedCornerShape(if (active) 5.dp else 3.dp),
                ),
        ) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.05f),
                                ImperialUmber.copy(alpha = if (active) 0.08f else 0.13f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = if (active) 5.dp else 3.dp,
                        color = ImperialIvory.copy(alpha = if (active) 0.24f else 0.18f),
                        shape = RoundedCornerShape(if (active) 5.dp else 3.dp),
                    ),
            )
            MemorialCoverLabel(
                active = active,
                index = index,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun MemorialCoverLabel(
    active: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val labelWidth = if (active) 0.5f else 0.48f
    val labelHeight = if (active) 0.58f else 0.56f
    BoxWithConstraints(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(maxWidth * labelWidth)
                .aspectRatio(MemorialCoverAspect / labelHeight * labelWidth)
                .background(ImperialIvory.copy(alpha = if (active) 0.88f else 0.78f), RoundedCornerShape(2.dp))
                .border(1.dp, ImperialBronze.copy(alpha = if (active) 0.76f else 0.52f), RoundedCornerShape(2.dp))
                .padding(if (active) 5.dp else 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(0.8.dp, ImperialBronze.copy(alpha = if (active) 0.66f else 0.46f), RoundedCornerShape(1.dp)),
            )
            Image(
                painter = painterResource(id = R.drawable.memorial_cover_corner),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(if (active) 11.dp else 8.dp),
                alpha = if (active) 0.72f else 0.5f,
                colorFilter = ColorFilter.tint(ImperialCinnabar.copy(alpha = if (active) 0.7f else 0.46f)),
            )
            Image(
                painter = painterResource(id = R.drawable.memorial_cover_corner),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(if (active) 11.dp else 8.dp)
                    .graphicsLayer(rotationZ = 90f),
                alpha = if (active) 0.72f else 0.5f,
                colorFilter = ColorFilter.tint(ImperialCinnabar.copy(alpha = if (active) 0.7f else 0.46f)),
            )
            Image(
                painter = painterResource(id = R.drawable.memorial_cover_corner),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(if (active) 11.dp else 8.dp)
                    .graphicsLayer(rotationZ = 180f),
                alpha = if (active) 0.72f else 0.5f,
                colorFilter = ColorFilter.tint(ImperialCinnabar.copy(alpha = if (active) 0.7f else 0.46f)),
            )
            Image(
                painter = painterResource(id = R.drawable.memorial_cover_corner),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(if (active) 11.dp else 8.dp)
                    .graphicsLayer(rotationZ = 270f),
                alpha = if (active) 0.72f else 0.5f,
                colorFilter = ColorFilter.tint(ImperialCinnabar.copy(alpha = if (active) 0.7f else 0.46f)),
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (active) 2.dp else 1.dp),
            ) {
                Text(
                    text = "奏\n章",
                    style = if (active) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    color = ImperialUmber,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = if (active) MaterialTheme.typography.titleMedium.lineHeight else MaterialTheme.typography.titleSmall.lineHeight,
                )
                Text(
                    text = if (active) "甲辰" else ((index % 9) + 1).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ImperialCinnabar.copy(alpha = if (active) 0.82f else 0.62f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BriefingCopy(
    pendingCount: Int,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (expanded) 10.dp else 8.dp),
    ) {
        Text(
            text = "奏章",
            style = if (expanded) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayMedium,
            color = ImperialUmber,
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = "门下既审，呈于御前",
            style = MaterialTheme.typography.titleMedium,
            color = ImperialUmber.copy(alpha = 0.82f),
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = "今日尚有 $pendingCount 封待批。轻触此页，展开奏章堆叠，准、驳、留中皆可一笔批下。",
            style = MaterialTheme.typography.bodyMedium,
            color = ImperialUmber.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth(if (expanded) 0.38f else 0.56f),
        )
    }
}
