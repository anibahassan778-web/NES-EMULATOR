/* NES emulator By ArDev */
package com.example.nes.core

import android.content.Context
import com.example.nes.data.NesDatabase
import com.example.nes.data.SaveStateEntity
import kotlinx.coroutines.flow.Flow
import java.io.*

/**
 * Manages 10 persistent local storage Save State slots per ROM.
 * Integrates binary hardware snapshot serialization with Room Database persistence.
 * NES emulator By ArDev
 */
class SaveStateManager(private val context: Context) {

    private val db = NesDatabase.getDatabase(context)
    private val dao = db.saveStateDao()

    companion object {
        const val MAX_SLOTS = 10
        private const val MAGIC_HEADER = "NESSAVE_V3"
    }

    private fun getSavesDirectory(): File {
        val dir = File(context.filesDir, "savestates")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getSaveFile(romKey: String, slot: Int): File {
        val safeKey = romKey.replace(Regex("[^a-zA-Z0-9_]"), "_")
        return File(getSavesDirectory(), "${safeKey}_slot${slot}.sav")
    }

    fun getStatesForRom(romKey: String): Flow<List<SaveStateEntity>> {
        return dao.getStatesForRom(romKey)
    }

    suspend fun getStatesListForRom(romKey: String): List<SaveStateEntity> {
        return dao.getStatesListForRom(romKey)
    }

    suspend fun hasSave(romKey: String, slot: Int): Boolean {
        val entity = dao.getStateBySlot(romKey, slot)
        if (entity != null) {
            val file = File(entity.filePath)
            return file.exists() && file.length() > 0
        }
        return getSaveFile(romKey, slot).exists()
    }

    suspend fun save(
        romKey: String,
        slot: Int,
        emulator: NesEmulator,
        customName: String? = null
    ): Boolean {
        return try {
            val file = getSaveFile(romKey, slot)
            FileOutputStream(file).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    DataOutputStream(bos).use { dos ->
                        dos.writeUTF(MAGIC_HEADER)
                        dos.writeLong(System.currentTimeMillis())
                        dos.writeInt(slot)

                        // 1. CPU State
                        val cpu = emulator.cpu
                        dos.writeInt(cpu.a)
                        dos.writeInt(cpu.x)
                        dos.writeInt(cpu.y)
                        dos.writeInt(cpu.pc)
                        dos.writeInt(cpu.sp)
                        dos.writeInt(cpu.status)
                        dos.writeLong(cpu.cycles)
                        dos.writeInt(cpu.remainingCycles)
                        dos.writeBoolean(cpu.nmiPending)
                        dos.writeBoolean(cpu.irqPending)

                        // 2. Bus State (Internal 2KB RAM)
                        emulator.bus.saveState(dos)

                        // 3. PPU State (VRAM, Palette, OAM, Registers)
                        emulator.ppu.saveState(dos)

                        // 4. APU State
                        emulator.apu.saveState(dos)

                        // 5. Cartridge Mapper State (PRG/CHR Banking, RAM)
                        emulator.cartridge?.mapper?.saveState(dos)

                        dos.flush()
                    }
                }
            }

            val sizeKb = (file.length() / 1024).coerceAtLeast(1)
            val name = customName ?: "Slot $slot"

            // Persist metadata to Room Database
            val entity = SaveStateEntity(
                romId = romKey,
                slotIndex = slot,
                slotName = name,
                timestamp = System.currentTimeMillis(),
                filePath = file.absolutePath,
                fileSizeFormatted = "$sizeKb KB"
            )
            dao.insertOrUpdate(entity)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun load(romKey: String, slot: Int, emulator: NesEmulator): Boolean {
        val file = getSaveFile(romKey, slot)
        if (!file.exists()) return false

        return try {
            FileInputStream(file).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    DataInputStream(bis).use { dis ->
                        val header = dis.readUTF()
                        if (header != MAGIC_HEADER) return false
                        val _timestamp = dis.readLong()
                        val _slot = dis.readInt()

                        // 1. CPU State
                        val cpu = emulator.cpu
                        cpu.a = dis.readInt()
                        cpu.x = dis.readInt()
                        cpu.y = dis.readInt()
                        cpu.pc = dis.readInt()
                        cpu.sp = dis.readInt()
                        cpu.status = dis.readInt()
                        cpu.cycles = dis.readLong()
                        cpu.remainingCycles = dis.readInt()
                        cpu.nmiPending = dis.readBoolean()
                        cpu.irqPending = dis.readBoolean()

                        // 2. Bus State
                        emulator.bus.loadState(dis)

                        // 3. PPU State
                        emulator.ppu.loadState(dis)

                        // 4. APU State
                        emulator.apu.loadState(dis)

                        // 5. Cartridge Mapper State
                        emulator.cartridge?.mapper?.loadState(dis)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun delete(romKey: String, slot: Int): Boolean {
        return try {
            val file = getSaveFile(romKey, slot)
            if (file.exists()) {
                file.delete()
            }
            dao.deleteBySlot(romKey, slot)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
