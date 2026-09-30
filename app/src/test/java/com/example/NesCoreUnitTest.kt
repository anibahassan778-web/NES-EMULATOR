package com.example

import com.example.nes.core.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests for NES Emulator Core:
 * CPU 6502, Bus, Cartridge iNES parser, Mapper 0, PPU registers & VRAM, and Input Controller.
 */
class NesCoreUnitTest {

    @Test
    fun `test CPU 6502 LDA immediate and flags`() {
        val bus = NesBus()
        val cpu = Cpu6502(bus)
        val ppu = Ppu2C02(null)
        val apu = Apu2A03()
        bus.cpu = cpu
        bus.ppu = ppu
        bus.apu = apu

        // LDA #$42 (Opcode 0xA9 0x42) placed in RAM at 0x0200
        bus.cpuWrite(0x0200, 0xA9)
        bus.cpuWrite(0x0201, 0x42)
        cpu.pc = 0x0200

        val cycles = cpu.step()
        assertEquals(2, cycles)
        assertEquals(0x42, cpu.a)
        assertFalse(cpu.getFlag(Cpu6502.FLAG_Z))
        assertFalse(cpu.getFlag(Cpu6502.FLAG_N))

        // LDA #$00 (sets Zero flag)
        bus.cpuWrite(0x0202, 0xA9)
        bus.cpuWrite(0x0203, 0x00)
        cpu.step()
        assertEquals(0, cpu.a)
        assertTrue(cpu.getFlag(Cpu6502.FLAG_Z))
        assertFalse(cpu.getFlag(Cpu6502.FLAG_N))

        // LDA #$F0 (sets Negative flag)
        bus.cpuWrite(0x0204, 0xA9)
        bus.cpuWrite(0x0205, 0xF0)
        cpu.step()
        assertEquals(0xF0, cpu.a)
        assertFalse(cpu.getFlag(Cpu6502.FLAG_Z))
        assertTrue(cpu.getFlag(Cpu6502.FLAG_N))
    }

    @Test
    fun `test CPU 6502 ADC with Carry and Overflow flags`() {
        val bus = NesBus()
        val cpu = Cpu6502(bus)
        val ppu = Ppu2C02(null)
        val apu = Apu2A03()
        bus.cpu = cpu
        bus.ppu = ppu
        bus.apu = apu

        // A = 0x50, ADC #$50 -> A = 0xA0 (Overflow because 80 + 80 = 160 > 127 in signed 8-bit)
        cpu.a = 0x50
        cpu.setFlag(Cpu6502.FLAG_C, false)
        bus.cpuWrite(0x0200, 0x69) // ADC immediate
        bus.cpuWrite(0x0201, 0x50)
        cpu.pc = 0x0200

        cpu.step()
        assertEquals(0xA0, cpu.a)
        assertFalse(cpu.getFlag(Cpu6502.FLAG_C))
        assertTrue(cpu.getFlag(Cpu6502.FLAG_V))

        // ADC with Carry: A = 0xFF, Carry = 1, ADC #$01 -> A = 0x01, Carry = 1
        cpu.a = 0xFF
        cpu.setFlag(Cpu6502.FLAG_C, true)
        bus.cpuWrite(0x0202, 0x69)
        bus.cpuWrite(0x0203, 0x01)
        cpu.step()
        assertEquals(0x01, cpu.a)
        assertTrue(cpu.getFlag(Cpu6502.FLAG_C))
    }

    @Test
    fun `test CPU 6502 Stack operations PHA and PLA`() {
        val bus = NesBus()
        val cpu = Cpu6502(bus)
        val ppu = Ppu2C02(null)
        val apu = Apu2A03()
        bus.cpu = cpu
        bus.ppu = ppu
        bus.apu = apu

        cpu.sp = 0xFF
        cpu.a = 0x77

        // PHA (0x48) in RAM at 0x0200
        bus.cpuWrite(0x0200, 0x48)
        cpu.pc = 0x0200
        cpu.step()
        assertEquals(0xFE, cpu.sp)
        assertEquals(0x77, bus.cpuRead(0x01FF))

        // Change A, then PLA (0x68)
        cpu.a = 0x00
        bus.cpuWrite(0x0201, 0x68)
        cpu.step()
        assertEquals(0xFF, cpu.sp)
        assertEquals(0x77, cpu.a)
    }

    @Test
    fun `test Bus RAM read write and mirroring`() {
        val bus = NesBus()
        val ppu = Ppu2C02(null)
        val apu = Apu2A03()
        val cpu = Cpu6502(bus)
        bus.cpu = cpu
        bus.ppu = ppu
        bus.apu = apu

        // Write to $0050, verify mirroring at $0850, $1050, $1850
        bus.cpuWrite(0x0050, 0xAB)
        assertEquals(0xAB, bus.cpuRead(0x0050))
        assertEquals(0xAB, bus.cpuRead(0x0850))
        assertEquals(0xAB, bus.cpuRead(0x1050))
        assertEquals(0xAB, bus.cpuRead(0x1850))
    }

