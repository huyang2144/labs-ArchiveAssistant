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
import androidx.compose.runtime.key
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
import androidx.compose.ui.unit.sp
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
import kotlin.random.Random

private const val MemorialCoverAspect = 1f / 2f
private const val MemorialWheelItemCount = 24
private const val MemorialActiveSlotDegrees = 225f
private const val MemorialWheelDragDegreesPerPixel = -0.18f
private const val MemorialWheelActiveScale = 1.58f
private const val MemorialWheelFocusHalfRangeDegrees = 24f
private const val MemorialWheelCoverSeed = 20260627
private const val MemorialWheelDuplicateGuard = 3
private val MemorialInk = Color.Black

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
    val shuffledCoverResources = remember(coverResources) {
        buildWheelCoverSequence(
            coverResources = coverResources,
            itemCount = MemorialWheelItemCount,
            seed = MemorialWheelCoverSeed,
        )
    }
    if (shuffledCoverResources.isEmpty()) return
    val animatedWheelRotation by animateFloatAsState(
        targetValue = wheelRotation,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 420f,
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
        val centerY = maxHeight * if (expanded) 0.61f else 0.59f
        val cardWidth = if (expanded) 94.dp else 72.dp
        val startDegrees = MemorialActiveSlotDegrees + animatedWheelRotation

        MemorialWheelInnerDisc(
            centerX = centerX,
            centerY = centerY,
            radius = innerRadius,
            expanded = expanded,
            modifier = Modifier.fillMaxSize(),
        )
        val wheelItems = remember(startDegrees) {
            val seamIndex = floorMod(((-startDegrees + 45f) / stepDegrees).roundToInt(), MemorialWheelItemCount)
            List(MemorialWheelItemCount) { index ->
                val degrees = startDegrees + index * stepDegrees
                WheelItemPlacement(
                    index = index,
                    drawOrder = floorMod(index - seamIndex, MemorialWheelItemCount),
                    degrees = degrees,
                    resId = shuffledCoverResources[index],
                    focus = wheelItemFocus(degrees),
                )
            }.sortedBy { it.drawOrder }
        }
        wheelItems.forEach { item ->
            key(item.index) {
                MemorialWheelCover(
                    resId = item.resId,
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
}

private data class WheelItemPlacement(
    val index: Int,
    val drawOrder: Int,
    val degrees: Float,
    val resId: Int,
    val focus: Float,
)

private fun buildWheelCoverSequence(
    coverResources: List<Int>,
    itemCount: Int,
    seed: Int,
): List<Int> {
    if (coverResources.isEmpty()) return emptyList()
    val random = Random(seed)
    val uniqueResources = coverResources.distinct()
    val guard = min(MemorialWheelDuplicateGuard, (uniqueResources.size - 1).coerceAtLeast(0))
    if (guard == 0) return List(itemCount) { coverResources[it % coverResources.size] }

    repeat(160) {
        val sequence = mutableListOf<Int>()
        while (sequence.size < itemCount) {
            val candidate = uniqueResources
                .shuffled(random)
                .firstOrNull { resource ->
                    sequence.takeLast(guard).none { recent -> recent == resource }
                } ?: uniqueResources.random(random)
            sequence += candidate
        }
        if (isCircularSequenceValid(sequence, guard)) {
            return sequence
        }
    }
    return buildGreedyCircularFallback(uniqueResources, itemCount, guard)
}

private fun buildGreedyCircularFallback(
    resources: List<Int>,
    itemCount: Int,
    guard: Int,
): List<Int> {
    val sequence = mutableListOf<Int>()
    repeat(itemCount) { index ->
        val candidate = resources
            .sortedBy { resource -> sequence.count { it == resource } }
            .firstOrNull { resource ->
                sequence.takeLast(guard).none { recent -> recent == resource }
            } ?: resources[index % resources.size]
        sequence += candidate
    }
    repeat(itemCount * resources.size) {
        if (isCircularSequenceValid(sequence, guard)) {
            return sequence
        }
        val conflictIndex = sequence.indices.firstOrNull { index ->
            (1..guard).any { distance -> sequence[index] == sequence[(index + distance) % sequence.size] }
        } ?: return sequence
        val swapIndex = sequence.indices.firstOrNull { index ->
            index != conflictIndex && canSwapWithoutNearDuplicate(sequence, conflictIndex, index, guard)
        } ?: return sequence
        val tmp = sequence[conflictIndex]
        sequence[conflictIndex] = sequence[swapIndex]
        sequence[swapIndex] = tmp
    }
    return sequence
}

private fun canSwapWithoutNearDuplicate(
    sequence: List<Int>,
    firstIndex: Int,
    secondIndex: Int,
    guard: Int,
): Boolean {
    val mutable = sequence.toMutableList()
    val tmp = mutable[firstIndex]
    mutable[firstIndex] = mutable[secondIndex]
    mutable[secondIndex] = tmp
    return isCircularSequenceValidAt(mutable, firstIndex, guard) &&
        isCircularSequenceValidAt(mutable, secondIndex, guard)
}

private fun isCircularSequenceValid(sequence: List<Int>, guard: Int): Boolean {
    if (sequence.isEmpty()) return true
    return sequence.indices.all { index ->
        isCircularSequenceValidAt(sequence, index, guard)
    }
}

private fun isCircularSequenceValidAt(sequence: List<Int>, index: Int, guard: Int): Boolean {
    if (sequence.isEmpty()) return true
    return (1..guard).all { distance ->
        sequence[index] != sequence[(index + distance) % sequence.size] &&
            sequence[index] != sequence[(index - distance + sequence.size) % sequence.size]
    }
}

private fun wheelItemFocus(degrees: Float): Float {
    val distance = angularDistanceDegrees(degrees, MemorialActiveSlotDegrees)
    return (1f - distance / MemorialWheelFocusHalfRangeDegrees).coerceIn(0f, 1f)
}

private fun floorMod(value: Int, modulus: Int): Int = ((value % modulus) + modulus) % modulus

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
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
                    colorFilter = ColorFilter.tint(MemorialInk),
                )
                Text(
                    text = "轻触阅读",
                    style = if (expanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    color = MemorialInk,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "上下拨动奏章轮",
                    style = if (expanded) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    color = MemorialInk.copy(alpha = 0.78f),
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
    val coverScale = 1f + (MemorialWheelActiveScale - 1f) * focus
    val borderAlpha = lerpFloat(0.46f, 0.7f, focus)
    val radians = Math.toRadians(degrees.toDouble())
    val height = width / MemorialCoverAspect
    val x = centerX + radius * cos(radians).toFloat() - width / 2f
    val y = centerY + radius * sin(radians).toFloat() - height * 0.75f
    val cornerRadius = lerpDp(3.dp, 5.dp, focus)
    val borderWidth = lerpDp(0.8.dp, 1.4.dp, focus)
    val innerBorderWidth = lerpDp(3.dp, 5.dp, focus)
    Box(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .offset(x = x, y = y)
                .width(width)
                .aspectRatio(MemorialCoverAspect)
                .graphicsLayer(
                    rotationZ = degrees + 90f,
                    transformOrigin = TransformOrigin(0.5f, 0.75f),
                    scaleX = coverScale,
                    scaleY = coverScale,
                )
                .background(ImperialParchment, RoundedCornerShape(cornerRadius))
                .border(
                    width = borderWidth,
                    color = ImperialBronze.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(cornerRadius),
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
                                ImperialUmber.copy(alpha = lerpFloat(0.13f, 0.08f, focus)),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = innerBorderWidth,
                        color = ImperialIvory.copy(alpha = lerpFloat(0.18f, 0.24f, focus)),
                        shape = RoundedCornerShape(cornerRadius),
                    ),
            )
            MemorialCoverLabel(
                focus = focus,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun MemorialCoverLabel(
    focus: Float,
    modifier: Modifier = Modifier,
) {
    val labelWidth = lerpFloat(0.48f, 0.5f, focus)
    val labelHeight = lerpFloat(0.56f, 0.58f, focus)
    val cornerSize = lerpDp(8.dp, 11.dp, focus)
    val cornerAlpha = lerpFloat(0.5f, 0.72f, focus)
    BoxWithConstraints(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(maxWidth * labelWidth)
                .aspectRatio(MemorialCoverAspect / labelHeight * labelWidth)
                .background(ImperialIvory.copy(alpha = lerpFloat(0.9f, 0.96f, focus)), RoundedCornerShape(2.dp))
                .border(1.dp, ImperialBronze.copy(alpha = lerpFloat(0.52f, 0.76f, focus)), RoundedCornerShape(2.dp))
                .padding(lerpDp(4.dp, 5.dp, focus)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(0.8.dp, ImperialBronze.copy(alpha = lerpFloat(0.46f, 0.66f, focus)), RoundedCornerShape(1.dp)),
            )
            Image(
                painter = painterResource(id = R.drawable.memorial_cover_corner),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(cornerSize),
                alpha = cornerAlpha,
                colorFilter = ColorFilter.tint(ImperialCinnabar.copy(alpha = lerpFloat(0.46f, 0.7f, focus))),
            )
            Image(
                painter = painterResource(id = R.drawable.memorial_cover_corner),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(cornerSize)
                    .graphicsLayer(rotationZ = 90f),
                alpha = cornerAlpha,
                colorFilter = ColorFilter.tint(ImperialCinnabar.copy(alpha = lerpFloat(0.46f, 0.7f, focus))),
            )
            Image(
                painter = painterResource(id = R.drawable.memorial_cover_corner),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(cornerSize)
                    .graphicsLayer(rotationZ = 180f),
                alpha = cornerAlpha,
                colorFilter = ColorFilter.tint(ImperialCinnabar.copy(alpha = lerpFloat(0.46f, 0.7f, focus))),
            )
            Image(
                painter = painterResource(id = R.drawable.memorial_cover_corner),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(cornerSize)
                    .graphicsLayer(rotationZ = 270f),
                alpha = cornerAlpha,
                colorFilter = ColorFilter.tint(ImperialCinnabar.copy(alpha = lerpFloat(0.46f, 0.7f, focus))),
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "奏\n章",
                    style = MaterialTheme.typography.titleLarge,
                    color = MemorialInk,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 25.sp,
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
            color = MemorialInk,
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = "今日尚有 $pendingCount 封待批。轻触此页，展开奏章堆叠，准、驳、留中皆可一笔批下。",
            style = if (expanded) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = MemorialInk.copy(alpha = 0.78f),
            modifier = Modifier.fillMaxWidth(if (expanded) 0.38f else 0.56f),
        )
    }
}
