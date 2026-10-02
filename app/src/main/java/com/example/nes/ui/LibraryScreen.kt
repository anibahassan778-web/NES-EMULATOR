/* NES emulator By ArDev */
package com.example.nes.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.example.nes.data.RomItem
import com.example.ui.theme.*

/**
 * Main Library Screen replicating Screenshot 1 (Vita3K Emulator Style).
 * - Header: Bold "Nes Emulator ArDev" and "v1.0.0 (Pro 60 FPS)"
 * - Top Action icons: Search, Settings Gear, Filter, and More
 * - List layout with square game icon, game title, ID code, and green/amber compatibility badges
 * - Floating Action Button (+) with Vita3K amber background
 * - In-app navigation to Vita3kSettingsScreen
 * NES emulator By ArDev
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: EmulatorViewModel,
    onLaunchRom: ((RomItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val games by viewModel.games.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    var showSettingsScreen by remember { mutableStateOf(false) }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterFavoritesOnly by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var selectedRomForOptions by remember { mutableStateOf<RomItem?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.importRomFromUri(uri)
            }
        }
    }

    if (showSettingsScreen) {
        Vita3kSettingsScreen(
            settings = settings,
            onUpdateSettings = { viewModel.updateSettings(it) },
            onBack = { showSettingsScreen = false }
        )
        return
    }

    val filteredGames = remember(games, searchQuery, filterFavoritesOnly) {
        games.filter { rom ->
            val matchesQuery = searchQuery.isBlank() || rom.title.contains(searchQuery, ignoreCase = true)
            val matchesFav = !filterFavoritesOnly || rom.isFavorite
            matchesQuery && matchesFav
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Vita3kDark,
        floatingActionButton = {
            // Circular Amber FAB at bottom right (Screenshot 1)
            FloatingActionButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "*/*"))
                    }
                    filePickerLauncher.launch(intent)
                },
                containerColor = Vita3kAmberDark,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("vita3k_fab_add_rom")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add ROM",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        snackbarHost = {
            statusMsg?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Vita3kCard,
                    contentColor = Color.White,
                    action = {
                        TextButton(onClick = { viewModel.clearMessage() }) {
                            Text("OK", color = Vita3kAmber)
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar (Screenshot 1: Nes Emulator ArDev + icons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and Version
                Column {
                    Text(
                        text = "Nes Emulator ArDev",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "v1.0.0 (Pro 60 FPS)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NesTextMuted
                    )
                }

                // Action Icons (Search, Settings, Filter, More)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Search
                    IconButton(onClick = { searchVisible = !searchVisible }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (searchVisible) Vita3kAmber else Color.White
                        )
                    }

                    // Settings Gear
                    IconButton(
                        onClick = { showSettingsScreen = true },
                        modifier = Modifier.testTag("top_bar_settings_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }

                    // Filter / Sort
                    IconButton(onClick = { filterFavoritesOnly = !filterFavoritesOnly }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (filterFavoritesOnly) Vita3kAmber else Color.White
                        )
                    }

                    // More ⋮
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(Vita3kCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rescan Library", color = Color.White) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.refreshGames()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Play Built-in Test ROM", color = Vita3kAmber) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.launchBuiltInRom()
                                }
                            )
                        }
                    }
                }
            }

            // Expandable Search Bar
            AnimatedVisibility(
                visible = searchVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed ROMs...", fontSize = 13.sp, color = NesTextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Vita3kAmber,
                        unfocusedBorderColor = Color(0xFF282C3D),
                        focusedContainerColor = Vita3kCard,
                        unfocusedContainerColor = Vita3kCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("vita3k_search_bar")
                )
            }

            // Games List (Screenshot 1 Layout)
            if (filteredGames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = NesTextMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching ROMs" else "No Games Installed",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the '+' button at the bottom to install NES ROM files.",
                            fontSize = 12.sp,
                            color = NesTextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredGames, key = { it.id }) { rom ->
                        Vita3kGameRow(
                            rom = rom,
                            onClick = {
                                onLaunchRom?.invoke(rom) ?: viewModel.launchRom(rom)
                            },
                            onLongClick = {
                                selectedRomForOptions = rom
                            }
                        )
                    }
                }
            }
        }
    }

    // Long-Press ROM Action Sheet
    if (selectedRomForOptions != null) {
        val targetRom = selectedRomForOptions!!
        ModalBottomSheet(
            onDismissRequest = { selectedRomForOptions = null },
            containerColor = Vita3kCard,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = targetRom.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "ID: ${if (targetRom.isBuiltIn) "NES-001" else "MAP-04-MMC3"}",
                    fontSize = 12.sp,
                    color = NesTextMuted
                )

                Button(
                    onClick = {
                        val romToPlay = targetRom
                        selectedRomForOptions = null
                        onLaunchRom?.invoke(romToPlay) ?: viewModel.launchRom(romToPlay)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Vita3kAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Play Game", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.toggleFavorite(targetRom)
                        selectedRomForOptions = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (targetRom.isFavorite) "Remove from Favorites" else "Add to Favorites",
                        color = Color.White
                    )
                }

                if (!targetRom.isBuiltIn) {
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteRom(targetRom)
                            selectedRomForOptions = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete ROM", color = Color(0xFFEF5350))
                    }
                }
            }
        }
    }
}

/**
 * Game Row replicating Screenshot 1:
 * - Square Game Thumbnail with rounded corners
 * - Title in bold white
 * - Sub-row with Code (e.g. PCSE00225 / NES-MMC3) and Green Playable Badge
 */
@Composable
private fun Vita3kGameRow(
    rom: RomItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .testTag("game_row_${rom.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Square Cartridge Icon (Screenshot 1: 48x48dp)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        colors = if (rom.isBuiltIn) {
                            listOf(Color(0xFF2C324A), Color(0xFF1B1E2D))
                        } else {
                            listOf(Color(0xFF382E1E), Color(0xFF1F1A12))
                        }
                    )
                )
                .border(1.dp, Color(0xFF2C3042), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (rom.isBuiltIn) Icons.Default.Hardware else Icons.Default.SportsEsports,
                contentDescription = null,
                tint = if (rom.isBuiltIn) Vita3kAmber else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Status
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rom.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Game ID (Screenshot 1: e.g. PCSE00225)
                val code = if (rom.isBuiltIn) "NES-001" else "NES-MAP4"
                Text(
                    text = code,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NesTextSecondary
                )

                // Compatibility Badge (Screenshot 1: [ Playable ] in green or [ Ingame- ] in amber)
                val isPlayable = rom.isBuiltIn || rom.fileSizeFormatted.isNotEmpty()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPlayable) Vita3kGreen else Vita3kIngameOrange)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPlayable) "Playable" else "Ingame-",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
