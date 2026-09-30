/* NES emulator By ArDev */
package com.example.nes.core

import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Mapper 1: Nintendo MMC1 (Memory Management Controller 1).
 * Supports popular classics: The Legend of Zelda, Metroid, Mega Man 2, Kid Icarus, Final Fantasy.
 * NES emulator By ArDev
 */
class Mapper1(
    private val prgRom: ByteArray,
    private val chrRom: ByteArray,
    override var mirroring: MirroringMode,
    private val prgBanks: Int,
    private val isChrRam: Boolean = false
) : Mapper {

    override val mapperId: Int = 1

    private val prgRam = ByteArray(8 * 1024)

    // Shift register & write counter
    private var shiftRegister: Int = 0x10
    private var controlRegister: Int = 0x0C // Default: PRG mode 3 (fix last bank at $C000)
    private var chrBank0: Int = 0
    private var chrBank1: Int = 0
    private var prgBank: Int = 0

    init {
        updateMirroring()
    }

    private fun updateMirroring() {
        mirroring = when (controlRegister and 0x03) {
            0 -> MirroringMode.SINGLE_SCREEN_LOWER
            1 -> MirroringMode.SINGLE_SCREEN_UPPER
            2 -> MirroringMode.VERTICAL
            3 -> MirroringMode.HORIZONTAL
            else -> MirroringMode.HORIZONTAL
        }
    }

    override fun cpuRead(address: Int): Int {
        return when (address) {
            in 0x6000..0x7FFF -> {
                prgRam[address - 0x6000].toInt() and 0xFF
            }
            in 0x8000..0xFFFF -> {
                val prgMode = (controlRegister shr 2) and 0x03
                val total16kBanks = (prgRom.size / 16384).coerceAtLeast(1)
                val mappedBank = when (prgMode) {
                    0, 1 -> {
                        // 32 KB switchable mode
                        val b32 = (prgBank and 0x0E) % (total16kBanks / 2).coerceAtLeast(1)
                        if (address < 0xC000) b32 * 2 else b32 * 2 + 1
                    }
                    2 -> {
                        // Fix first bank at $8000, switch 16KB at $C000
                        if (address < 0xC000) 0 else prgBank % total16kBanks
                    }
                    3 -> {
                        // Switch 16KB at $8000, fix last bank at $C000
                        if (address < 0xC000) (prgBank and 0x0F) % total16kBanks else total16kBanks - 1
                    }
                    else -> 0
                }
                val offset = address and 0x3FFF
                val physAddr = mappedBank * 16384 + offset
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
        if (address in 0x6000..0x7FFF) {
            prgRam[address - 0x6000] = (value and 0xFF).toByte()
            return true
        }

        if (address in 0x8000..0xFFFF) {
            // If bit 7 is set, reset shift register
            if ((value and 0x80) != 0) {
                shiftRegister = 0x10
                controlRegister = controlRegister or 0x0C
                updateMirroring()
                return true
            }

            val isFull = (shiftRegister and 0x01) != 0
            shiftRegister = (shiftRegister shr 1) or ((value and 0x01) shl 4)

            if (isFull) {
                val regValue = shiftRegister
                shiftRegister = 0x10

                when (address) {
                    in 0x8000..0x9FFF -> {
                        controlRegister = regValue and 0x1F
                        updateMirroring()
                    }
                    in 0xA000..0xBFFF -> {
                        chrBank0 = regValue and 0x1F
                    }
                    in 0xC000..0xDFFF -> {
                        chrBank1 = regValue and 0x1F
                    }
                    in 0xE000..0xFFFF -> {
                        prgBank = regValue and 0x0F
                    }
                }
            }
            return true
        }
        return false
    }

    override fun ppuRead(address: Int): Int {
        if (address in 0x0000..0x1FFF) {
            val chrMode = (controlRegister shr 4) and 0x01
            val total4kBanks = (chrRom.size / 4096).coerceAtLeast(1)

            val physAddr = if (chrMode == 0) {
                // 8 KB mode
                val b8 = (chrBank0 and 0x1E) % (total4kBanks / 2).coerceAtLeast(1)
                b8 * 8192 + (address and 0x1FFF)
            } else {
                // Two separate 4 KB banks
                if (address < 0x1000) {
                    val b4 = chrBank0 % total4kBanks
                    b4 * 4096 + (address and 0x0FFF)
                } else {
                    val b4 = chrBank1 % total4kBanks
                    b4 * 4096 + (address and 0x0FFF)
                }
            }

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
            val chrMode = (controlRegister shr 4) and 0x01
            val total4kBanks = (chrRom.size / 4096).coerceAtLeast(1)

            val physAddr = if (chrMode == 0) {
                val b8 = (chrBank0 and 0x1E) % (total4kBanks / 2).coerceAtLeast(1)
                b8 * 8192 + (address and 0x1FFF)
            } else {
                if (address < 0x1000) {
                    (chrBank0 % total4kBanks) * 4096 + (address and 0x0FFF)
                } else {
                    (chrBank1 % total4kBanks) * 4096 + (address and 0x0FFF)
                }
            }

            if (physAddr in chrRom.indices) {
                chrRom[physAddr] = (value and 0xFF).toByte()
                return true
            }
        }
        return false
    }

    override fun saveState(out: DataOutputStream) {
        out.writeInt(shiftRegister)
        out.writeInt(controlRegister)
        out.writeInt(chrBank0)
        out.writeInt(chrBank1)
        out.writeInt(prgBank)
        out.writeInt(prgRam.size)
        out.write(prgRam)
        out.writeBoolean(isChrRam)
        if (isChrRam) {
            out.writeInt(chrRom.size)
            out.write(chrRom)
        }
    }

    override fun loadState(inp: DataInputStream) {
        shiftRegister = inp.readInt()
        controlRegister = inp.readInt()
        chrBank0 = inp.readInt()
        chrBank1 = inp.readInt()
        prgBank = inp.readInt()
        updateMirroring()

        val ramSize = inp.readInt()
        inp.readFully(prgRam, 0, ramSize.coerceAtMost(prgRam.size))
        val chrRamFlag = inp.readBoolean()
        if (chrRamFlag && isChrRam) {
            val chrSize = inp.readInt()
            inp.readFully(chrRom, 0, chrSize.coerceAtMost(chrRom.size))
        }
    }
}
