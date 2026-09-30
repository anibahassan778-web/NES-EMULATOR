/* NES emulator By ArDev */
package com.example.nes.core

import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Mapper 0 (NROM-128 and NROM-256).
 * Standard mapper for early NES titles (Super Mario Bros., Donkey Kong, Duck Hunt, Pac-Man, etc.).
 * NES emulator By ArDev
 */
class Mapper0(
    private val prgRom: ByteArray,
    private val chrRom: ByteArray,
    override var mirroring: MirroringMode,
    private val prgBanks: Int,
    private val isChrRam: Boolean = false
) : Mapper {

    override val mapperId: Int = 0

    // Optional 8KB PRG RAM at 0x6000 - 0x7FFF
    private val prgRam = ByteArray(8 * 1024)

    override fun cpuRead(address: Int): Int {
        return when (address) {
            in 0x6000..0x7FFF -> {
                prgRam[address - 0x6000].toInt() and 0xFF
            }
            in 0x8000..0xFFFF -> {
                val mask = if (prgBanks > 1) 0x7FFF else 0x3FFF
                val mappedAddr = (address - 0x8000) and mask
                if (mappedAddr < prgRom.size) {
                    prgRom[mappedAddr].toInt() and 0xFF
                } else {
                    0
                }
            }
            else -> -1
        }
    }

    override fun cpuWrite(address: Int, value: Int): Boolean {
        if (address in 0x6000..0x7FFF) {
            prgRam[address - 0x6000] = (value and 0xFF).toByte()
            return true
        }
        // PRG ROM writes are ignored in Mapper 0
        return false
    }

    override fun ppuRead(address: Int): Int {
        if (address in 0x0000..0x1FFF) {
            return if (address < chrRom.size) {
                chrRom[address].toInt() and 0xFF
            } else {
                0
            }
        }
        return -1
    }

    override fun ppuWrite(address: Int, value: Int): Boolean {
        if (address in 0x0000..0x1FFF && isChrRam) {
            if (address < chrRom.size) {
                chrRom[address] = (value and 0xFF).toByte()
                return true
            }
        }
        return false
    }

    override fun saveState(out: DataOutputStream) {
        out.writeInt(prgRam.size)
        out.write(prgRam)
        out.writeBoolean(isChrRam)
        if (isChrRam) {
            out.writeInt(chrRom.size)
            out.write(chrRom)
        }
    }

    override fun loadState(inp: DataInputStream) {
        val ramSize = inp.readInt()
        inp.readFully(prgRam, 0, ramSize.coerceAtMost(prgRam.size))
        val chrRamFlag = inp.readBoolean()
        if (chrRamFlag && isChrRam) {
            val chrSize = inp.readInt()
            inp.readFully(chrRom, 0, chrSize.coerceAtMost(chrRom.size))
        }
    }
}
