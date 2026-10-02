/* NES emulator By ArDev */
package com.example

import com.example.nes.core.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests for NES Mappers:
 * - Mapper 1 (MMC1): Shift register, PRG banking, CHR banking, mirroring
 * - Mapper 2 (UxROM): Switchable $8000 bank, fixed $C000 bank, CHR RAM
 * - Mapper 3 (CNROM): Switchable CHR banks
 * - Mapper 4 (MMC3): 8KB PRG banks, 1KB/2KB CHR banks, Scanline IRQ counter
 * - Mapper 7 (AxROM): 32KB PRG bank switching, single screen mirroring
 * - Cartridge loader supporting Mappers 0, 1, 2, 3, 4, 7
 * NES emulator By ArDev
 */
class MappersTest {

    @Test
    fun `test Mapper 1 MMC1 shift register and bank switching`() {
        val prgSize = 8 * 16384 // 128 KB (8 banks)
        val prgRom = ByteArray(prgSize) { (it / 16384).toByte() }
        val chrRom = ByteArray(16 * 4096) { (it / 4096).toByte() }

        val mapper1 = Mapper1(
            prgRom = prgRom,
            chrRom = chrRom,
            mirroring = MirroringMode.VERTICAL,
            prgBanks = 8
        )

        // Reset MMC1 shift register (write with bit 7 set)
        mapper1.cpuWrite(0x8000, 0x80)

        // Write 5 bits to PRG bank register (0xE000) to select bank 3
        val valueToShift = 0x03
        for (i in 0 until 5) {
            val bit = (valueToShift shr i) and 0x01
            mapper1.cpuWrite(0xE000, bit)
        }

        // In default mode 3, bank 3 should be mapped at $8000, and last bank (7) at $C000
        val readAt8000 = mapper1.cpuRead(0x8000)
        assertEquals(3, readAt8000)

        val readAtC000 = mapper1.cpuRead(0xC000)
        assertEquals(7, readAtC000)

        // PRG RAM write & read
        mapper1.cpuWrite(0x6000, 0x99)
        assertEquals(0x99, mapper1.cpuRead(0x6000))
    }

    @Test
    fun `test Mapper 2 UxROM bank switching`() {
        val prgSize = 4 * 16384 // 64 KB (4 banks: 0, 1, 2, 3)
        val prgRom = ByteArray(prgSize) { (it / 16384).toByte() }
        val chrRam = ByteArray(8192)

        val mapper2 = Mapper2(
            prgRom = prgRom,
            chrRom = chrRam,
            mirroring = MirroringMode.HORIZONTAL,
            prgBanks = 4,
            isChrRam = true
        )

        // Initially bank 0 at $8000, last bank (3) at $C000
        assertEquals(0, mapper2.cpuRead(0x8000))
        assertEquals(3, mapper2.cpuRead(0xC000))

        // Switch bank at $8000 to bank 2
        mapper2.cpuWrite(0x8000, 0x02)
        assertEquals(2, mapper2.cpuRead(0x8000))
        assertEquals(3, mapper2.cpuRead(0xC000)) // Last bank remains fixed

        // Switch bank at $8000 to bank 1
        mapper2.cpuWrite(0xA000, 0x01)
        assertEquals(1, mapper2.cpuRead(0x8000))
        assertEquals(3, mapper2.cpuRead(0xC000))

        // CHR RAM read/write
        assertTrue(mapper2.ppuWrite(0x0100, 0x77))
        assertEquals(0x77, mapper2.ppuRead(0x0100))
    }

    @Test
    fun `test Mapper 3 CNROM CHR bank switching`() {
        val prgRom = ByteArray(32768) { 0x55 }
        val chrRom = ByteArray(4 * 8192) { (it / 8192).toByte() } // 4 CHR banks: 0, 1, 2, 3

        val mapper3 = Mapper3(
            prgRom = prgRom,
            chrRom = chrRom,
            mirroring = MirroringMode.VERTICAL,
            prgBanks = 2,
            isChrRam = false
        )

        // Fixed PRG ROM read
        assertEquals(0x55, mapper3.cpuRead(0x8000))

        // Initially bank 0
        assertEquals(0, mapper3.ppuRead(0x0000))

        // Switch to CHR bank 2
        mapper3.cpuWrite(0x8000, 0x02)
        assertEquals(2, mapper3.ppuRead(0x0000))

        // Switch to CHR bank 3
        mapper3.cpuWrite(0x8000, 0x03)
        assertEquals(3, mapper3.ppuRead(0x0000))
    }

