package com.ohstem.robot_controller.ui.screens.gamepad

import android.content.pm.ActivityInfo
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ohstem.robot_controller.MainActivity
import com.ohstem.robot_controller.ui.components.DPad
import com.ohstem.robot_controller.ui.components.Joystick
import com.ohstem.robot_controller.ui.components.Ps4Button
import com.ohstem.robot_controller.ui.theme.*
import com.ohstem.robot_controller.viewmodel.GamepadViewModel
import kotlin.math.min

@Composable
fun GamepadScreen(viewModel: GamepadViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        val activity = context as? MainActivity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val refW = 800.dp
        val refH = 360.dp
        val scale = (min(maxWidth / refW, maxHeight / refH)).coerceIn(0.6f, 1.5f)

        val sclTrigW = (63f * scale).dp
        val sclTrigH = (25f * scale).dp
        val sclScreenPadding = (16f * scale).dp
        val sclButtonSize = (64f * scale).dp
        val sclJoySize = (90f * scale).dp
        val sclPs4BtnSize = (52f * scale).dp
        val sclActionBoxSize = (180f * scale).dp
        val sclDpadButtonSize = (50f / 130f * 180f * scale).dp
        val sclL1RightEdge = sclScreenPadding + sclTrigW
        val sclSpacer2 = (2f * scale).dp
        val sclSpacer6 = (6f * scale).dp
        val sclSpacer12 = (12f * scale).dp
        val sclSpacedBy32 = (32f * scale).dp

        val sclFont12 = (12f * scale).sp
        val sclFont11 = (11f * scale).sp
        val sclFont10 = (10f * scale).sp

        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DarkBackground
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                    ) {
                    GamepadStatusBar(
                        isConnected = uiState.isConnected,
                        isReconnecting = uiState.isReconnecting,
                        showDebugOverlay = uiState.showDebugOverlay,
                        robotName = uiState.robotName,
                        commandFps = uiState.commandFps,
                        onExit = { /* TODO: Navigate back */ },
                        onToggleDebug = { viewModel.toggleDebugOverlay() },
                        sclFont12 = sclFont12,
                        sclFont11 = sclFont11,
                        sclFont10 = sclFont10,
                        sclSpacer6 = sclSpacer6,
                        sclSpacer8 = (8f * scale).dp,
                        sclDotSize = (8f * scale).dp,
                        sclIconBtnSize = (24f * scale).dp,
                        sclIconSize = (16f * scale).dp,
                        sclPaddingH = (12f * scale).dp,
                        sclPaddingV = (4f * scale).dp
                    )

                    Spacer(modifier = Modifier.height(sclSpacer2))
                    TriggerRow(
                        viewModel = viewModel,
                        sclScreenPadding = sclScreenPadding,
                        sclTrigW = sclTrigW,
                        sclTrigH = sclTrigH,
                        sclFont11 = sclFont11,
                        sclSpacer6 = sclSpacer6,
                        sclLeftColW = (140f * scale).dp,
                        sclRightColW = (160f * scale).dp
                    )
                    Spacer(modifier = Modifier.height(sclSpacer6))

                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(0.3f)) {
                            DPad(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = sclL1RightEdge),
                                size = sclActionBoxSize,
                                buttonSize = sclDpadButtonSize,
                                onPress = { viewModel.onButtonPressed(it) },
                                onRelease = { viewModel.onButtonReleased(it) }
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(
                                        x = sclL1RightEdge + sclActionBoxSize,
                                        y = sclActionBoxSize - sclPs4BtnSize / 2
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Joystick(
                                    size = sclJoySize,
                                    onValueChange = { x, y ->
                                        viewModel.onJoystickMove("LX", x)
                                        viewModel.onJoystickMove("LY", y)
                                    },
                                    onClick = { viewModel.onButtonPressed("L3") }
                                )
                                Text(text = "L3", color = Color.Gray.copy(alpha = 0.5f), fontSize = sclFont10)
                            }
                        }

                        Column(
                            modifier = Modifier.weight(0.4f).padding(top = (16f * scale).dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(sclSpacedBy32),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ShareOptionButton(
                                    label = "SHARE",
                                    onClick = { viewModel.onButtonPressed("SHARE") },
                                    onRelease = { viewModel.onButtonReleased("SHARE") },
                                    sclButtonSize = sclButtonSize,
                                    sclFont12 = sclFont12
                                )
                                ShareOptionButton(
                                    label = "OPTIONS",
                                    onClick = { viewModel.onButtonPressed("OPTIONS") },
                                    onRelease = { viewModel.onButtonReleased("OPTIONS") },
                                    sclButtonSize = sclButtonSize,
                                    sclFont12 = sclFont12
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(0.3f).padding(end = sclScreenPadding + sclTrigW)) {
                            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                ActionButtons(
                                    viewModel = viewModel,
                                    sclPs4BtnSize = sclPs4BtnSize,
                                    sclContainerSize = sclActionBoxSize
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = -(sclActionBoxSize), y = sclActionBoxSize - sclPs4BtnSize / 2),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Joystick(
                                    size = sclJoySize,
                                    onValueChange = { x, y ->
                                        viewModel.onJoystickMove("RX", x)
                                        viewModel.onJoystickMove("RY", y)
                                    },
                                    onClick = { viewModel.onButtonPressed("R3") }
                                )
                                Text(text = "R3", color = Color.Gray.copy(alpha = 0.5f), fontSize = sclFont10)
                            }
                        }
                    }
                }
            }
            }

            if (uiState.showDebugOverlay) {
                DebugOverlay(
                    lastCommand = uiState.lastCommand,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = sclSpacer12, bottom = sclSpacer12),
                    sclFont12 = sclFont12,
                    sclPaddingH = (8f * scale).dp,
                    sclPaddingV = (4f * scale).dp
                )
            }
        }
    }
}

