/* NES emulator By ArDev */
package com.example.nes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing local save states in Room.
 * NES emulator By ArDev
 */
@Dao
interface SaveStateDao {

    @Query("SELECT * FROM save_states ORDER BY timestamp DESC")
    fun getAllStates(): Flow<List<SaveStateEntity>>

    @Query("SELECT * FROM save_states WHERE romId = :romId ORDER BY slotIndex ASC")
    fun getStatesForRom(romId: String): Flow<List<SaveStateEntity>>

    @Query("SELECT * FROM save_states WHERE romId = :romId ORDER BY slotIndex ASC")
    suspend fun getStatesListForRom(romId: String): List<SaveStateEntity>

    @Query("SELECT * FROM save_states WHERE romId = :romId AND slotIndex = :slotIndex LIMIT 1")
    suspend fun getStateBySlot(romId: String, slotIndex: Int): SaveStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: SaveStateEntity): Long

    @Delete
    suspend fun deleteState(state: SaveStateEntity)

    @Query("DELETE FROM save_states WHERE romId = :romId AND slotIndex = :slotIndex")
    suspend fun deleteBySlot(romId: String, slotIndex: Int)

    @Query("DELETE FROM save_states WHERE romId = :romId")
    suspend fun deleteAllForRom(romId: String)
}
