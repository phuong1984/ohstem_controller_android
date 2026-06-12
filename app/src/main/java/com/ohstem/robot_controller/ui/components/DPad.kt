package com.ohstem.robot_controller.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DPAD_COLOR = Color(0xFF2A2A2A)
private val DPAD_ACTIVE_COLOR = Color(0xFF555555)

@Composable
fun DPad(
    onPress: (String) -> Unit,
    onRelease: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    buttonSize: Dp = 50.dp
) {
    var activeDirection by remember { mutableStateOf<String?>(null) }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (activeDirection != null) 0.9f else 0.5f,
        animationSpec = tween(100),
        label = "dpadAlpha"
    )

    Box(modifier = modifier) {
        // Cross shape background with rounded corners
        val crossBg = DPAD_COLOR.copy(alpha = animatedAlpha)
        
        // Vertical bar
        Box(modifier = Modifier
            .size(buttonSize, size)
            .align(Alignment.Center)
            .clip(RoundedCornerShape(8.dp))
            .background(crossBg))
        
        // Horizontal bar
        Box(modifier = Modifier
            .size(size, buttonSize)
            .align(Alignment.Center)
            .clip(RoundedCornerShape(8.dp))
            .background(crossBg))

        // Up
        DPadButton(
            direction = "UP",
            icon = Icons.Default.ArrowUpward,
            isActive = activeDirection == "UP",
            onPress = { activeDirection = "UP"; onPress("UP") },
            onRelease = { activeDirection = null; onRelease("UP") },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(buttonSize)
        )
        
        // Down
        DPadButton(
            direction = "DOWN",
            icon = Icons.Default.ArrowDownward,
            isActive = activeDirection == "DOWN",
            onPress = { activeDirection = "DOWN"; onPress("DOWN") },
            onRelease = { activeDirection = null; onRelease("DOWN") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(buttonSize)
        )
        
        // Left
        DPadButton(
            direction = "LEFT",
            icon = Icons.Default.ArrowBack,
            isActive = activeDirection == "LEFT",
            onPress = { activeDirection = "LEFT"; onPress("LEFT") },
            onRelease = { activeDirection = null; onRelease("LEFT") },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(buttonSize)
        )
        
        // Right
        DPadButton(
            direction = "RIGHT",
            icon = Icons.Default.ArrowForward,
            isActive = activeDirection == "RIGHT",
            onPress = { activeDirection = "RIGHT"; onPress("RIGHT") },
            onRelease = { activeDirection = null; onRelease("RIGHT") },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(buttonSize)
        )
    }
}

@Composable
private fun DPadButton(
    direction: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(direction) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = direction,
            tint = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(28.dp)
        )
    }
}