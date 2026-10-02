/* NES emulator By ArDev */
package com.example.nes.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Minimalist Pure NES Virtual Controller.
 * Uncluttered layout:
 * - Left: Crisp D-Pad (Up, Down, Left, Right)
 * - Right: Pure NES [ B ] and [ A ] circular action buttons
 * - Bottom Center: Slanted [ SELECT ] and [ START ] rounded pills
 * NES emulator By ArDev
 */
@Composable
fun VirtualController(
    settings: EmulatorSettings,
    onButtonChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val hapticEnabled = settings.hapticFeedback
    val opacity = settings.controlsOpacity.coerceIn(0.2f, 1.0f)
    val outlineColor = Color.White.copy(alpha = opacity)
    val pressedFillColor = Color.White.copy(alpha = (opacity * 0.45f).coerceAtMost(0.6f))

    fun triggerHaptic() {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    val baseScale = when (settings.controlSize) {
        ControlSize.SMALL -> 0.85f
        ControlSize.NORMAL -> 1.0f
        ControlSize.LARGE -> 1.15f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("nes_pure_virtual_controller")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Controls Row: D-Pad (Left) <-----------------> [ B ] [ A ] (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: Minimalist Outline D-Pad
                Vita3kDpad(
                    outlineColor = outlineColor,
                    pressedFill = pressedFillColor,
                    size = 156.dp * baseScale,
                    onButtonChange = { btn, pressed ->
                        if (pressed) triggerHaptic()
                        onButtonChange(btn, pressed)
                    }
                )

                // RIGHT: Pure NES Two-Button Cluster [ B ] and [ A ]
                NesActionButtons(
                    outlineColor = outlineColor,
                    pressedFill = pressedFillColor,
                    buttonSize = 64.dp * baseScale,
                    onButtonChange = { btn, pressed ->
                        if (pressed) triggerHaptic()
                        onButtonChange(btn, pressed)
                    }
                )
            }

            // Bottom Center Row: [ SELECT ] and [ START ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinePillButton(
                    label = "SELECT",
                    outlineColor = outlineColor,
                    pressedFill = pressedFillColor,
                    width = 84.dp * baseScale,
                    height = 32.dp * baseScale,
                    fontSize = 11.sp,
                    onPress = { pressed ->
                        if (pressed) triggerHaptic()
                        onButtonChange(Controller.BUTTON_SELECT, pressed)
                    },
                    modifier = Modifier.testTag("btn_select")
                )

                Spacer(modifier = Modifier.width(28.dp * baseScale))

                OutlinePillButton(
                    label = "START",
                    outlineColor = outlineColor,
                    pressedFill = pressedFillColor,
                    width = 84.dp * baseScale,
                    height = 32.dp * baseScale,
                    fontSize = 11.sp,
                    onPress = { pressed ->
                        if (pressed) triggerHaptic()
                        onButtonChange(Controller.BUTTON_START, pressed)
                    },
                    modifier = Modifier.testTag("btn_start")
                )
            }
        }
    }
}

/**
 * Pure NES 2-Button Action Cluster: [ B ] and [ A ]
 * Diagonal offset matching authentic NES ergonomics.
 */
@Composable
private fun NesActionButtons(
    outlineColor: Color,
    pressedFill: Color,
    buttonSize: Dp,
    onButtonChange: (Int, Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        // [ B ] Button (Slightly lower)
        Box(modifier = Modifier.padding(top = 18.dp)) {
            NesCircleButton(
                label = "B",
                outlineColor = outlineColor,
                pressedFill = pressedFill,
                size = buttonSize,
                onPress = { pressed ->
                    onButtonChange(Controller.BUTTON_B, pressed)
                },
                modifier = Modifier.testTag("btn_b")
            )
        }

        // [ A ] Button (Slightly higher)
        Box(modifier = Modifier.padding(bottom = 18.dp)) {
            NesCircleButton(
                label = "A",
                outlineColor = outlineColor,
                pressedFill = pressedFill,
                size = buttonSize,
                onPress = { pressed ->
                    onButtonChange(Controller.BUTTON_A, pressed)
                },
                modifier = Modifier.testTag("btn_a")
            )
        }
    }
}

/**
 * Minimalist Outline D-Pad with 4 directional wings, hollow arrowheads, and center diamond.
 */
