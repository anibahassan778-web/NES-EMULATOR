/* NES emulator By ArDev */
package com.example.nes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nes.data.*
import com.example.ui.theme.*

enum class Vita3kSettingsTab(val label: String, val icon: ImageVector) {
    CORE("Core", Icons.Default.Memory),
    CPU("CPU", Icons.Default.Speed),
    GRAPHICS("Graphics", Icons.Default.GraphicEq),
    AUDIO("Audio", Icons.Default.VolumeUp),
    CONTROLS("Controls", Icons.Default.SportsEsports),
    SAVES("Save States", Icons.Default.Bookmark)
}

/**
 * Settings Screen faithfully replicating Screenshot 2 (Vita3K Emulator).
 * Features:
 * - Top App Bar with back navigation, Settings title, and search/more icons
 * - Horizontal scrolling category tabs with Vita3K Amber highlights
 * - Section Cards with setting items, subtitles, info icons (ℹ️), and segmented choice pills
 * - Vita3K Amber Switch controls and volume/opacity sliders
 * - Full alignment with "Nes Emulator ArDev"
 * NES emulator By ArDev
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Vita3kSettingsScreen(
    settings: EmulatorSettings,
    onUpdateSettings: (EmulatorSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var activeTab by remember { mutableStateOf(Vita3kSettingsTab.GRAPHICS) }
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Vita3kDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                    IconButton(onClick = { /* More */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Vita3kDark
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Tab Chips (Screenshot 2: Core, CPU, Graphics, Audio, Controls, Saves)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Vita3kSettingsTab.values()) { tab ->
                    val isSelected = tab == activeTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Vita3kAmber.copy(alpha = 0.22f) else Vita3kPillInactive)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Vita3kAmber else Color(0xFF2C2F3D),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { activeTab = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) Vita3kAmber else NesTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tab.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Vita3kAmber else NesTextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Settings Content Cards (Screenshot 2 style)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (activeTab) {
                    Vita3kSettingsTab.GRAPHICS -> {
                        item {
                            Vita3kCategoryCard(title = "Renderer") {
                                // Item 1: Backend Renderer
                                Vita3kSettingItem(
                                    title = "Backend Renderer",
                                    subtitle = "Hardware 60 FPS",
                                    onInfoClick = {
                                        infoDialogTitle = "Backend Renderer"
                                        infoDialogText = "Hardware accelerated 60 FPS NES PPU 2C02 rendering pipeline with zero runtime allocations."
                                    }
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Vita3kPill(
                                            label = "Standard",
                                            isSelected = false,
                                            onClick = {}
                                        )
                                        Vita3kPill(
                                            label = "Hardware 60FPS",
                                            isSelected = true,
                                            onClick = {}
                                        )
                                    }
                                }

                                Vita3kDivider()

                                // Item 2: Screen Filter
                                Vita3kSettingItem(
                                    title = "Screen Filter",
                                    subtitle = if (settings.smoothFilter) "Bilinear" else "Nearest",
                                    onInfoClick = {
                                        infoDialogTitle = "Screen Filter"
                                        infoDialogText = "Nearest gives crisp retro pixels, Bilinear smooths texture edges."
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Vita3kPill(
                                            label = "Nearest",
                                            isSelected = !settings.smoothFilter,
                                            onClick = { onUpdateSettings(settings.copy(smoothFilter = false)) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Vita3kPill(
                                            label = "Bilinear",
                                            isSelected = settings.smoothFilter,
                                            onClick = { onUpdateSettings(settings.copy(smoothFilter = true)) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Vita3kDivider()

                                // Item 3: Aspect Ratio Mode
                                Vita3kSettingItem(
                                    title = "Aspect Ratio",
                                    subtitle = when (settings.aspectRatio) {
                                        AspectRatioMode.ORIGINAL_4_3 -> "Original (4:3)"
                                        AspectRatioMode.SQUARE_1_1 -> "Square (1:1)"
                                        AspectRatioMode.FULL_STRETCH -> "Full Stretch"
                                    },
                                    onInfoClick = {
                                        infoDialogTitle = "Aspect Ratio"
                                        infoDialogText = "Original 4:3 matches authentic CRT TVs. Square 1:1 provides uniform pixel sizing."
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AspectRatioMode.values().forEach { mode ->
                                            Vita3kPill(
                                                label = when (mode) {
                                                    AspectRatioMode.ORIGINAL_4_3 -> "4:3"
                                                    AspectRatioMode.SQUARE_1_1 -> "1:1"
                                                    AspectRatioMode.FULL_STRETCH -> "Stretch"
                                                },
                                                isSelected = settings.aspectRatio == mode,
                                                onClick = { onUpdateSettings(settings.copy(aspectRatio = mode)) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Vita3kDivider()

                                // Item 4: Surface Sync / FPS Cap
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Disable Surface Sync",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = NesTextMuted,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        infoDialogTitle = "Disable Surface Sync"
                                                        infoDialogText = "Allows uncapped rendering and ultra low touch-to-display latency on 120Hz displays."
                                                    }
                                            )
                                        }
                                        Text(
                                            text = "Enabled (Optimal 60 FPS)",
                                            fontSize = 11.sp,
                                            color = NesTextMuted
                                        )
                                    }

                                    Switch(
                                        checked = true,
                                        onCheckedChange = {},
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Vita3kAmber
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Vita3kSettingsTab.CONTROLS -> {
                        item {
                            Vita3kCategoryCard(title = "Touch Overlay") {
                                // Gamepad Size
                                Vita3kSettingItem(
                                    title = "Gamepad Size",
                                    subtitle = settings.controlSize.name,
                                    onInfoClick = {
                                        infoDialogTitle = "Gamepad Size"
                                        infoDialogText = "Scales the minimalist white outline D-pad, analog rings, and buttons."
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ControlSize.values().forEach { size ->
                                            Vita3kPill(
                                                label = size.name,
                                                isSelected = settings.controlSize == size,
                                                onClick = { onUpdateSettings(settings.copy(controlSize = size)) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Vita3kDivider()

                                // Opacity Slider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Overlay Opacity", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        Text("${(settings.controlsOpacity * 100).toInt()}%", fontSize = 13.sp, color = Vita3kAmber, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = settings.controlsOpacity,
                                        onValueChange = { onUpdateSettings(settings.copy(controlsOpacity = it)) },
                                        valueRange = 0.2f..1.0f,
                                        colors = SliderDefaults.colors(thumbColor = Vita3kAmber, activeTrackColor = Vita3kAmber)
                                    )
                                }

                                Vita3kDivider()

                                // Haptic Vibration
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Haptic Feedback", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        Text("Tactile impulse when touching buttons", fontSize = 11.sp, color = NesTextMuted)
                                    }
                                    Switch(
                                        checked = settings.hapticFeedback,
                                        onCheckedChange = { onUpdateSettings(settings.copy(hapticFeedback = it)) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Vita3kAmber
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Vita3kSettingsTab.AUDIO -> {
                        item {
                            Vita3kCategoryCard(title = "APU Audio Engine") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Enable 2A03 APU Sound", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        Text("Synthesizes Pulse, Triangle, and Noise channels", fontSize = 11.sp, color = NesTextMuted)
                                    }
                                    Switch(
                                        checked = settings.audioEnabled,
                                        onCheckedChange = { onUpdateSettings(settings.copy(audioEnabled = it)) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Vita3kAmber
                                        )
                                    )
                                }

                                if (settings.audioEnabled) {
                                    Vita3kDivider()
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Audio Volume", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                            Text("${(settings.audioVolume * 100).toInt()}%", fontSize = 13.sp, color = Vita3kAmber, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = settings.audioVolume,
                                            onValueChange = { onUpdateSettings(settings.copy(audioVolume = it)) },
                                            colors = SliderDefaults.colors(thumbColor = Vita3kAmber, activeTrackColor = Vita3kAmber)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Vita3kSettingsTab.CORE, Vita3kSettingsTab.CPU -> {
                        item {
                            Vita3kCategoryCard(title = "NES Core Specifications") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Vita3kInfoRow("Core Architecture", "MOS Technology 6502 (2A03)")
                                    Vita3kInfoRow("PPU Pipeline", "Ricoh 2C02 (256x240 @ 60 FPS)")
                                    Vita3kInfoRow("Supported Mappers", "0 (NROM), 1 (MMC1), 2, 3, 4 (MMC3), 7 (AxROM)")
                                    Vita3kInfoRow("Developer Branding", "Nes Emulator ArDev")
                                    Vita3kInfoRow("Memory Footprint", "Zero-Allocation Low Memory Profile")
                                }
                            }
                        }
                    }

                    Vita3kSettingsTab.SAVES -> {
                        item {
                            Vita3kCategoryCard(title = "Multi-Slot Save Engine") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Vita3kInfoRow("Storage Backend", "Room SQLite Database")
                                    Vita3kInfoRow("Snapshot Slots", "10 Slots Per Game")
                                    Vita3kInfoRow("Format", "Binary Hardware State (NESSAVE_V3)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Info Dialog
    if (infoDialogTitle != null && infoDialogText != null) {
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null; infoDialogText = null },
            title = { Text(infoDialogTitle!!, color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text(infoDialogText!!, color = NesTextSecondary) },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null; infoDialogText = null }) {
                    Text("OK", color = Vita3kAmber, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Vita3kCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun Vita3kCategoryCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Vita3kCard)
                .border(1.dp, Vita3kCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun Vita3kSettingItem(
    title: String,
    subtitle: String,
    onInfoClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = NesTextMuted
                )
            }
            IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = NesTextMuted, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun Vita3kPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Vita3kAmber else Vita3kPillInactive)
            .border(
                1.dp,
                if (isSelected) Vita3kAmber else Color(0xFF2B2E3D),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else Color.White
        )
    }
}

@Composable
private fun Vita3kDivider() {
    HorizontalDivider(
        color = Color(0xFF222532),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun Vita3kInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = NesTextMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
