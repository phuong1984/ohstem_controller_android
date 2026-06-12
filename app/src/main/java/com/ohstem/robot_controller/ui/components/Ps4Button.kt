package com.ohstem.robot_controller.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Ps4Button(
    label: String,
    color: Color,
    onClick: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    fontSizeMultiplier: Float = 1.0f
) {
    var isPressed by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = tween(100),
        label = "buttonScale"
    )

    val fontSizeValue = (size / 1.dp) * 0.65f * fontSizeMultiplier
    val fontSize = fontSizeValue.sp
    val glyphOffset = -(fontSizeValue * 0.12f).dp

    Box(
        modifier = modifier
            .size(size)
            .scale(animatedScale)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(CircleShape)
            .background(if (isPressed) color else color.copy(alpha = 0.75f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onClick()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = glyphOffset)
        )
    }
}