@Composable
private fun Vita3kDpad(
    outlineColor: Color,
    pressedFill: Color,
    size: Dp,
    onButtonChange: (Int, Boolean) -> Unit
) {
    var upPressed by remember { mutableStateOf(false) }
    var downPressed by remember { mutableStateOf(false) }
    var leftPressed by remember { mutableStateOf(false) }
    var rightPressed by remember { mutableStateOf(false) }

    fun updateTouch(offset: Offset, boxSize: Float, isDown: Boolean) {
        if (!isDown) {
            if (upPressed) { upPressed = false; onButtonChange(Controller.BUTTON_UP, false) }
            if (downPressed) { downPressed = false; onButtonChange(Controller.BUTTON_DOWN, false) }
            if (leftPressed) { leftPressed = false; onButtonChange(Controller.BUTTON_LEFT, false) }
            if (rightPressed) { rightPressed = false; onButtonChange(Controller.BUTTON_RIGHT, false) }
            return
        }

        val cx = boxSize / 2f
        val cy = boxSize / 2f
        val dx = offset.x - cx
        val dy = offset.y - cy
        val dist = sqrt(dx * dx + dy * dy)

        val deadZone = boxSize * 0.10f
        if (dist < deadZone) {
            return
        }

        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).let { if (it < 0) it + 360.0 else it }

        // 8-way diagonal detection
        val newRight = angle in 337.5..360.0 || angle in 0.0..22.5 || angle in 22.5..67.5 || angle in 292.5..337.5
        val newDown = angle in 22.5..157.5
        val newLeft = angle in 112.5..247.5
        val newUp = angle in 202.5..337.5

        if (newUp != upPressed) { upPressed = newUp; onButtonChange(Controller.BUTTON_UP, newUp) }
        if (newDown != downPressed) { downPressed = newDown; onButtonChange(Controller.BUTTON_DOWN, newDown) }
        if (newLeft != leftPressed) { leftPressed = newLeft; onButtonChange(Controller.BUTTON_LEFT, newLeft) }
        if (newRight != rightPressed) { rightPressed = newRight; onButtonChange(Controller.BUTTON_RIGHT, newRight) }
    }

    Box(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val boxSize = size.toPx()
                        updateTouch(offset, boxSize, true)
                        tryAwaitRelease()
                        updateTouch(Offset.Zero, boxSize, false)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        updateTouch(offset, size.toPx(), true)
                    },
                    onDrag = { change, _ ->
                        updateTouch(change.position, size.toPx(), true)
                    },
                    onDragEnd = {
                        updateTouch(Offset.Zero, size.toPx(), false)
                    },
                    onDragCancel = {
                        updateTouch(Offset.Zero, size.toPx(), false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val armW = w * 0.32f
            val armL = (w - armW) / 2f
            val strokeW = 2.2f

            // Draw Cross Outline
            val path = Path().apply {
                moveTo((w - armW) / 2f, 0f)
                lineTo((w + armW) / 2f, 0f)
                lineTo((w + armW) / 2f, armL)
                lineTo(w, armL)
                lineTo(w, (h + armW) / 2f)
                lineTo((w + armW) / 2f, (h + armW) / 2f)
                lineTo((w + armW) / 2f, h)
                lineTo((w - armW) / 2f, h)
                lineTo((w - armW) / 2f, (h + armW) / 2f)
                lineTo(0f, (h + armW) / 2f)
                lineTo(0f, armL)
                lineTo((w - armW) / 2f, armL)
                close()
            }

            drawPath(path, color = outlineColor, style = Stroke(width = strokeW))

            // Highlight active wings
            if (upPressed) {
                drawRect(pressedFill, topLeft = Offset((w - armW) / 2f, 0f), size = androidx.compose.ui.geometry.Size(armW, armL))
            }
            if (downPressed) {
                drawRect(pressedFill, topLeft = Offset((w - armW) / 2f, h - armL), size = androidx.compose.ui.geometry.Size(armW, armL))
            }
            if (leftPressed) {
                drawRect(pressedFill, topLeft = Offset(0f, armL), size = androidx.compose.ui.geometry.Size(armL, armW))
            }
            if (rightPressed) {
                drawRect(pressedFill, topLeft = Offset(w - armL, armL), size = androidx.compose.ui.geometry.Size(armL, armW))
            }

            // Center Diamond
            val diamondSize = armW * 0.4f
            val dPath = Path().apply {
                moveTo(w / 2f, h / 2f - diamondSize)
                lineTo(w / 2f + diamondSize, h / 2f)
                lineTo(w / 2f, h / 2f + diamondSize)
                lineTo(w / 2f - diamondSize, h / 2f)
                close()
            }
            drawPath(dPath, color = outlineColor, style = Stroke(width = 1.6f))
        }
    }
}

@Composable
private fun NesCircleButton(
    label: String,
    outlineColor: Color,
    pressedFill: Color,
    size: Dp,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isPressed) pressedFill else Color.Transparent)
            .border(2.0.dp, outlineColor, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress(true)
                        tryAwaitRelease()
                        isPressed = false
                        onPress(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = outlineColor
        )
    }
}

@Composable
private fun OutlinePillButton(
    label: String,
    outlineColor: Color,
    pressedFill: Color,
    width: Dp,
    height: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    onPress: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(height / 2f))
            .background(if (isPressed) pressedFill else Color.Transparent)
            .border(1.8.dp, outlineColor, RoundedCornerShape(height / 2f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress(true)
                        tryAwaitRelease()
                        isPressed = false
                        onPress(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = outlineColor
        )
    }
}