    @Test
    fun `test Mapper 4 MMC3 PRG banking and scanline IRQ counter`() {
        val prgSize = 16 * 8192 // 128 KB (16 banks of 8KB: 0..15)
        val prgRom = ByteArray(prgSize) { (it / 8192).toByte() }
        val chrRom = ByteArray(64 * 1024) { (it / 1024).toByte() } // 64 banks of 1KB

        val mapper4 = Mapper4(
            prgRom = prgRom,
            chrRom = chrRom,
            mirroring = MirroringMode.VERTICAL,
            prgBanks = 8,
            isChrRam = false
        )

        // Select R6 (PRG bank at $8000)
        mapper4.cpuWrite(0x8000, 0x06)
        mapper4.cpuWrite(0x8001, 0x05) // Bank 5 at $8000
        assertEquals(5, mapper4.cpuRead(0x8000))

        // Last bank (15) is always fixed at $E000
        assertEquals(15, mapper4.cpuRead(0xE000))

        // Test PRG RAM ($6000-$7FFF)
        mapper4.cpuWrite(0x6000, 0x42)
        assertEquals(0x42, mapper4.cpuRead(0x6000))

        // Test Scanline IRQ counter ($C000 latch, $C001 reload, $E001 enable)
        mapper4.cpuWrite(0xC000, 0x03) // Latch = 3
        mapper4.cpuWrite(0xC001, 0x00) // Reload = true
        mapper4.cpuWrite(0xE001, 0x00) // Enable IRQ

        assertFalse(mapper4.irqState())

        mapper4.stepScanline() // Counter = 3
        assertFalse(mapper4.irqState())

        mapper4.stepScanline() // Counter = 2
        assertFalse(mapper4.irqState())

        mapper4.stepScanline() // Counter = 1
        assertFalse(mapper4.irqState())

        mapper4.stepScanline() // Counter = 0 -> IRQ Fired!
        assertTrue(mapper4.irqState())

        mapper4.clearIrq()
        assertFalse(mapper4.irqState())
    }

    @Test
    fun `test Mapper 7 AxROM 32KB PRG banking and single screen mirroring`() {
        val prgSize = 4 * 32768 // 128 KB (4 banks of 32KB: 0..3)
        val prgRom = ByteArray(prgSize) { (it / 32768).toByte() }
        val chrRam = ByteArray(8192)

        val mapper7 = Mapper7(
            prgRom = prgRom,
            chrRom = chrRam,
            mirroring = MirroringMode.SINGLE_SCREEN_LOWER,
            prgBanks = 8,
            isChrRam = true
        )

        // Initial bank 0
        assertEquals(0, mapper7.cpuRead(0x8000))

        // Switch to bank 2 with single screen upper mirroring (bit 4 set: 0x12)
        mapper7.cpuWrite(0x8000, 0x12)
        assertEquals(2, mapper7.cpuRead(0x8000))
        assertEquals(MirroringMode.SINGLE_SCREEN_UPPER, mapper7.mirroring)

        // Switch to bank 3 with single screen lower mirroring (0x03)
        mapper7.cpuWrite(0x8000, 0x03)
        assertEquals(3, mapper7.cpuRead(0x8000))
        assertEquals(MirroringMode.SINGLE_SCREEN_LOWER, mapper7.mirroring)
    }

    @Test
    fun `test Cartridge parser supports Mappers 0, 1, 2, 3, 4, 7`() {
        fun makeRom(mapperId: Int): ByteArray {
            val header = ByteArray(16)
            header[0] = 0x4E.toByte() // N
            header[1] = 0x45.toByte() // E
            header[2] = 0x53.toByte() // S
            header[3] = 0x1A.toByte()
            header[4] = 4 // 64 KB PRG
            header[5] = 2 // 16 KB CHR
            header[6] = ((mapperId and 0x0F) shl 4).toByte()
            header[7] = (mapperId and 0xF0).toByte()

            val prg = ByteArray(64 * 1024) { 0xEA.toByte() } // NOPs
            val chr = ByteArray(16 * 1024) { 0x00 }
            return header + prg + chr
        }

        // Test Mapper 0
        val cart0 = Cartridge.fromBytes(makeRom(0), "Test NROM")
        assertEquals(0, cart0.mapperId)
        assertTrue(cart0.mapper is Mapper0)

        // Test Mapper 1
        val cart1 = Cartridge.fromBytes(makeRom(1), "Test MMC1")
        assertEquals(1, cart1.mapperId)
        assertTrue(cart1.mapper is Mapper1)

        // Test Mapper 2
        val cart2 = Cartridge.fromBytes(makeRom(2), "Test UxROM")
        assertEquals(2, cart2.mapperId)
        assertTrue(cart2.mapper is Mapper2)

        // Test Mapper 3
        val cart3 = Cartridge.fromBytes(makeRom(3), "Test CNROM")
        assertEquals(3, cart3.mapperId)
        assertTrue(cart3.mapper is Mapper3)

        // Test Mapper 4 (MMC3)
        val cart4 = Cartridge.fromBytes(makeRom(4), "Test MMC3")
        assertEquals(4, cart4.mapperId)
        assertTrue(cart4.mapper is Mapper4)

        // Test Mapper 7 (AxROM)
        val cart7 = Cartridge.fromBytes(makeRom(7), "Test AxROM")
        assertEquals(7, cart7.mapperId)
        assertTrue(cart7.mapper is Mapper7)
    }
}
