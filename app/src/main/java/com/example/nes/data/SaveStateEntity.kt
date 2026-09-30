/* NES emulator By ArDev */
package com.example.nes.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Database Entity representing a persistent save state for a game ROM.
 * Supports multi-slot management with local storage file path, custom name, and timestamp.
 * NES emulator By ArDev
 */
@Entity(
    tableName = "save_states",
    indices = [Index(value = ["romId", "slotIndex"], unique = true)]
)
data class SaveStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val romId: String,
    val slotIndex: Int, // 1 to 10
    val slotName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val filePath: String,
    val fileSizeFormatted: String = ""
)
