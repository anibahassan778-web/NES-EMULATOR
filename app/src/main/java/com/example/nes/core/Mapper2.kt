/* NES emulator By ArDev */
package com.example.nes.core

import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Mapper 2: UxROM / UNROM.
 * Supports classics: Contra, Mega Man 1, Castlevania, DuckTales, Metal Gear.
 * NES emulator By ArDev
 */
class Mapper2(
    private val prgRom: ByteArray,
    private val chrRom: ByteArray,
    override var mirroring: MirroringMode,
    private val prgBanks: Int,
    private val isChrRam: Boolean = true
) : Mapper {

    override val mapperId: Int = 2

    private var selectedBank: Int = 0
    private val lastBankIndex: Int = (prgRom.size / 16384).coerceAtLeast(1) - 1

    override fun cpuRead(address: Int): Int {
        return when (address) {
            in 0x8000..0xBFFF -> {
                val totalBanks = (prgRom.size / 16384).coerceAtLeast(1)
                val bank = selectedBank % totalBanks
                val offset = address - 0x8000
                val physAddr = bank * 16384 + offset
                if (physAddr in prgRom.indices) {
                    prgRom[physAddr].toInt() and 0xFF
                } else {
                    0
                }
            }
            in 0xC000..0xFFFF -> {
                val offset = address - 0xC000
                val physAddr = lastBankIndex * 16384 + offset
                if (physAddr in prgRom.indices) {
                    prgRom[physAddr].toInt() and 0xFF
                } else {
                    0
                }
            }
            else -> -1
        }
    }

    override fun cpuWrite(address: Int, value: Int): Boolean {
        if (address in 0x8000..0xFFFF) {
            selectedBank = value and 0x0F
            return true
        }
        return false
    }

    override fun ppuRead(address: Int): Int {
        if (address in 0x0000..0x1FFF) {
            return if (address in chrRom.indices) {
                chrRom[address].toInt() and 0xFF
            } else {
                0
            }
        }
        return -1
    }

    override fun ppuWrite(address: Int, value: Int): Boolean {
        if (address in 0x0000..0x1FFF && isChrRam) {
            if (address in chrRom.indices) {
                chrRom[address] = (value and 0xFF).toByte()
                return true
            }
        }
        return false
    }

    override fun saveState(out: DataOutputStream) {
        out.writeInt(selectedBank)
        out.writeBoolean(isChrRam)
        if (isChrRam) {
            out.writeInt(chrRom.size)
            out.write(chrRom)
        }
    }

    override fun loadState(inp: DataInputStream) {
        selectedBank = inp.readInt()
        val chrRamFlag = inp.readBoolean()
        if (chrRamFlag && isChrRam) {
            val chrSize = inp.readInt()
            inp.readFully(chrRom, 0, chrSize.coerceAtMost(chrRom.size))
        }
    }
}
