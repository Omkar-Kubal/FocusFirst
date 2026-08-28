package com.focusfirst.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfirst.data.model.TimerPhase
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TokiChronoRing(
    displayTime: String,
    phase: TimerPhase,
    progress: Float,
    accessibilitySummary: String,
    modifier: Modifier = Modifier,
    ringSize: Dp = 320.dp,
    supportingText: String,
    isPaused: Boolean,
) {
    val cs = MaterialTheme.colorScheme
    val accent = when (phase) {
        TimerPhase.FOCUS -> FocusedVoid.FocusRed
        TimerPhase.SHORT_BREAK -> FocusedVoid.ShortBreak
        TimerPhase.LONG_BREAK -> FocusedVoid.LongBreak
    }
    val targetProgress = progress.coerceIn(0f, 1f)
    val progressAnim = remember { Animatable(targetProgress) }
    LaunchedEffect(targetProgress) {
        progressAnim.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(280, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        )
    }

    Box(
        modifier = modifier
            .size(ringSize)
            .semantics { contentDescription = accessibilitySummary },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = progressAnim.value
            val stroke = 14.dp.toPx()
            val innerStroke = 9.dp.toPx()
            val inset = stroke / 2f + 5.dp.toPx()
            val canvasSize = this.size
            val arcSize = Size(canvasSize.width - inset * 2f, canvasSize.height - inset * 2f)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = cs.surfaceVariant,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            if (p > 0f) {
                drawArc(
                    color = accent.copy(alpha = if (isPaused) 0.55f else 1f),
                    startAngle = -90f,
                    sweepAngle = 360f * p,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }

            val innerInset = inset + 34.dp.toPx()
            drawArc(
                color = cs.onSurface.copy(alpha = 0.9f),
                startAngle = 210f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(innerInset, innerInset),
                size = Size(canvasSize.width - innerInset * 2f, canvasSize.height - innerInset * 2f),
                style = Stroke(innerStroke, cap = StrokeCap.Round),
            )

            val radius = (canvasSize.minDimension - inset * 2f) / 2f
            val angle = Math.toRadians((-90f + 360f * p).toDouble())
            val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
            val handle = Offset(
                x = center.x + cos(angle).toFloat() * radius,
                y = center.y + sin(angle).toFloat() * radius,
            )
            drawCircle(
                color = if (phase == TimerPhase.FOCUS) FocusedVoid.FocusRed else accent,
                radius = 8.dp.toPx(),
                center = handle,
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = 3.dp.toPx(),
                center = handle,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayTime,
                color = cs.onBackground,
                fontSize = 78.sp,
                lineHeight = 82.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = supportingText,
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
