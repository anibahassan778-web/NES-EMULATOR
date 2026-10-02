/* NES emulator By ArDev */
package com.example.nes.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nes.data.AspectRatioMode
import com.example.nes.data.RomItem
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Modern High-Performance NES In-Game Screen.
 * Features:
 * - Clean, clutter-free viewport with sleek Floating Action Pill
 * - Modal In-Game Sheet for all actions (No overlapping menus or conflicting toolbars)
 * - Hardware-accelerated CRT scanlines toggle with zero memory footprint
 * - 10-Slot Save State selector with instant save & restore
 * - 3D Tactile Virtual Controller (Nostalgia.NES & Delta style)
 * - Watermark: NES emulator By ArDev
 * NES emulator By ArDev
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    rom: RomItem,
    viewModel: EmulatorViewModel,
    onExit: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isFastForward by viewModel.isFastForward.collectAsState()
    val fps by viewModel.currentFps.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    var showMenuSheet by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableStateOf(1) }
    var crtScanlinesEnabled by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Collect all save states for current ROM to show slot status in sheet
    val romSaves by viewModel.saveStateManager.getStatesForRom(rom.id).collectAsState(initial = emptyList())
    val occupiedSlots = remember(romSaves) { romSaves.map { it.slotIndex }.toSet() }

    // Framebuffer Bitmap (256x240)
    val bitmap = remember {
        Bitmap.createBitmap(256, 240, Bitmap.Config.ARGB_8888)
    }
    var frameVersion by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        viewModel.emulator.setOnFrameRenderedListener { framePixels ->
            bitmap.setPixels(framePixels, 0, 256, 0, 0, 256, 240)
            frameVersion++
        }
        onDispose {
            viewModel.emulator.setOnFrameRenderedListener {}
        }
    }

    BackHandler {
        if (showMenuSheet) {
            showMenuSheet = false
            if (isPaused) viewModel.togglePause()
        } else {
            // Open menu sheet when back is pressed
            if (!isPaused) viewModel.togglePause()
            showMenuSheet = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD: Settings & Exit (Top-Left) + Title & FPS (Top-Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TOP LEFT: Exit (Back to Menu) & Settings Icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Back / Exit to Menu
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xCC181A24))
                            .border(1.dp, Color(0xFF2C3042), CircleShape)
                            .clickable { onExit() }
                            .padding(8.dp)
                            .testTag("in_game_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit to Menu",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings Button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xCC181A24))
                            .border(1.dp, Color(0xFF2C3042), CircleShape)
                            .clickable {
                                if (!isPaused) viewModel.togglePause()
                                showMenuSheet = true
                            }
                            .padding(8.dp)
                            .testTag("in_game_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Vita3kAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // TOP RIGHT: Title & 60 FPS Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xCC181A24))
                            .border(1.dp, Color(0xFF2C3042), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = rom.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xCC181A24))
                            .border(1.dp, Color(0xFF2C3042), RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${fps} FPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (fps >= 55) NesGreen else NesRedBright
                        )
                    }
                }
            }

            // Game Canvas Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = if (isLandscape) 32.dp else 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .border(1.5.dp, Color(0xFF232635), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("nes_game_canvas")
                    ) {
                        if (frameVersion < 0) return@Canvas

                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val (destW, destH) = when (settings.aspectRatio) {
                            AspectRatioMode.ORIGINAL_4_3 -> {
                                val targetAspect = 4f / 3f
                                if (canvasWidth / canvasHeight > targetAspect) {
                                    val h = canvasHeight
                                    val w = h * targetAspect
                                    Pair(w, h)
                                } else {
                                    val w = canvasWidth
                                    val h = w / targetAspect
                                    Pair(w, h)
                                }
                            }
                            AspectRatioMode.SQUARE_1_1 -> {
                                val minDim = kotlin.math.min(canvasWidth, canvasHeight)
                                Pair(minDim, minDim)
                            }
                            AspectRatioMode.FULL_STRETCH -> Pair(canvasWidth, canvasHeight)
                        }

                        val left = (canvasWidth - destW) / 2f
                        val top = (canvasHeight - destH) / 2f

                        val paint = Paint().apply {
                            isFilterBitmap = settings.smoothFilter
                            isDither = false
                        }

                        val srcRect = Rect(0, 0, 256, 240)
                        val dstRect = Rect(
                            left.toInt(),
                            top.toInt(),
                            (left + destW).toInt(),
                            (top + destH).toInt()
                        )

                        drawContext.canvas.nativeCanvas.drawBitmap(bitmap, srcRect, dstRect, paint)

                        // CRT Scanline Shader Effect (Hardware accelerated)
                        if (crtScanlinesEnabled) {
                            var lineY = top
                            while (lineY < top + destH) {
                                drawLine(
                                    color = Color.Black.copy(alpha = 0.35f),
                                    start = Offset(left, lineY),
                                    end = Offset(left + destW, lineY),
                                    strokeWidth = 1.6f
                                )
                                lineY += 4.5f
                            }
                        }
                    }

                    // Simple Pause Banner
                    if (isPaused && !showMenuSheet) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { viewModel.togglePause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NesRed)
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "PAUSED",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap to resume or tap menu pill above",
                                    fontSize = 11.sp,
                                    color = NesTextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Pure NES Minimalist Virtual Controller
            VirtualController(
                settings = settings,
                onButtonChange = { btn, pressed ->
                    viewModel.setButtonPressed(btn, pressed)
                },
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // In-Game Status Notification
        AnimatedVisibility(
            visible = statusMsg != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-20).dp)
        ) {
            statusMsg?.let { msg ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE61B1E2B))
                        .border(1.dp, NesGold, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Modern In-Game Modal Bottom Sheet (Unified Control Center)
        if (showMenuSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showMenuSheet = false
                    if (isPaused) viewModel.togglePause()
                },
                containerColor = Color(0xF513151E),
                scrimColor = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF383C52))
                    )
                },
                modifier = Modifier.testTag("in_game_menu_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sheet Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = rom.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Nes Emulator ArDev • 60 FPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Vita3kAmber
                            )
                        }

                        Button(
                            onClick = {
                                showMenuSheet = false
                                if (isPaused) viewModel.togglePause()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Vita3kAmber),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    // Quick Action Toggles Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Fast Forward (2x)
                        FilterChip(
                            selected = isFastForward,
                            onClick = { viewModel.toggleFastForward() },
                            leadingIcon = {
                                Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Fast 2x", fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Vita3kAmber,
                                selectedLabelColor = Color.Black,
                                containerColor = Vita3kPillInactive,
                                labelColor = NesTextMuted
                            ),
                            border = null,
                            modifier = Modifier.weight(1f)
                        )

                        // CRT Scanlines
                        FilterChip(
                            selected = crtScanlinesEnabled,
                            onClick = { crtScanlinesEnabled = !crtScanlinesEnabled },
                            leadingIcon = {
                                Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("CRT Filter", fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Vita3kAmber,
                                selectedLabelColor = Color.Black,
                                containerColor = Vita3kPillInactive,
                                labelColor = NesTextMuted
                            ),
                            border = null,
                            modifier = Modifier.weight(1f)
                        )

                        // Reset Game
                        Button(
                            onClick = {
                                viewModel.resetGame()
                                showMenuSheet = false
                                if (isPaused) viewModel.togglePause()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222534)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    // Save States Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1B1E2B))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Save State Slots (1 - 10)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (occupiedSlots.contains(selectedSlot)) "Slot $selectedSlot: Saved" else "Slot $selectedSlot: Empty",
                                    fontSize = 11.sp,
                                    color = if (occupiedSlots.contains(selectedSlot)) NesGold else NesTextMuted
                                )
                            }

                            // Slot Selector Bar
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(10) { index ->
                                    val slot = index + 1
                                    val isSelected = selectedSlot == slot
                                    val isOccupied = occupiedSlots.contains(slot)

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> NesRed
                                                    isOccupied -> Color(0xFF2D3246)
                                                    else -> Color(0xFF161822)
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color.White else if (isOccupied) NesGold.copy(alpha = 0.5f) else Color(0xFF282C3D),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedSlot = slot },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$slot",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else if (isOccupied) NesGold else NesTextMuted
                                        )
                                    }
                                }
                            }

                            // Save & Load Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.saveCurrentState(selectedSlot)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NesGoldDark),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Slot $selectedSlot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.loadCurrentState(selectedSlot)
                                        showMenuSheet = false
                                        if (isPaused) viewModel.togglePause()
                                    },
                                    enabled = occupiedSlots.contains(selectedSlot),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2C3044),
                                        disabledContainerColor = Color(0xFF1B1D28)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load Slot $selectedSlot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Exit to Library
                    Button(
                        onClick = {
                            showMenuSheet = false
                            onExit()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242738)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = NesTextMuted, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exit to Library", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
