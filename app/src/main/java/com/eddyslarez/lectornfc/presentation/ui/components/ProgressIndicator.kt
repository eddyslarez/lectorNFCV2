package com.eddyslarez.lectornfc.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Float = 8f,
    backgroundColor: Color = Color.Gray.copy(alpha = 0.3f),
    showPercentage: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = EaseInOutCubic),
        label = "progress"
    )

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircularProgress(
                progress = animatedProgress,
                color = color,
                strokeWidth = strokeWidth,
                backgroundColor = backgroundColor
            )
        }

        if (showPercentage) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun LinearProgressWithLabel(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun AnimatedProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF4CAF50),
    secondaryColor: Color = Color(0xFF2196F3),
    backgroundColor: Color = Color.Gray.copy(alpha = 0.3f),
    strokeWidth: Float = 12f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_animation")

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500, easing = EaseInOutCubic),
        label = "progress"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAnimatedProgressRing(
                progress = animatedProgress,
                rotation = rotation,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                backgroundColor = backgroundColor,
                strokeWidth = strokeWidth
            )
        }
    }
}

@Composable
fun SectorProgressGrid(
    totalSectors: Int,
    crackedSectors: Set<Int>,
    currentSector: Int,
    modifier: Modifier = Modifier
) {
    val columns = 4
    val rows = (totalSectors + columns - 1) / columns

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Progreso por Sectores",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until columns) {
                        val sector = row * columns + col
                        if (sector < totalSectors) {
                            SectorIndicator(
                                sector = sector,
                                isCracked = crackedSectors.contains(sector),
                                isCurrent = sector == currentSector,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (row < rows - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SectorIndicator(
    sector: Int,
    isCracked: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isCracked -> Color(0xFF4CAF50)
        isCurrent -> Color(0xFFFF9800)
        else -> Color(0xFF616161)
    }

    val animatedColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(300),
        label = "sector_color"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (isCurrent) {
                    val infiniteTransition = rememberInfiniteTransition(label = "current_sector")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Modifier.then(Modifier)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = animatedColor)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sector.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private fun DrawScope.drawCircularProgress(
    progress: Float,
    color: Color,
    strokeWidth: Float,
    backgroundColor: Color
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = (size.minDimension - strokeWidth) / 2

    // Background circle
    drawCircle(
        color = backgroundColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Progress arc
    val sweepAngle = progress * 360f
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawAnimatedProgressRing(
    progress: Float,
    rotation: Float,
    primaryColor: Color,
    secondaryColor: Color,
    backgroundColor: Color,
    strokeWidth: Float
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = (size.minDimension - strokeWidth) / 2

    // Background circle
    drawCircle(
        color = backgroundColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth / 2, cap = StrokeCap.Round)
    )

    // Animated background ring
    val backgroundSweepAngle = 30f
    for (i in 0 until 12) {
        val angle = i * 30f + rotation
        val startAngle = angle - backgroundSweepAngle / 2
        drawArc(
            color = secondaryColor.copy(alpha = 0.3f),
            startAngle = startAngle,
            sweepAngle = backgroundSweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth / 3, cap = StrokeCap.Round)
        )
    }

    // Progress arc
    val sweepAngle = progress * 360f
    drawArc(
        color = primaryColor,
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Progress indicator dot
    if (progress > 0) {
        val progressAngle = (-90f + sweepAngle) * (Math.PI / 180f)
        val dotX = center.x + radius * cos(progressAngle).toFloat()
        val dotY = center.y + radius * sin(progressAngle).toFloat()

        drawCircle(
            color = primaryColor,
            radius = strokeWidth / 2,
            center = Offset(dotX, dotY)
        )
    }
}
