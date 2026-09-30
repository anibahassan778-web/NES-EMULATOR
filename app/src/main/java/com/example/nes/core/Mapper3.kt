/* NES emulator By ArDev */
package com.example.nes.core

import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Mapper 3: CNROM.
 * Supports classics: Cybernoid, Solomon's Key, Gradius, Pipe Dream.
 * NES emulator By ArDev
 */
class Mapper3(
    private val prgRom: ByteArray,
    private val chrRom: ByteArray,
    override var mirroring: MirroringMode,
    private val prgBanks: Int,
    private val isChrRam: Boolean = false
) : Mapper {

    override val mapperId: Int = 3

    private var chrBank: Int = 0

    override fun cpuRead(address: Int): Int {
        if (address in 0x8000..0xFFFF) {
            val mask = if (prgBanks > 1) 0x7FFF else 0x3FFF
            val mappedAddr = (address - 0x8000) and mask
            return if (mappedAddr in prgRom.indices) {
                prgRom[mappedAddr].toInt() and 0xFF
            } else {
                0
            }
        }
        return -1
    }

    override fun cpuWrite(address: Int, value: Int): Boolean {
        if (address in 0x8000..0xFFFF) {
            chrBank = value and 0x03
            return true
        }
        return false
    }

    override fun ppuRead(address: Int): Int {
        if (address in 0x0000..0x1FFF) {
            val totalChrBanks = (chrRom.size / 8192).coerceAtLeast(1)
            val bank = chrBank % totalChrBanks
            val physAddr = bank * 8192 + address
            return if (physAddr in chrRom.indices) {
                chrRom[physAddr].toInt() and 0xFF
            } else {
                0
            }
        }
        return -1
    }

    override fun ppuWrite(address: Int, value: Int): Boolean {
        if (address in 0x0000..0x1FFF && isChrRam) {
            val totalChrBanks = (chrRom.size / 8192).coerceAtLeast(1)
            val bank = chrBank % totalChrBanks
            val physAddr = bank * 8192 + address
            if (physAddr in chrRom.indices) {
                chrRom[physAddr] = (value and 0xFF).toByte()
                return true
            }
        }
        return false
    }

    override fun saveState(out: DataOutputStream) {
        out.writeInt(chrBank)
    }

    override fun loadState(inp: DataInputStream) {
        chrBank = inp.readInt()
    }
}