    @Test
    fun `test Cartridge iNES parser and validation`() {
        // Valid 16KB PRG + 8KB CHR header
        val header = ByteArray(16 + 16384 + 8192)
        header[0] = 0x4E.toByte() // N
        header[1] = 0x45.toByte() // E
        header[2] = 0x53.toByte() // S
        header[3] = 0x1A.toByte()
        header[4] = 1 // 1 x 16KB PRG
        header[5] = 1 // 1 x 8KB CHR
        header[6] = 0 // Mapper 0, horizontal mirroring
        header[7] = 0

        val cartridge = Cartridge.fromBytes(header, "Test Cartridge")
        assertEquals(0, cartridge.mapperId)
        assertEquals(16384, cartridge.prgRomSize)
        assertEquals(8192, cartridge.chrRomSize)
        assertEquals(MirroringMode.HORIZONTAL, cartridge.mirroring)

        // Invalid header should throw
        val invalidHeader = ByteArray(32)
        assertThrows(IllegalArgumentException::class.java) {
            Cartridge.fromBytes(invalidHeader, "Bad ROM")
        }
    }

    @Test
    fun `test Mapper0 NROM-128 PRG mirroring`() {
        val prg = ByteArray(16384)
        prg[0] = 0x12 // First byte at $8000
        prg[16383] = 0x34 // Last byte at $BFFF

        val chr = ByteArray(8192)
        chr[0] = 0x56

        val mapper0 = Mapper0(
            prgRom = prg,
            chrRom = chr,
            mirroring = MirroringMode.HORIZONTAL,
            prgBanks = 1,
            isChrRam = false
        )

        // PRG at $8000
        assertEquals(0x12, mapper0.cpuRead(0x8000))
        // PRG mirrored at $C000
        assertEquals(0x12, mapper0.cpuRead(0xC000))
        // PRG at $BFFF
        assertEquals(0x34, mapper0.cpuRead(0xBFFF))
        // PRG mirrored at $FFFF
        assertEquals(0x34, mapper0.cpuRead(0xFFFF))

        // CHR read
        assertEquals(0x56, mapper0.ppuRead(0x0000))
    }

    @Test
    fun `test PPU Registers and VRAM access`() {
        val ppu = Ppu2C02(null)
        ppu.reset()

        // PPUADDR latch: write $21, then $08 -> VRAM address = $2108
        ppu.cpuWrite(6, 0x21) // High byte
        ppu.cpuWrite(6, 0x08) // Low byte
        assertEquals(0x2108, ppu.v)

        // Write to PPUDATA ($2108)
        ppu.cpuWrite(7, 0x5A)
        // Verify written to internal VRAM
        val readVal = ppu.ppuRead(0x2108)
        assertEquals(0x5A, readVal)

        // Palette Mirroring: write to $3F10 should mirror to $3F00
        ppu.cpuWrite(6, 0x3F)
        ppu.cpuWrite(6, 0x10)
        ppu.cpuWrite(7, 0x20)
        assertEquals(0x20, ppu.ppuRead(0x3F00))
        assertEquals(0x20, ppu.ppuRead(0x3F10))
    }

    @Test
    fun `test Controller input all 8 buttons and shift register`() {
        val controller = Controller()

        // Press A, Select, and Right
        controller.setButtonPressed(Controller.BUTTON_A, true)
        controller.setButtonPressed(Controller.BUTTON_SELECT, true)
        controller.setButtonPressed(Controller.BUTTON_RIGHT, true)

        // Strobe controller
        controller.writeStrobe(1)
        controller.writeStrobe(0)

        // Read in order: A, B, Select, Start, Up, Down, Left, Right
        assertEquals(1, controller.read()) // A (pressed)
        assertEquals(0, controller.read()) // B (not pressed)
        assertEquals(1, controller.read()) // Select (pressed)
        assertEquals(0, controller.read()) // Start
        assertEquals(0, controller.read()) // Up
        assertEquals(0, controller.read()) // Down
        assertEquals(0, controller.read()) // Left
        assertEquals(1, controller.read()) // Right (pressed)
    }

    @Test
    fun `test Homebrew Test ROM generation`() {
        val romBytes = HomebrewTestRom.ROM_BYTES
        assertTrue("ROM bytes should be valid iNES size", romBytes.size > 16384)
        assertEquals('N'.code.toByte(), romBytes[0])
        assertEquals('E'.code.toByte(), romBytes[1])
        assertEquals('S'.code.toByte(), romBytes[2])
        assertEquals(0x1A.toByte(), romBytes[3])

        // Parse with Cartridge loader
        val cart = Cartridge.fromBytes(romBytes, "Homebrew Test")
        assertNotNull(cart)
        assertEquals(0, cart.mapperId)
    }
}
