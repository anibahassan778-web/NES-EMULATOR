/* NES emulator By ArDev */
package com.example.nes.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
 * Classic NES Retro Arcade In-Game Screen.
 * Integrates:
 * - 60 FPS NES Canvas with configurable CRT / Pixel Perfect scaling
 * - Authentic NES Virtual Controller (D-Pad, A, B, Turbo A, Turbo B, Select, Start)
 * - Multi-slot Save State Management system (Slots 1 to 10)
 * - Quick Save and Quick Load shortcuts
 * - Persistent watermark: NES emulator By ArDev
 */
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

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSaveStateManager by remember { mutableStateOf(false) }
    var activeQuickSlot by remember { mutableStateOf(1) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Framebuffer Bitmap (256x240)
    val bitmap = remember {
        Bitmap.createBitmap(256, 240, Bitmap.Config.ARGB_8888)
    }
    var frameVersion by remember { mutableStateOf(0L) }

    // Connect to emulator frame output
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
        if (showSaveStateManager) {
            showSaveStateManager = false
        } else {
            onExit()
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = settings,
            onSaveSettings = { viewModel.updateSettings(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showSaveStateManager) {
        SaveStateManagerDialog(
            rom = rom,
            viewModel = viewModel,
            isInGame = true,
            onDismiss = { showSaveStateManager = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NesDarkBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Retro Arcade Top Bar
            RetroArcadeTopBar(
                title = rom.title,
                fps = fps,
                isPaused = isPaused,
                isFastForward = isFastForward,
                onBack = onExit,
                onTogglePause = { viewModel.togglePause() },
                onToggleFastForward = { viewModel.toggleFastForward() },
                onReset = { viewModel.resetGame() },
                onOpenSaveManager = { showSaveStateManager = true },
                onOpenSettings = { showSettingsDialog = true }
            )

            // Game Screen Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = if (isLandscape) 32.dp else 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Bezel Frame
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .border(2.dp, NesSurfaceCard, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("nes_game_canvas")
                    ) {
                        // Suppress unused warning while ensuring recomposition on new frames
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
                    }

                    // Pause Overlay
                    if (isPaused) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
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
                                        fontSize = 18.sp,
                                        letterSpacing = 2.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { showSaveStateManager = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NesSurfaceCard)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Saves", tint = NesGold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إدارة التقدم (Save States)", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Save / Load Slot Bar
            QuickSaveSlotBar(
                activeSlot = activeQuickSlot,
                onSelectSlot = { activeQuickSlot = it },
                onQuickSave = {
                    coroutineScope.launch {
                        val ok = viewModel.saveStateManager.save(rom.id, activeQuickSlot, viewModel.emulator)
                        viewModel.showMessage(if (ok) "تم الحفظ السريع في الخانة $activeQuickSlot" else "فشل الحفظ السريع")
                    }
                },
                onQuickLoad = {
                    coroutineScope.launch {
                        val ok = viewModel.saveStateManager.load(rom.id, activeQuickSlot, viewModel.emulator)
                        viewModel.showMessage(if (ok) "تم التحميل السريع من الخانة $activeQuickSlot" else "لا توجد حالة محفوظة بالخانة $activeQuickSlot")
                    }
                },
                onOpenFullManager = { showSaveStateManager = true }
            )

            // Authentic NES Controller
            VirtualController(
                settings = settings,
                onButtonChange = { buttonIndex, pressed ->
                    viewModel.setButtonPressed(buttonIndex, pressed)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isLandscape) 4.dp else 16.dp)
            )
        }

        // On-screen message toast
        statusMsg?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NesSurfaceCard.copy(alpha = 0.95f))
                    .border(1.dp, NesGold, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = msg, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun RetroArcadeTopBar(
    title: String,
    fps: Int,
    isPaused: Boolean,
    isFastForward: Boolean,
    onBack: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleFastForward: () -> Unit,
    onReset: () -> Unit,
    onOpenSaveManager: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NesSurface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp).testTag("in_game_back_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NES emulator By ArDev",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = NesGold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${fps} FPS",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = if (fps >= 55) NesGreen else NesRedBright
                    )
                }
            }
        }

        // Action Toolbar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Save State Manager
            IconButton(
                onClick = onOpenSaveManager,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NesSurfaceCard)
                    .testTag("in_game_save_manager_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save States", tint = NesGold, modifier = Modifier.size(18.dp))
            }

            // Fast Forward (2x)
            IconButton(
                onClick = onToggleFastForward,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isFastForward) NesGoldDark else NesSurfaceCard)
                    .testTag("in_game_fast_forward_button")
            ) {
                Icon(Icons.Default.FastForward, contentDescription = "Fast Forward", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            // Reset Game
            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NesSurfaceCard)
                    .testTag("in_game_reset_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            // Pause / Play
            IconButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isPaused) NesRed else NesSurfaceCard)
                    .testTag("in_game_pause_button")
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Settings
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NesSurfaceCard)
                    .testTag("in_game_settings_button")
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NesTextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun QuickSaveSlotBar(
    activeSlot: Int,
    onSelectSlot: (Int) -> Unit,
    onQuickSave: () -> Unit,
    onQuickLoad: () -> Unit,
    onOpenFullManager: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Slot selector (1..5 quick pills)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Slot:", color = NesTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            (1..5).forEach { slot ->
                val isSelected = slot == activeSlot
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) NesGold else NesSurfaceCard)
                        .clickable { onSelectSlot(slot) }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$slot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        // Quick Actions
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Quick Save
            Button(
                onClick = onQuickSave,
                colors = ButtonDefaults.buttonColors(containerColor = NesRed),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Q-Save", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Quick Load
            Button(
                onClick = onQuickLoad,
                colors = ButtonDefaults.buttonColors(containerColor = NesGreen),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Q-Load", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            // Open Full 10-Slot Manager
            IconButton(
                onClick = onOpenFullManager,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NesSurfaceCard)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "All Slots", tint = NesGold, modifier = Modifier.size(16.dp))
            }
        }
    }
}
