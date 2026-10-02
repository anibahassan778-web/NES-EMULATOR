/* NES emulator By ArDev */
package com.example.nes.core

import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * NES Mapper 4 (Nintendo MMC3).
 * Supports Super Mario Bros 3, Mega Man 3/4/5/6, Kirby's Adventure, etc.
 * Features:
 * - 8KB PRG ROM bank switching ($8000-$FFFF)
 * - 1KB & 2KB CHR ROM/RAM bank switching ($0000-$1FFF)
 * - Dynamic Vertical / Horizontal mirroring ($A000)
 * - 8KB PRG RAM at $6000-$7FFF
 * - Scanline-driven IRQ counter for raster split-screen status bars ($C000-$E001)
 * NES emulator By ArDev
 */
class Mapper4(
    private val prgRom: ByteArray,
    private val chrRom: ByteArray,
    override var mirroring: MirroringMode,
    private val prgBanks: Int,
    private val isChrRam: Boolean
) : Mapper {

    override val mapperId: Int = 4

    // 8KB PRG RAM at $6000-$7FFF
    private val prgRam = ByteArray(8192)

    // Registers
    private var bankSelect: Int = 0
    private val registers = IntArray(8)

    private var prgMode: Int = 0 // Bit 6 of $8000
    private var chrInversion: Int = 0 // Bit 7 of $8000

    private var prgRamProtect: Boolean = false
    private var prgRamEnable: Boolean = true

    // Scanline IRQ
    private var irqLatch: Int = 0
    private var irqCounter: Int = 0
    private var irqEnabled: Boolean = false
    private var irqReload: Boolean = false
    private var irqPending: Boolean = false

    // Number of 8KB PRG banks
    private val totalPrg8k: Int = (prgRom.size / 8192).coerceAtLeast(1)
    // Number of 1KB CHR banks
    private val totalChr1k: Int = (chrRom.size / 1024).coerceAtLeast(1)

    init {
        registers[0] = 0
        registers[1] = 2
        registers[2] = 4
        registers[3] = 5
        registers[4] = 6
        registers[5] = 7
        registers[6] = 0
        registers[7] = 1
    }

    override fun irqState(): Boolean = irqPending

    override fun clearIrq() {
        irqPending = false
    }

    override fun stepScanline() {
        if (irqCounter == 0 || irqReload) {
            irqCounter = irqLatch
            irqReload = false
        } else {
            irqCounter--
        }

        if (irqCounter == 0 && irqEnabled) {
            irqPending = true
        }
    }

    override fun cpuRead(address: Int): Int {
        val addr = address and 0xFFFF
        return when (addr) {
            in 0x6000..0x7FFF -> {
                if (prgRamEnable) {
                    prgRam[addr - 0x6000].toInt() and 0xFF
                } else 0
            }
            in 0x8000..0x9FFF -> {
                val bank = if (prgMode == 0) registers[6] else totalPrg8k - 2
                val offset = (bank % totalPrg8k) * 8192 + (addr - 0x8000)
                if (offset in prgRom.indices) prgRom[offset].toInt() and 0xFF else 0
            }
            in 0xA000..0xBFFF -> {
                val bank = registers[7]
                val offset = (bank % totalPrg8k) * 8192 + (addr - 0xA000)
                if (offset in prgRom.indices) prgRom[offset].toInt() and 0xFF else 0
            }
            in 0xC000..0xDFFF -> {
                val bank = if (prgMode == 0) totalPrg8k - 2 else registers[6]
                val offset = (bank % totalPrg8k) * 8192 + (addr - 0xC000)
                if (offset in prgRom.indices) prgRom[offset].toInt() and 0xFF else 0
            }
            in 0xE000..0xFFFF -> {
                val bank = totalPrg8k - 1 // Fixed to last 8KB bank
                val offset = bank * 8192 + (addr - 0xE000)
                if (offset in prgRom.indices) prgRom[offset].toInt() and 0xFF else 0
            }
            else -> -1
        }
    }

    override fun cpuWrite(address: Int, value: Int): Boolean {
        val addr = address and 0xFFFF
        val v8 = value and 0xFF

        when (addr) {
            in 0x6000..0x7FFF -> {
                if (prgRamEnable && !prgRamProtect) {
                    prgRam[addr - 0x6000] = v8.toByte()
                }
                return true
            }
            in 0x8000..0x9FFF -> {
                if ((addr and 1) == 0) { // $8000: Bank Select
                    bankSelect = v8 and 0x07
                    prgMode = (v8 shr 6) and 0x01
                    chrInversion = (v8 shr 7) and 0x01
                } else { // $8001: Bank Data
                    registers[bankSelect] = v8
                }
                return true
            }
            in 0xA000..0xBFFF -> {
                if ((addr and 1) == 0) { // $A000: Mirroring
                    mirroring = if ((v8 and 0x01) == 0) MirroringMode.VERTICAL else MirroringMode.HORIZONTAL
                } else { // $A001: PRG RAM protect
                    prgRamProtect = (v8 and 0x40) != 0
                    prgRamEnable = (v8 and 0x80) != 0
                }
                return true
            }
            in 0xC000..0xDFFF -> {
                if ((addr and 1) == 0) { // $C000: IRQ Latch
                    irqLatch = v8
                } else { // $C001: IRQ Reload
                    irqReload = true
                }
                return true
            }
            in 0xE000..0xFFFF -> {
                if ((addr and 1) == 0) { // $E000: IRQ Disable
                    irqEnabled = false
                    irqPending = false
                } else { // $E001: IRQ Enable
                    irqEnabled = true
                }
                return true
            }
        }
        return false
    }

    override fun ppuRead(address: Int): Int {
        val addr = address and 0x1FFF
        val bank = getChrBank(addr)
        val offset = (bank % totalChr1k) * 1024 + (addr and 0x03FF)
        return if (offset in chrRom.indices) chrRom[offset].toInt() and 0xFF else 0
    }

    override fun ppuWrite(address: Int, value: Int): Boolean {
        if (!isChrRam) return false
        val addr = address and 0x1FFF
        val bank = getChrBank(addr)
        val offset = (bank % totalChr1k) * 1024 + (addr and 0x03FF)
        if (offset in chrRom.indices) {
            chrRom[offset] = (value and 0xFF).toByte()
            return true
        }
        return false
    }

    private fun getChrBank(address: Int): Int {
        val addr = address and 0x1FFF
        return if (chrInversion == 0) {
            when (addr) {
                in 0x0000..0x07FF -> (registers[0] and 0xFE) + ((addr shr 10) and 1)
                in 0x0800..0x0FFF -> (registers[1] and 0xFE) + ((addr shr 10) and 1)
                in 0x1000..0x13FF -> registers[2]
                in 0x1400..0x17FF -> registers[3]
                in 0x1800..0x1BFF -> registers[4]
                in 0x1C00..0x1FFF -> registers[5]
                else -> 0
            }
        } else {
            when (addr) {
                in 0x0000..0x03FF -> registers[2]
                in 0x0400..0x07FF -> registers[3]
                in 0x0800..0x0BFF -> registers[4]
                in 0x0C00..0x0FFF -> registers[5]
                in 0x1000..0x17FF -> (registers[0] and 0xFE) + ((addr shr 10) and 1)
                in 0x1800..0x1FFF -> (registers[1] and 0xFE) + ((addr shr 10) and 1)
                else -> 0
            }
        }
    }

    override fun saveState(out: DataOutputStream) {
        out.write(prgRam)
        out.writeInt(bankSelect)
        for (r in registers) out.writeInt(r)
        out.writeInt(prgMode)
        out.writeInt(chrInversion)
        out.writeBoolean(prgRamProtect)
        out.writeBoolean(prgRamEnable)
        out.writeInt(irqLatch)
        out.writeInt(irqCounter)
        out.writeBoolean(irqEnabled)
        out.writeBoolean(irqReload)
        out.writeBoolean(irqPending)
    }

    override fun loadState(inp: DataInputStream) {
        inp.readFully(prgRam)
        bankSelect = inp.readInt()
        for (i in registers.indices) registers[i] = inp.readInt()
        prgMode = inp.readInt()
        chrInversion = inp.readInt()
        prgRamProtect = inp.readBoolean()
        prgRamEnable = inp.readBoolean()
        irqLatch = inp.readInt()
        irqCounter = inp.readInt()
        irqEnabled = inp.readBoolean()
        irqReload = inp.readBoolean()
        irqPending = inp.readBoolean()
    }
}
