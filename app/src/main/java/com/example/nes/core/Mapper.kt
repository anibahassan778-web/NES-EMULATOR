package com.example.nes.core

import java.io.DataInputStream
import java.io.DataOutputStream

enum class MirroringMode {
    HORIZONTAL,
    VERTICAL,
    SINGLE_SCREEN_LOWER,
    SINGLE_SCREEN_UPPER,
    FOUR_SCREEN
}

/**
 * Interface for NES Cartridge Mappers (e.g. Mapper 0 / NROM, Mapper 1 / MMC1, etc.).
 */
interface Mapper {
    val mapperId: Int
    var mirroring: MirroringMode

    /**
     * Map CPU address (0x8000 - 0xFFFF for PRG ROM, 0x6000 - 0x7FFF for PRG RAM).
     * Returns the byte value read, or -1 if address not handled.
     */
    fun cpuRead(address: Int): Int

    /**
     * Write to CPU address space.
     * Returns true if write was handled by the mapper.
     */
    fun cpuWrite(address: Int, value: Int): Boolean

    /**
     * Map PPU address (0x0000 - 0x1FFF for CHR ROM/RAM).
     * Returns the byte value read, or -1 if address not handled.
     */
    fun ppuRead(address: Int): Int

    /**
     * Write to PPU address space (for CHR RAM).
     * Returns true if write was handled by the mapper.
     */
    fun ppuWrite(address: Int, value: Int): Boolean

    fun saveState(out: DataOutputStream) {}
    fun loadState(inp: DataInputStream) {}
}
