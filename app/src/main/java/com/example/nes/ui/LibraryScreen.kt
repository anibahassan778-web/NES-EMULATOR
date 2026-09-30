/* NES emulator By ArDev */
package com.example.nes.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nes.data.RomItem
import com.example.ui.theme.*

/**
 * Classic NES Retro Arcade Game Library & Cartridge Shelf.
 * Features:
 * - Vintage NES Front-Loader & Cartridge aesthetics (Charcoal, Crimson, Gold)
 * - Multi-slot Save State management access directly from the library
 * - Support for Mappers 0, 1, 2, and 3
 * - Watermark: NES emulator By ArDev
 */
@Composable
fun LibraryScreen(
    viewModel: EmulatorViewModel,
    onLaunchRom: (RomItem) -> Unit
) {
    val games by viewModel.games.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var activeSaveManagerRom by remember { mutableStateOf<RomItem?>(null) }

    val arabic = settings.languageArabic

    // SAF Document Picker launcher for .nes files
    val romPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importRomFromUri(it)
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = settings,
            onSaveSettings = { viewModel.updateSettings(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAboutDialog) {
        RetroAboutDialog(
            arabic = arabic,
            onDismiss = { showAboutDialog = false }
        )
    }

    activeSaveManagerRom?.let { rom ->
        SaveStateManagerDialog(
            rom = rom,
            viewModel = viewModel,
            isInGame = false,
            onLoadStateSuccess = {
                activeSaveManagerRom = null
                onLaunchRom(rom)
            },
            onDismiss = { activeSaveManagerRom = null }
        )
    }

    Scaffold(
        containerColor = NesDarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            statusMsg?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = NesSurfaceCard,
                    contentColor = Color.White,
                    action = {
                        TextButton(onClick = { viewModel.clearMessage() }) {
                            Text(if (arabic) "حسناً" else "OK", color = NesGold)
                        }
                    }
                ) {
                    Text(text = msg)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(NesDarkBackground),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Retro NES Arcade Top Header
            RetroHeaderBar(
                arabic = arabic,
                onAddRom = { romPickerLauncher.launch(arrayOf("*/*")) },
                onOpenAbout = { showAboutDialog = true },
                onOpenSettings = { showSettingsDialog = true }
            )

            // Game Cartridge Library List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("nes_cartridge_library"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(games, key = { it.id }) { rom ->
                    NesCartridgeCard(
                        rom = rom,
                        arabic = arabic,
                        onPlay = { onLaunchRom(rom) },
                        onOpenSaveStates = { activeSaveManagerRom = rom },
                        onToggleFavorite = { viewModel.toggleFavorite(rom) },
                        onDelete = { viewModel.deleteRom(rom) }
                    )
                }
            }

            // Bottom Arcade Watermark Bar
            RetroBottomStatusBar(arabic = arabic)
        }
    }
}

@Composable
private fun RetroHeaderBar(
    arabic: Boolean,
    onAddRom: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NesSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // NES Entertainment System Branding & Watermark
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NesRed)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "NES",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "CLASSIC ARCADE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "NES emulator By ArDev",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = NesGold
                )
            }
        }

        // Actions: + Add ROM, Info, Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add ROM Button
            Button(
                onClick = onAddRom,
                colors = ButtonDefaults.buttonColors(containerColor = NesRed),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_rom_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add ROM", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (arabic) "+ إضافة لعبة" else "+ Add ROM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // About (i) Button
            IconButton(
                onClick = onOpenAbout,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NesSurfaceCard)
                    .testTag("retro_about_button")
            ) {
                Icon(Icons.Default.Info, contentDescription = "About", tint = NesGold, modifier = Modifier.size(18.dp))
            }

            // Settings Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NesSurfaceCard)
                    .testTag("retro_settings_button")
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NesTextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun NesCartridgeCard(
    rom: RomItem,
    arabic: Boolean,
    onPlay: () -> Unit,
    onOpenSaveStates: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cartridge_card_${rom.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NesSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, NesBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(NesSurfaceCard, NesSurface)
                    )
                )
                .padding(14.dp)
        ) {
            // Cartridge Top Row: Game Info & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NesDarkBackground)
                            .border(1.dp, NesBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (rom.isBuiltIn) Icons.Default.Hardware else Icons.Default.SportsEsports,
                            contentDescription = "Game",
                            tint = if (rom.isBuiltIn) NesGold else NesRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = rom.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = rom.fileSizeFormatted,
                                fontSize = 11.sp,
                                color = NesTextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NesSurfaceSelected)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (rom.isBuiltIn) "HOMEBREW" else "NES iNES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NesGold
                                )
                            }
                        }
                    }
                }

                // Favorite & Delete Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (rom.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (rom.isFavorite) NesRedBright else NesTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!rom.isBuiltIn) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = NesTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Play Game & Save State Manager
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play Game Button
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(42.dp)
                        .testTag("play_game_button_${rom.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = NesRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (arabic) "تشغيل اللعبة" else "Play Game",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                // Save States Manager Button
                OutlinedButton(
                    onClick = onOpenSaveStates,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("open_save_states_button_${rom.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NesGold),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NesGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Saves", tint = NesGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (arabic) "حفظ التقدم" else "Saves",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NesGold
                    )
                }
            }
        }
    }
}

@Composable
private fun RetroBottomStatusBar(arabic: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NesSurface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NES emulator By ArDev",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = NesGold,
            modifier = Modifier.testTag("watermark_bottom_bar")
        )
        Text(
            text = if (arabic) "نظام حفظ محلي (10 خانات) • Mappers 0,1,2,3" else "10-Slot Local Saves • Mappers 0,1,2,3",
            fontSize = 10.sp,
            color = NesTextSecondary
        )
    }
}

@Composable
private fun RetroAboutDialog(
    arabic: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NesSurface,
            border = androidx.compose.foundation.BorderStroke(2.dp, NesGold),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("retro_about_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NesRed)
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NES ARCADE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "NES emulator",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "By ArDev",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NesGold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NesSurfaceCard)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (arabic) "المميزات ونظام حفظ التقدم:" else "Features & Save States:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NesGreen
                        )
                        Text("• نظام حفظ محلي يدعم حتى 10 خانات (Multi-slot Local Storage).", fontSize = 11.sp, color = NesTextPrimary)
                        Text("• محاكي متكامل بلغة Kotlin (MOS 6502, PPU 2C02 60 FPS, APU Sound).", fontSize = 11.sp, color = NesTextPrimary)
                        Text("• يد تحكم NES الأصلية مع أزرار Turbo وخيارات التخصيص.", fontSize = 11.sp, color = NesTextPrimary)
                        Text("• توافق 90%+ مع أشهر الألعاب عبر Mappers: NROM, MMC1, UxROM, CNROM.", fontSize = 11.sp, color = NesTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NesRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (arabic) "إغلاق" else "Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
