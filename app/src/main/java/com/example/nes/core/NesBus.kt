package com.example.nes.core

import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * NES System Bus. Interconnects CPU, RAM, PPU, APU, Cartridge, and Controllers.
 */
class NesBus {

    val cpuRam = ByteArray(2048)

    lateinit var cpu: Cpu6502
    lateinit var ppu: Ppu2C02
    lateinit var apu: Apu2A03
    var cartridge: Cartridge? = null
    val controller1 = Controller()
    val controller2 = Controller()

    var dmaCycles: Int = 0

    fun reset() {
        cpuRam.fill(0)
        ppu.reset()
        cpu.reset()
    }

    fun cpuRead(address: Int): Int {
        val addr = address and 0xFFFF
        return when (addr) {
            in 0x0000..0x1FFF -> {
                cpuRam[addr and 0x07FF].toInt() and 0xFF
            }
            in 0x2000..0x3FFF -> {
                ppu.cpuRead(addr and 0x07)
            }
            0x4015 -> {
                apu.cpuRead(addr)
            }
            0x4016 -> {
                controller1.read()
            }
            0x4017 -> {
                controller2.read()
            }
            in 0x4020..0xFFFF -> {
                cartridge?.cpuRead(addr) ?: 0
            }
            else -> 0
        }
    }

    fun cpuWrite(address: Int, value: Int) {
        val addr = address and 0xFFFF
        val v8 = value and 0xFF
        when (addr) {
            in 0x0000..0x1FFF -> {
                cpuRam[addr and 0x07FF] = v8.toByte()
            }
            in 0x2000..0x3FFF -> {
                ppu.cpuWrite(addr and 0x07, v8)
            }
            0x4014 -> { // OAM DMA
                val page = v8 shl 8
                for (i in 0..255) {
                    val byte = cpuRead(page + i)
                    ppu.oam[(ppu.oamAddr + i) and 0xFF] = (byte and 0xFF).toByte()
                }
                dmaCycles += 513
            }
            0x4016 -> {
                controller1.writeStrobe(v8)
                controller2.writeStrobe(v8)
            }
            in 0x4000..0x4013, 0x4015, 0x4017 -> {
                apu.cpuWrite(addr, v8)
            }
            in 0x4020..0xFFFF -> {
                cartridge?.cpuWrite(addr, v8)
            }
        }
    }

    fun saveState(out: DataOutputStream) {
        out.write(cpuRam)
        out.writeInt(dmaCycles)
    }

    fun loadState(inp: DataInputStream) {
        inp.readFully(cpuRam)
        dmaCycles = inp.readInt()
    }
}
