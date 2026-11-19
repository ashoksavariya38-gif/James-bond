package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalLuminaColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom vector emblem combining a 4-point sparkle star nested inside a geometric spiral
 * and a subtle graduation cap outline with the Gemini-inspired blue -> purple -> cyan gradient.
 */
@Composable
fun LuminaSparkleLogo(
  modifier: Modifier = Modifier,
  size: Dp = 32.dp,
) {
  val luminaColors = LocalLuminaColors.current
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val cx = w / 2f
    val cy = h * 0.57f
    val gradient = Brush.linearGradient(
      colors = luminaColors.sparkleGradientStops,
      start = Offset(0f, 0f),
      end = Offset(w, h),
    )
    val strokeW = (w * 0.068f).coerceAtLeast(1.5f)

    // 1. Subtle graduation cap outline resting atop the spiral
    val capPath = Path().apply {
      moveTo(cx, h * 0.07f)
      lineTo(w * 0.78f, h * 0.19f)
      lineTo(cx, h * 0.30f)
      lineTo(w * 0.22f, h * 0.19f)
      close()
    }
    drawPath(
      path = capPath,
      brush = gradient,
      style = Stroke(width = strokeW * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    // Graduation cap tassel line
    drawLine(
      brush = gradient,
      start = Offset(w * 0.78f, h * 0.19f),
      end = Offset(w * 0.81f, h * 0.33f),
      strokeWidth = strokeW * 0.75f,
      cap = StrokeCap.Round,
    )

    // 2. Geometric spiral enclosing the 4-point sparkle star
    val spiralPath = Path()
    val turns = 1.38f
    val steps = 48
    val maxR = w * 0.37f
    val minR = w * 0.18f
    for (i in 0..steps) {
      val t = i.toFloat() / steps
      val angle = -Math.PI.toFloat() / 2f + t * turns * 2f * Math.PI.toFloat()
      val r = maxR - (maxR - minR) * t
      val px = cx + r * cos(angle)
      val py = cy + r * sin(angle)
      if (i == 0) spiralPath.moveTo(px, py) else spiralPath.lineTo(px, py)
    }
    drawPath(
      path = spiralPath,
      brush = gradient,
      style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Outer subtle arc balance
    drawArc(
      brush = gradient,
      startAngle = 145f,
      sweepAngle = 180f,
      useCenter = false,
      topLeft = Offset(cx - maxR, cy - maxR),
      size = Size(maxR * 2f, maxR * 2f),
      style = Stroke(width = strokeW * 0.8f, cap = StrokeCap.Round),
    )

    // 3. Central 4-point sparkle star
    val starOuter = w * 0.145f
    val starInner = w * 0.045f
    val starPath = Path().apply {
      moveTo(cx, cy - starOuter)
      quadraticTo(cx + starInner * 0.5f, cy - starInner * 0.5f, cx + starOuter, cy)
      quadraticTo(cx + starInner * 0.5f, cy + starInner * 0.5f, cx, cy + starOuter)
      quadraticTo(cx - starInner * 0.5f, cy + starInner * 0.5f, cx - starOuter, cy)
      quadraticTo(cx - starInner * 0.5f, cy - starInner * 0.5f, cx, cy - starOuter)
      close()
    }
    drawPath(path = starPath, brush = gradient)
  }
}