@Composable
private fun GamepadStatusBar(
    isConnected: Boolean,
    isReconnecting: Boolean = false,
    showDebugOverlay: Boolean = false,
    robotName: String,
    commandFps: Int,
    onExit: () -> Unit,
    onToggleDebug: () -> Unit = {},
    sclFont12: TextUnit,
    sclFont11: TextUnit,
    sclFont10: TextUnit,
    sclSpacer6: Dp,
    sclSpacer8: Dp,
    sclDotSize: Dp,
    sclIconBtnSize: Dp,
    sclIconSize: Dp,
    sclPaddingH: Dp,
    sclPaddingV: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = sclPaddingH, vertical = sclPaddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val dotColor = when {
                isReconnecting -> WarningOrange
                isConnected -> ConnectedGreen
                else -> DisconnectedRed
            }
            val statusText = when {
                isReconnecting -> "Reconnecting..."
                isConnected -> robotName.ifEmpty { "ESP32-Robot" }
                else -> "Disconnected"
            }
            val statusColor = when {
                isReconnecting || isConnected -> Color.White
                else -> Color.Gray
            }
            Box(
                modifier = Modifier
                    .size(sclDotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(sclSpacer6))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = sclFont12,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = if (showDebugOverlay) "Debug ON" else "PS4 Controller",
            color = if (showDebugOverlay) PrimaryBlue else Color.White.copy(alpha = 0.6f),
            fontSize = sclFont11,
            fontWeight = FontWeight.Light,
            modifier = Modifier.clickable { onToggleDebug() }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$commandFps c/s", color = Color.Gray.copy(alpha = 0.6f), fontSize = sclFont10)
            Spacer(modifier = Modifier.width(sclSpacer8))
            IconButton(onClick = onExit, modifier = Modifier.size(sclIconBtnSize)) {
                Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.Gray, modifier = Modifier.size(sclIconSize))
            }
        }
    }
}

