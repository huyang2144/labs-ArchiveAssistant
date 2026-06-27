package com.lyihub.archiveassistant.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyihub.archiveassistant.R
import com.lyihub.archiveassistant.ui.theme.ImperialBronze
import com.lyihub.archiveassistant.ui.theme.ImperialCinnabar
import com.lyihub.archiveassistant.ui.theme.ImperialIvory
import com.lyihub.archiveassistant.ui.theme.ImperialLightGold
import com.lyihub.archiveassistant.ui.theme.ImperialParchment
import com.lyihub.archiveassistant.ui.theme.ImperialUmber
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun MemorialBriefingPane(
    pendingCount: Int,
    onOpenMemorialDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverPainters = listOf(
        painterResource(R.drawable.memorial_cover_pattern),
        painterResource(R.drawable.memorial_cover_02),
        painterResource(R.drawable.memorial_cover_03),
        painterResource(R.drawable.memorial_cover_04),
        painterResource(R.drawable.memorial_cover_05),
        painterResource(R.drawable.memorial_cover_06),
        painterResource(R.drawable.memorial_cover_07),
        painterResource(R.drawable.memorial_cover_08),
        painterResource(R.drawable.memorial_cover_09),
        painterResource(R.drawable.memorial_cover_10),
        painterResource(R.drawable.memorial_cover_11),
        painterResource(R.drawable.memorial_cover_12),
    )

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
            coverPainters = coverPainters,
            modifier = Modifier.fillMaxSize(),
        )
        BriefingCopy(
            pendingCount = pendingCount,
            expanded = expanded,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(if (expanded) 30.dp else 20.dp),
        )
        RingCenterHint(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = if (expanded) 48.dp else 24.dp),
        )
    }
}

@Composable
private fun MemorialCoverWheel(
    coverPainters: List<Painter>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val panelMin = min(size.width, size.height)
        val outerRadius = panelMin * 0.92f
        val innerRadius = outerRadius * 0.57f
        val segmentSweep = 13.6f
        val segmentGap = 1.35f
        val startAngle = 122f
        val center = Offset(size.width * 0.86f, size.height * 0.92f)

        drawCircle(
            color = ImperialLightGold.copy(alpha = 0.34f),
            radius = outerRadius,
            center = center,
        )
        drawCircle(
            color = ImperialIvory.copy(alpha = 0.94f),
            radius = innerRadius,
            center = center,
        )

        repeat(22) { index ->
            val angle = startAngle + index * (segmentSweep + segmentGap)
            val painter = coverPainters[index % coverPainters.size]
            drawRingSegment(
                painter = painter,
                center = center,
                innerRadius = innerRadius,
                outerRadius = outerRadius,
                startDegrees = angle,
                sweepDegrees = segmentSweep,
                index = index,
            )
        }

        drawCircle(
            color = ImperialBronze.copy(alpha = 0.16f),
            radius = innerRadius,
            center = center,
        )
        drawCircle(
            color = ImperialCinnabar.copy(alpha = 0.22f),
            radius = innerRadius * 0.78f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRingSegment(
    painter: Painter,
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startDegrees: Float,
    sweepDegrees: Float,
    index: Int,
) {
    val segmentPath = ringSegmentPath(center, innerRadius, outerRadius, startDegrees, sweepDegrees)
    val midDegrees = startDegrees + sweepDegrees / 2f
    val midRadians = Math.toRadians(midDegrees.toDouble())
    val segmentCenter = Offset(
        x = center.x + cos(midRadians).toFloat() * ((innerRadius + outerRadius) / 2f),
        y = center.y + sin(midRadians).toFloat() * ((innerRadius + outerRadius) / 2f),
    )
    val segmentWidth = (outerRadius - innerRadius) * 0.88f
    val segmentHeight = outerRadius * 0.26f

    clipPath(segmentPath) {
        drawPath(
            path = segmentPath,
            color = if (index % 3 == 0) ImperialParchment else ImperialLightGold,
        )
        rotate(degrees = midDegrees + 90f, pivot = segmentCenter) {
            translate(
                left = segmentCenter.x - segmentWidth / 2f,
                top = segmentCenter.y - segmentHeight / 2f,
            ) {
                with(painter) {
                    draw(
                        size = Size(segmentWidth, segmentHeight),
                        alpha = 0.88f,
                    )
                }
            }
        }
        drawPath(
            path = segmentPath,
            color = ImperialIvory.copy(alpha = if (index % 2 == 0) 0.12f else 0.24f),
        )
    }
    drawPath(
        path = segmentPath,
        color = ImperialIvory.copy(alpha = 0.86f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.1.dp.toPx()),
    )
}

private fun ringSegmentPath(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startDegrees: Float,
    sweepDegrees: Float,
): Path {
    return Path().apply {
        arcTo(
            rect = Rect(center - Offset(outerRadius, outerRadius), Size(outerRadius * 2f, outerRadius * 2f)),
            startAngleDegrees = startDegrees,
            sweepAngleDegrees = sweepDegrees,
            forceMoveTo = true,
        )
        arcTo(
            rect = Rect(center - Offset(innerRadius, innerRadius), Size(innerRadius * 2f, innerRadius * 2f)),
            startAngleDegrees = startDegrees + sweepDegrees,
            sweepAngleDegrees = -sweepDegrees,
            forceMoveTo = false,
        )
        close()
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
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "门下既审，呈于御前",
            style = MaterialTheme.typography.titleMedium,
            color = ImperialUmber.copy(alpha = 0.82f),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "今日尚有 $pendingCount 封待批。轻触此页，展开奏章堆叠，准、驳、留中皆可一笔批下。",
            style = MaterialTheme.typography.bodyMedium,
            color = ImperialUmber.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth(if (expanded) 0.44f else 0.62f),
        )
    }
}

@Composable
private fun RingCenterHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .size(154.dp)
            .background(ImperialIvory.copy(alpha = 0.72f))
            .border(1.dp, ImperialParchment)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.dashboard_placeholder),
            contentDescription = null,
            modifier = Modifier.size(42.dp),
            colorFilter = ColorFilter.tint(ImperialBronze),
            alpha = 0.82f,
        )
        Text(
            text = "批阅",
            style = MaterialTheme.typography.titleLarge,
            color = ImperialUmber,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "奏折在此",
            style = MaterialTheme.typography.bodySmall,
            color = ImperialUmber.copy(alpha = 0.64f),
            textAlign = TextAlign.Center,
        )
    }
}
