/* NES emulator By ArDev */
package com.example.nes.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nes.core.Controller
import com.example.nes.data.ControlSize
import com.example.nes.data.EmulatorSettings
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VirtualController(
    settings: EmulatorSettings,
    onButtonChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val hapticEnabled = settings.hapticFeedback

    fun triggerHaptic() {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    val dpadSize: Dp = when (settings.controlSize) {
        ControlSize.SMALL -> 140.dp
        ControlSize.NORMAL -> 170.dp
        ControlSize.LARGE -> 200.dp
    }

    val actionButtonSize: Dp = when (settings.controlSize) {
        ControlSize.SMALL -> 52.dp
        ControlSize.NORMAL -> 64.dp
        ControlSize.LARGE -> 74.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(settings.controlsOpacity)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // D-Pad
        DpadControl(
            size = dpadSize,
            onDirectionChange = { up, down, left, right ->
                onButtonChange(Controller.BUTTON_UP, up)
                onButtonChange(Controller.BUTTON_DOWN, down)
                onButtonChange(Controller.BUTTON_LEFT, left)
                onButtonChange(Controller.BUTTON_RIGHT, right)
            },
            onHaptic = { triggerHaptic() }
        )

        // Center Select & Start
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RetroPillButton(
                label = "SELECT",
                testTag = "controller_select",
                onPressChange = { pressed ->
                    if (pressed) triggerHaptic()
                    onButtonChange(Controller.BUTTON_SELECT, pressed)
                }
            )
            RetroPillButton(
                label = "START",
                testTag = "controller_start",
                onPressChange = { pressed ->
                    if (pressed) triggerHaptic()
                    onButtonChange(Controller.BUTTON_START, pressed)
                }
            )
        }

        // Action Buttons (B & A) + Turbo
        ActionButtonsDeck(
            buttonSize = actionButtonSize,
            onButtonChange = onButtonChange,
            onHaptic = { triggerHaptic() }
        )
    }
}

@Composable
private fun DpadControl(
    size: Dp,
    onDirectionChange: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit,
    onHaptic: () -> Unit
) {
    var activeDir by remember { mutableStateOf("NONE") }

    Box(
        modifier = Modifier
            .size(size)
            .testTag("dpad_controller")
            .pointerInput(Unit) {
                fun processOffset(offset: Offset) {
                    val half = size.toPx() / 2f
                    val dx = offset.x - half
                    val dy = offset.y - half
                    val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

                    if (dist < half * 0.18f || dist > half * 1.3f) {
                        if (activeDir != "NONE") {
                            activeDir = "NONE"
                            onDirectionChange(false, false, false, false)
                        }
                        return
                    }

                    val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).let {
                        if (it < 0) it + 360.0 else it
                    }

                    // Angles: 0 = Right, 90 = Down, 180 = Left, 270 = Up
                    val up = angle in 202.5..337.5
                    val down = angle in 22.5..157.5
                    val left = angle in 112.5..247.5
                    val right = angle >= 292.5 || angle <= 67.5

                    val newDir = "$up-$down-$left-$right"
                    if (newDir != activeDir) {
                        activeDir = newDir
                        onHaptic()
                        onDirectionChange(up, down, left, right)
                    }
                }

                detectDragGestures(
                    onDragStart = { processOffset(it) },
                    onDrag = { change, _ ->
                        change.consume()
                        processOffset(change.position)
                    },
                    onDragEnd = {
                        activeDir = "NONE"
                        onDirectionChange(false, false, false, false)
                    },
                    onDragCancel = {
                        activeDir = "NONE"
                        onDirectionChange(false, false, false, false)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val half = size.toPx() / 2f
                        val dx = offset.x - half
                        val dy = offset.y - half
                        val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).let {
                            if (it < 0) it + 360.0 else it
                        }
                        val up = angle in 202.5..337.5
                        val down = angle in 22.5..157.5
                        val left = angle in 112.5..247.5
                        val right = angle >= 292.5 || angle <= 67.5

                        onHaptic()
                        onDirectionChange(up, down, left, right)
                        tryAwaitRelease()
                        onDirectionChange(false, false, false, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // D-Pad Cross Background Shape
        val crossThickness = size * 0.36f
        Box(
            modifier = Modifier
                .width(crossThickness)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(NesDpad)
                .border(1.5.dp, NesBorder, RoundedCornerShape(6.dp))
        )
        Box(
            modifier = Modifier
                .height(crossThickness)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(NesDpad)
                .border(1.5.dp, NesBorder, RoundedCornerShape(6.dp))
        )

        // Center Pivot
        Box(
            modifier = Modifier
                .size(size * 0.28f)
                .clip(CircleShape)
                .background(NesSurfaceCard)
        )

        // Direction Indicators
        Text("▲", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
        Text("▼", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
        Text("◀", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp))
        Text("▶", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))
    }
}

@Composable
private fun RetroPillButton(
    label: String,
    testTag: String,
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag(testTag)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPressChange(true)
                        tryAwaitRelease()
                        isPressed = false
                        onPressChange(false)
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(14.dp)
                .rotate(-20f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isPressed) NesRed else NesDpad)
                .border(1.dp, NesBorder, RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = NesTextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ActionButtonsDeck(
    buttonSize: Dp,
    onButtonChange: (Int, Boolean) -> Unit,
    onHaptic: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        // B Button (Left, Lower)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 12.dp)
        ) {
            TurboButton(
                label = "TB",
                testTag = "button_turbo_b",
                onTurboPulse = { onButtonChange(Controller.BUTTON_B, it) },
                onHaptic = onHaptic
            )
            Spacer(modifier = Modifier.height(8.dp))
            RoundActionButton(
                label = "B",
                size = buttonSize,
                testTag = "button_b",
                onPressChange = { pressed ->
                    if (pressed) onHaptic()
                    onButtonChange(Controller.BUTTON_B, pressed)
                }
            )
        }

        // A Button (Right, Upper)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-8).dp)
        ) {
            TurboButton(
                label = "TA",
                testTag = "button_turbo_a",
                onTurboPulse = { onButtonChange(Controller.BUTTON_A, it) },
                onHaptic = onHaptic
            )
            Spacer(modifier = Modifier.height(8.dp))
            RoundActionButton(
                label = "A",
                size = buttonSize,
                testTag = "button_a",
                onPressChange = { pressed ->
                    if (pressed) onHaptic()
                    onButtonChange(Controller.BUTTON_A, pressed)
                }
            )
        }
    }
}

@Composable
private fun RoundActionButton(
    label: String,
    size: Dp,
    testTag: String,
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .testTag(testTag)
            .clip(CircleShape)
            .background(if (isPressed) NesRedDark else NesRed)
            .border(2.dp, NesBorder, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPressChange(true)
                        tryAwaitRelease()
                        isPressed = false
                        onPressChange(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

@Composable
private fun TurboButton(
    label: String,
    testTag: String,
    onTurboPulse: (Boolean) -> Unit,
    onHaptic: () -> Unit
) {
    var isHolding by remember { mutableStateOf(false) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            onHaptic()
            while (isActive && isHolding) {
                onTurboPulse(true)
                delay(33) // ~30Hz turbo oscillation
                onTurboPulse(false)
                delay(33)
            }
            onTurboPulse(false)
        }
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .testTag(testTag)
            .clip(CircleShape)
            .background(if (isHolding) NesGold else NesSurfaceCard)
            .border(1.dp, NesBorder, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        tryAwaitRelease()
                        isHolding = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isHolding) Color.Black else NesTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
