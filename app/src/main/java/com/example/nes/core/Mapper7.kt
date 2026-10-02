/* NES emulator By ArDev */
package com.example.nes.core

import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * NES Mapper 7 (AxROM).
 * Supports Battletoads, Marble Madness, Cobra Triangle, Time Lord, etc.
 * Features:
 * - 32KB switchable PRG ROM bank at $8000-$FFFF
 * - 8KB CHR RAM at $0000-$1FFF
 * - Single-Screen Mirroring (Screen A / Screen B)
 * NES emulator By ArDev
 */
class Mapper7(
    private val prgRom: ByteArray,
    private val chrRom: ByteArray,
    override var mirroring: MirroringMode,
    private val prgBanks: Int,
    private val isChrRam: Boolean
) : Mapper {

    override val mapperId: Int = 7

    private var prgBank: Int = 0
    private val totalPrg32k: Int = (prgRom.size / 32768).coerceAtLeast(1)

    override fun cpuRead(address: Int): Int {
        val addr = address and 0xFFFF
        if (addr in 0x8000..0xFFFF) {
            val offset = (prgBank % totalPrg32k) * 32768 + (addr - 0x8000)
            return if (offset in prgRom.indices) prgRom[offset].toInt() and 0xFF else 0
        }
        return -1
    }

    override fun cpuWrite(address: Int, value: Int): Boolean {
        val addr = address and 0xFFFF
        if (addr in 0x8000..0xFFFF) {
            val v8 = value and 0xFF
            prgBank = v8 and 0x07
            mirroring = if ((v8 and 0x10) != 0) {
                MirroringMode.SINGLE_SCREEN_UPPER
            } else {
                MirroringMode.SINGLE_SCREEN_LOWER
            }
            return true
        }
        return false
    }

    override fun ppuRead(address: Int): Int {
        val addr = address and 0x1FFF
        return if (addr in chrRom.indices) chrRom[addr].toInt() and 0xFF else 0
    }

    override fun ppuWrite(address: Int, value: Int): Boolean {
        val addr = address and 0x1FFF
        if (isChrRam && addr in chrRom.indices) {
            chrRom[addr] = (value and 0xFF).toByte()
            return true
        }
        return false
    }

    override fun saveState(out: DataOutputStream) {
        out.writeInt(prgBank)
        out.writeUTF(mirroring.name)
    }

    override fun loadState(inp: DataInputStream) {
        prgBank = inp.readInt()
        mirroring = MirroringMode.valueOf(inp.readUTF())
    }
}
