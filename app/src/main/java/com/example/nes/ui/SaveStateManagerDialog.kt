/* NES emulator By ArDev */
package com.example.nes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nes.core.SaveStateManager
import com.example.nes.data.RomItem
import com.example.nes.data.SaveStateEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Interactive Save State Management Dialog for Local Storage.
 * Allows viewing all 10 slots with timestamps, status, saving, loading, and deletion.
 * NES emulator By ArDev
 */
@Composable
fun SaveStateManagerDialog(
    rom: RomItem,
    viewModel: EmulatorViewModel,
    isInGame: Boolean = true,
    onLoadStateSuccess: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val savedStates by viewModel.saveStateManager.getStatesForRom(rom.id).collectAsState(initial = emptyList())
    var slotToDelete by remember { mutableStateOf<Int?>(null) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()) }

    // Map existing entities by slotIndex
    val statesMap = remember(savedStates) {
        savedStates.associateBy { it.slotIndex }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NesSurface,
            border = androidx.compose.foundation.BorderStroke(2.dp, NesBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("save_state_manager_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header with Arcade Gold & Watermark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Save State",
                                tint = NesGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إدارة حفظ التقدم (Save States)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "NES emulator By ArDev • ${rom.title}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NesGoldLight,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_save_manager_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NesTextSecondary)
                    }
                }

                // In-dialog feedback banner
                statusFeedback?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NesRedDark.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = msg, color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 10 Save Slots List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items((1..SaveStateManager.MAX_SLOTS).toList()) { slotIndex ->
                        val stateEntity = statesMap[slotIndex]
                        SaveSlotCard(
                            slotIndex = slotIndex,
                            stateEntity = stateEntity,
                            isInGame = isInGame,
                            dateFormat = dateFormat,
                            onSave = {
                                coroutineScope.launch {
                                    val success = viewModel.saveStateManager.save(
                                        romKey = rom.id,
                                        slot = slotIndex,
                                        emulator = viewModel.emulator,
                                        customName = "Slot $slotIndex"
                                    )
                                    statusFeedback = if (success) {
                                        "تم حفظ التقدم في الخانة $slotIndex بنجاح!"
                                    } else {
                                        "فشل حفظ التقدم في الخانة $slotIndex"
                                    }
                                }
                            },
                            onLoad = {
                                coroutineScope.launch {
                                    val success = viewModel.saveStateManager.load(
                                        romKey = rom.id,
                                        slot = slotIndex,
                                        emulator = viewModel.emulator
                                    )
                                    if (success) {
                                        statusFeedback = "تم استرجاع التقدم من الخانة $slotIndex!"
                                        onLoadStateSuccess?.invoke()
                                    } else {
                                        statusFeedback = "فشل تحميل الخانة $slotIndex"
                                    }
                                }
                            },
                            onDelete = {
                                slotToDelete = slotIndex
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer with Watermark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NES emulator By ArDev",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NesGold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("إغلاق (Close)", color = NesTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    slotToDelete?.let { slot ->
        AlertDialog(
            onDismissRequest = { slotToDelete = null },
            title = {
                Text("تأكيد الحذف", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في حذف بيانات الحفظ للخانة $slot؟ لا يمكن التراجع عن هذا الإجراء.",
                    color = NesTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.saveStateManager.delete(rom.id, slot)
                            statusFeedback = "تم حذف الخانة $slot بنجاح"
                            slotToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NesRed)
                ) {
                    Text("حذف (Delete)", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { slotToDelete = null }) {
                    Text("إلغاء (Cancel)", color = NesTextSecondary)
                }
            },
            containerColor = NesSurfaceCard
        )
    }
}

@Composable
private fun SaveSlotCard(
    slotIndex: Int,
    stateEntity: SaveStateEntity?,
    isInGame: Boolean,
    dateFormat: SimpleDateFormat,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val isOccupied = stateEntity != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("save_slot_card_$slotIndex"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOccupied) NesSurfaceCard else NesDarkBackground.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isOccupied) NesBorderGlow.copy(alpha = 0.6f) else NesBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isOccupied) NesGoldDark else NesControllerGray)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SLOT $slotIndex",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOccupied) stateEntity!!.slotName else "خانة فارغة (Empty)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isOccupied) Color.White else NesTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isOccupied) {
                    val formattedDate = remember(stateEntity!!.timestamp) {
                        dateFormat.format(Date(stateEntity.timestamp))
                    }
                    Text(
                        text = "$formattedDate • ${stateEntity.fileSizeFormatted}",
                        fontSize = 11.sp,
                        color = NesTextSecondary
                    )
                } else {
                    Text(
                        text = "جاهزة لحفظ تقدم اللعبة الحالي",
                        fontSize = 11.sp,
                        color = NesTextMuted
                    )
                }
            }

            // Actions Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isInGame) {
                    // Save Button
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NesRed)
                            .testTag("save_button_slot_$slotIndex")
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isOccupied) {
                    // Load Button
                    IconButton(
                        onClick = onLoad,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NesGreen)
                            .testTag("load_button_slot_$slotIndex")
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Load",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NesDarkBackground)
                            .testTag("delete_button_slot_$slotIndex")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = NesRedBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