@Composable
private fun TriggerRow(
    viewModel: GamepadViewModel,
    sclScreenPadding: Dp,
    sclTrigW: Dp,
    sclTrigH: Dp,
    sclFont11: TextUnit,
    sclSpacer6: Dp,
    sclLeftColW: Dp,
    sclRightColW: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sclScreenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.width(sclLeftColW), horizontalAlignment = Alignment.Start) {
            TriggerButton(label = "L2", onClick = { viewModel.onButtonPressed("L2") }, onRelease = { viewModel.onButtonReleased("L2") }, sclTrigW = sclTrigW, sclTrigH = sclTrigH, sclFont11 = sclFont11)
            Spacer(modifier = Modifier.height(sclSpacer6))
            TriggerButton(label = "L1", onClick = { viewModel.onButtonPressed("L1") }, onRelease = { viewModel.onButtonReleased("L1") }, sclTrigW = sclTrigW, sclTrigH = sclTrigH, sclFont11 = sclFont11)
        }
        Column(modifier = Modifier.width(sclRightColW), horizontalAlignment = Alignment.End) {
            TriggerButton(label = "R2", onClick = { viewModel.onButtonPressed("R2") }, onRelease = { viewModel.onButtonReleased("R2") }, sclTrigW = sclTrigW, sclTrigH = sclTrigH, sclFont11 = sclFont11)
            Spacer(modifier = Modifier.height(sclSpacer6))
            TriggerButton(label = "R1", onClick = { viewModel.onButtonPressed("R1") }, onRelease = { viewModel.onButtonReleased("R1") }, sclTrigW = sclTrigW, sclTrigH = sclTrigH, sclFont11 = sclFont11)
        }
    }
}

@Composable
private fun TriggerButton(
    label: String,
    onClick: () -> Unit,
    onRelease: () -> Unit,
    sclTrigW: Dp,
    sclTrigH: Dp,
    sclFont11: TextUnit
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(sclTrigW, sclTrigH)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(if (isPressed) Color(0xFF555555) else Color(0xFF2A2A2A))
            .pointerInput(Unit) { detectTapGestures(onPress = { isPressed = true; onClick(); tryAwaitRelease(); isPressed = false; onRelease() }) },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.LightGray, fontSize = sclFont11, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ShareOptionButton(
    label: String,
    onClick: () -> Unit,
    onRelease: () -> Unit,
    sclButtonSize: Dp,
    sclFont12: TextUnit
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(sclButtonSize, sclButtonSize * 0.6f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPressed) Color(0xFF444444) else Color(0xFF222222))
            .pointerInput(Unit) { detectTapGestures(onPress = { isPressed = true; onClick(); tryAwaitRelease(); isPressed = false; onRelease() }) },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.Gray, fontSize = sclFont12, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ActionButtons(
    viewModel: GamepadViewModel,
    sclPs4BtnSize: Dp,
    sclContainerSize: Dp
) {
    Box(modifier = Modifier.size(sclContainerSize)) {
        Ps4Button(
            label = "△",
            color = PS4Green,
            onClick = { viewModel.onButtonPressed("TRIANGLE") },
            onRelease = { viewModel.onButtonReleased("TRIANGLE") },
            modifier = Modifier.align(Alignment.TopCenter),
            size = sclPs4BtnSize
        )
        Ps4Button(
            label = "□",
            color = PS4Pink,
            onClick = { viewModel.onButtonPressed("SQUARE") },
            onRelease = { viewModel.onButtonReleased("SQUARE") },
            modifier = Modifier.align(Alignment.CenterStart),
            size = sclPs4BtnSize
        )
        Ps4Button(
            label = "○",
            color = PS4Red,
            onClick = { viewModel.onButtonPressed("CIRCLE") },
            onRelease = { viewModel.onButtonReleased("CIRCLE") },
            modifier = Modifier.align(Alignment.CenterEnd),
            size = sclPs4BtnSize,
            fontSizeMultiplier = 1.5f
        )
        Ps4Button(
            label = "✕",
            color = PS4Blue,
            onClick = { viewModel.onButtonPressed("CROSS") },
            onRelease = { viewModel.onButtonReleased("CROSS") },
            modifier = Modifier.align(Alignment.BottomCenter),
            size = sclPs4BtnSize
        )
    }
}

@Composable
private fun DebugOverlay(
    lastCommand: String,
    modifier: Modifier = Modifier,
    sclFont12: TextUnit,
    sclPaddingH: Dp,
    sclPaddingV: Dp
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.6f)),
        color = Color.Transparent
    ) {
        Text(
            text = "▶ $lastCommand",
            color = PrimaryBlue,
            fontSize = sclFont12,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = sclPaddingH, vertical = sclPaddingV)
        )
    }
}
