package com.ohstem.robot_controller.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

private const val DEADZONE_RATIO = 0.10f

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    dotSize: Dp = 56.dp,
    onValueChange: (x: Float, y: Float) -> Unit,
    onClick: () -> Unit = {}
) {
    var dotPosition by remember { mutableStateOf(Offset.Zero) }
    var isPressed by remember { mutableStateOf(false) }
    var isClicked by remember { mutableStateOf(false) }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 0.3f,
        animationSpec = tween(100),
        label = "joystickAlpha"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = tween(100),
        label = "joystickScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isPressed = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val radiusPx = size.toPx() / 2
                        val newPos = dotPosition + dragAmount
                        val dist = sqrt(newPos.x * newPos.x + newPos.y * newPos.y)
                        val clampedRadius = radiusPx * (1f - DEADZONE_RATIO)

                        dotPosition = if (dist <= clampedRadius) {
                            newPos
                        } else {
                            val angle = atan2(newPos.y.toDouble(), newPos.x.toDouble()).toFloat()
                            Offset(
                                cos(angle) * clampedRadius,
                                sin(angle) * clampedRadius
                            )
                        }

                        // Apply deadzone: only emit if past deadzone
                        val normalizedDist = dist / clampedRadius
                        if (normalizedDist > DEADZONE_RATIO) {
                            onValueChange(
                                dotPosition.x / clampedRadius,
                                -dotPosition.y / clampedRadius
                            )
                        } else {
                            onValueChange(0f, 0f)
                        }
                    },
                    onDragEnd = {
                        dotPosition = Offset.Zero
                        isPressed = false
                        onValueChange(0f, 0f)
                    },
                    onDragCancel = {
                        dotPosition = Offset.Zero
                        isPressed = false
                        onValueChange(0f, 0f)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isClicked = !isClicked
                        onClick()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasCenter = Offset(size.toPx() / 2, size.toPx() / 2)
            val outerRadius = size.toPx() / 2
            val dotRadius = dotSize.toPx() / 2

            // Outer circle border
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = outerRadius - 2.dp.toPx(),
                center = canvasCenter,
                style = Stroke(width = 2.dp.toPx())
            )

            // Outer fill
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = outerRadius - 2.dp.toPx(),
                center = canvasCenter
            )

            // Inner subtle grid lines for visual reference
            drawGridLines(canvasCenter, outerRadius)

            // Deadzone circle (subtle)
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = outerRadius * DEADZONE_RATIO,
                center = canvasCenter,
                style = Stroke(width = 1.dp.toPx())
            )

            // Shadow under dot
            val shadowOffset = 3.dp.toPx()
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = dotRadius * animatedScale,
                center = canvasCenter + dotPosition + Offset(shadowOffset, shadowOffset)
            )

            // Dot glow
            drawCircle(
                color = Color(0xFF448AFF).copy(alpha = animatedAlpha * 0.3f),
                radius = dotRadius * 1.6f * animatedScale,
                center = canvasCenter + dotPosition
            )

            // Dot main
            drawCircle(
                color = if (isPressed) Color(0xFF82B1FF) else Color(0xFF448AFF),
                radius = dotRadius * animatedScale,
                center = canvasCenter + dotPosition
            )

            // Dot inner highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = dotRadius * 0.35f * animatedScale,
                center = canvasCenter + dotPosition
            )

            // Axis indicator lines (subtle crosshair)
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = canvasCenter - Offset(outerRadius * 0.6f, 0f),
                end = canvasCenter + Offset(outerRadius * 0.6f, 0f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = canvasCenter - Offset(0f, outerRadius * 0.6f),
                end = canvasCenter + Offset(0f, outerRadius * 0.6f),
                strokeWidth = 1.dp.toPx()
            )
        }

        // L3/R3 click indicator dot
        if (isClicked) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(10.dp)
                    .background(Color.White.copy(alpha = 0.6f), CircleShape)
            )
        }
    }
}

private fun DrawScope.drawGridLines(center: Offset, radius: Float) {
    val gridColor = Color.White.copy(alpha = 0.03f)
    val step = radius / 4
    for (i in 1..3) {
        val r = step * i
        drawCircle(
            color = gridColor,
            radius = r,
            center = center,
            style = Stroke(width = 0.5.dp.toPx())
        )
    }
}