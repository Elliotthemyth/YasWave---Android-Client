package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WaveVisualizer(
    bands: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 50.dp,
    barSpacingRatio: Float = 0.3f,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalBars = bands.size.coerceAtLeast(1)
        val availableWidth = size.width
        val canvasHeight = size.height

        val stepWidth = availableWidth / totalBars
        val barWidth = stepWidth * (1f - barSpacingRatio)
        val spacing = stepWidth * barSpacingRatio

        for (i in bands.indices) {
            val amplitude = bands[i].coerceIn(0.04f, 1.0f)
            val barHeight = (canvasHeight * amplitude).coerceAtLeast(4f)
            val left = i * stepWidth + spacing / 2f
            val top = canvasHeight - barHeight

            // Draw clean restrained bar
            drawRoundRect(
                color = accentColor.copy(alpha = 0.75f + (amplitude * 0.25f)),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
            )
        }
    }
}
