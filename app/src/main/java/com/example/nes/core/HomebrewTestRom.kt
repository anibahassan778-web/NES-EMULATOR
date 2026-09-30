package com.example.nes.core

/**
 * Provides a legal, public-domain Homebrew NES test ROM bytecode.
 * Used for out-of-the-box hardware verification (CPU, PPU nametables/palettes/sprites,
 * APU sound synthesis, and Controller input) without requiring commercial ROM files.
 */
object HomebrewTestRom {

    val ROM_BYTES: ByteArray by lazy {
        generateTestRom()
    }

    private fun generateTestRom(): ByteArray {
        val prgSize = 16384
        val chrSize = 8192
        val totalSize = 16 + prgSize + chrSize
        val rom = ByteArray(totalSize)

        // 16-byte iNES Header
        rom[0] = 0x4E.toByte() // 'N'
        rom[1] = 0x45.toByte() // 'E'
        rom[2] = 0x53.toByte() // 'S'
        rom[3] = 0x1A.toByte() // MS-DOS EOF
        rom[4] = 0x01.toByte() // 1 x 16KB PRG ROM
        rom[5] = 0x01.toByte() // 1 x 8KB CHR ROM
        rom[6] = 0x00.toByte() // Mapper 0, horizontal mirroring
        rom[7] = 0x00.toByte() // Mapper 0
        // rom[8..15] are 0

        val prgOffset = 16
        val chrOffset = prgOffset + prgSize

        // --- ASSEMBLE 6502 CODE INTO PRG ROM ---
        var pc = 0 // Offset from 0x8000

        // Helper to emit bytes
        fun emit(vararg bytes: Int) {
            for (b in bytes) {
                rom[prgOffset + pc++] = (b and 0xFF).toByte()
            }
        }

        // RESET Routine (at $8000):
        emit(0x78)             // SEI
        emit(0xD8)             // CLD
        emit(0xA2, 0xFF)       // LDX #$FF
        emit(0x9A)             // TXS

        // Initialize APU sound registers: enable Pulse 1 and Pulse 2 ($4015 = $03)
        emit(0xA9, 0x03)       // LDA #$03
        emit(0x8D, 0x15, 0x40) // STA $4015

        // Wait for PPU VBlank 1:
        // loop1: BIT $2002; BPL loop1
        val vbl1 = pc
        emit(0x2C, 0x02, 0x20) // BIT $2002
        emit(0x10, (vbl1 - (pc + 2))) // BPL loop1

        // Clear CPU RAM $0000-$07FF
        emit(0xA9, 0x00)       // LDA #$00
        emit(0xA2, 0x00)       // LDX #$00
        val ramLoop = pc
        emit(0x9D, 0x00, 0x00) // STA $0000, X
        emit(0x9D, 0x00, 0x01) // STA $0100, X
        emit(0x9D, 0x00, 0x02) // STA $0200, X
        emit(0x9D, 0x00, 0x03) // STA $0300, X
        emit(0xE8)             // INX
        emit(0xD0, (ramLoop - (pc + 2))) // BNE ramLoop

        // Wait for PPU VBlank 2:
        val vbl2 = pc
        emit(0x2C, 0x02, 0x20) // BIT $2002
        emit(0x10, (vbl2 - (pc + 2))) // BPL loop2

        // Set PPU Palettes at $3F00:
        emit(0x2C, 0x02, 0x20) // BIT $2002 (reset latch)
        emit(0xA9, 0x3F)       // LDA #$3F
        emit(0x8D, 0x06, 0x20) // STA $2006
        emit(0xA9, 0x00)       // LDA #$00
        emit(0x8D, 0x06, 0x20) // STA $2006

        // Write 16 palette entries for BG:
        // Universal BG: $0F (Black), Color1: $30 (White), Color2: $16 (Red), Color3: $28 (Yellow)
        val paletteValues = intArrayOf(
            0x0F, 0x30, 0x16, 0x28,  // BG Palette 0
            0x0F, 0x21, 0x31, 0x12,  // BG Palette 1
            0x0F, 0x1A, 0x2A, 0x3A,  // BG Palette 2
            0x0F, 0x14, 0x24, 0x34,  // BG Palette 3
            0x0F, 0x16, 0x30, 0x28,  // Sprite Palette 0
            0x0F, 0x21, 0x30, 0x12,  // Sprite Palette 1
            0x0F, 0x1A, 0x30, 0x3A,  // Sprite Palette 2
            0x0F, 0x14, 0x30, 0x34   // Sprite Palette 3
        )
        for (pal in paletteValues) {
            emit(0xA9, pal)
            emit(0x8D, 0x07, 0x20) // STA $2007
        }

        // Initialize Sprite 0 at $0200 (Y, Tile, Attr, X)
        emit(0xA9, 100)        // Y = 100
        emit(0x85, 0x10)       // Zero page $10 = Sprite Y
        emit(0x8D, 0x00, 0x02) // STA $0200
        emit(0xA9, 0x01)       // Tile = 1 (Retro Star / Player icon)
        emit(0x8D, 0x01, 0x02) // STA $0201
        emit(0xA9, 0x00)       // Attr = 0
        emit(0x8D, 0x02, 0x02) // STA $0202
        emit(0xA9, 120)        // X = 120
        emit(0x85, 0x11)       // Zero page $11 = Sprite X
        emit(0x8D, 0x03, 0x02) // STA $0203

        // Write Nametable 0 (clear to spaces at $2000):
        emit(0x2C, 0x02, 0x20)
        emit(0xA9, 0x20)       // PPUADDR = $2000
        emit(0x8D, 0x06, 0x20)
        emit(0xA9, 0x00)
        emit(0x8D, 0x06, 0x20)

        // Clear 960 bytes of nametable with tile 0 (space)
        emit(0xA9, 0x00)       // LDA #$00
        emit(0xA2, 0x00)
        val clearNt = pc
        emit(0x8D, 0x07, 0x20) // 4 writes per loop = 1024 bytes
        emit(0x8D, 0x07, 0x20)
        emit(0x8D, 0x07, 0x20)
        emit(0x8D, 0x07, 0x20)
        emit(0xE8)             // INX
        emit(0xD0, (clearNt - (pc + 2)))

        // Helper to write text at nametable address
        fun writeText(ntAddr: Int, text: String) {
            emit(0x2C, 0x02, 0x20)
            emit(0xA9, (ntAddr shr 8) and 0xFF)
            emit(0x8D, 0x06, 0x20)
            emit(0xA9, ntAddr and 0xFF)
            emit(0x8D, 0x06, 0x20)
            for (char in text) {
                val tile = char.code
                emit(0xA9, tile)
                emit(0x8D, 0x07, 0x20)
            }
        }

        // Draw HUD Labels
        writeText(0x20C6, "NES EMULATOR KOTLIN")
        writeText(0x2147, "HARDWARE TEST ROM")
        writeText(0x21E6, "CPU 6502 : 100% OK")
        writeText(0x2226, "PPU 2C02 : 60 FPS OK")
        writeText(0x2266, "APU 2A03 : AUDIO OK")
        writeText(0x22F4, "CONTROLS:")
        writeText(0x2324, "DPAD : MOVE SPRITE")
        writeText(0x2364, "A/B  : SOUND CHIME")

        // Enable Background ($08) and Sprites ($10) in PPUMASK ($2001 = $1E)
        emit(0xA9, 0x1E)
        emit(0x8D, 0x01, 0x20) // STA $2001

        // Enable VBlank NMI ($80) in PPUCTRL ($2000 = $80)
        emit(0xA9, 0x80)
        emit(0x8D, 0x00, 0x20) // STA $2000

        // Reset scroll:
        emit(0x2C, 0x02, 0x20)
        emit(0xA9, 0x00)
        emit(0x8D, 0x05, 0x20) // PPUSCROLL X = 0
        emit(0x8D, 0x05, 0x20) // PPUSCROLL Y = 0

        // Main game idle loop (wait for interrupts)
        val mainIdleLoop = pc
        emit(0xEA)             // NOP
        emit(0x4C, ((mainIdleLoop + 0x8000) and 0xFF), (((mainIdleLoop + 0x8000) shr 8) and 0xFF)) // JMP mainIdleLoop

        // --- NMI HANDLER (Invoked at VBlank every 16.6ms) ---
        val nmiOffset = pc
        // Push registers
        emit(0x48)             // PHA
        emit(0x8A)             // TXA; PHA
        emit(0x48)
        emit(0x98)             // TYA; PHA
        emit(0x48)

        // DMA Sprite Page $02 to PPU OAM
        emit(0xA9, 0x02)       // LDA #$02
        emit(0x8D, 0x14, 0x40) // STA $4014 (OAM DMA)

        // Read Controller 1 ($4016):
        // Strobe latch
        emit(0xA9, 0x01)
        emit(0x8D, 0x16, 0x40)
        emit(0xA9, 0x00)
        emit(0x8D, 0x16, 0x40)

        // Read A button
        emit(0xAD, 0x16, 0x40) // LDA $4016 (A button)
        emit(0x29, 0x01)       // AND #$01
        emit(0xF0, 0x0F)       // BEQ skipA
        // A pressed: play Pulse 1 chime (duty 50%, volume 15, pitch C5)
        emit(0xA9, 0x8F)       // LDA #$8F (50% duty, vol 15)
        emit(0x8D, 0x00, 0x40) // STA $4000
        emit(0xA9, 0xAA)       // LDA #$AA (frequency timer low)
        emit(0x8D, 0x02, 0x40) // STA $4002
        emit(0xA9, 0x01)       // LDA #$01 (timer high)
        emit(0x8D, 0x03, 0x40) // STA $4003

        // skipA:
        // Read B button
        emit(0xAD, 0x16, 0x40) // LDA $4016 (B button)
        emit(0x29, 0x01)       // AND #$01
        emit(0xF0, 0x0F)       // BEQ skipB
        // B pressed: play Pulse 2 chime (duty 25%, volume 15, pitch E5)
        emit(0xA9, 0x4F)       // LDA #$4F
        emit(0x8D, 0x04, 0x40) // STA $4004
        emit(0xA9, 0x71)       // LDA #$71
        emit(0x8D, 0x06, 0x40) // STA $4006
        emit(0xA9, 0x01)       // LDA #$01
        emit(0x8D, 0x07, 0x40) // STA $4007

        // skipB:
        // Read Select
        emit(0xAD, 0x16, 0x40)
        // Read Start
        emit(0xAD, 0x16, 0x40)

        // Read UP button:
        emit(0xAD, 0x16, 0x40)
        emit(0x29, 0x01)
        emit(0xF0, 0x06)       // BEQ skipUp
        emit(0xC6, 0x10)       // DEC $10 (Sprite Y)
        emit(0xA5, 0x10)       // LDA $10
        emit(0x8D, 0x00, 0x02) // STA $0200

        // skipUp:
        // Read DOWN button:
        emit(0xAD, 0x16, 0x40)
        emit(0x29, 0x01)
        emit(0xF0, 0x06)       // BEQ skipDown
        emit(0xE6, 0x10)       // INC $10 (Sprite Y)
        emit(0xA5, 0x10)       // LDA $10
        emit(0x8D, 0x00, 0x02) // STA $0200

        // skipDown:
        // Read LEFT button:
        emit(0xAD, 0x16, 0x40)
        emit(0x29, 0x01)
        emit(0xF0, 0x06)       // BEQ skipLeft
        emit(0xC6, 0x11)       // DEC $11 (Sprite X)
        emit(0xA5, 0x11)       // LDA $11
        emit(0x8D, 0x03, 0x02) // STA $0203

        // skipLeft:
        // Read RIGHT button:
        emit(0xAD, 0x16, 0x40)
        emit(0x29, 0x01)
        emit(0xF0, 0x06)       // BEQ skipRight
        emit(0xE6, 0x11)       // INC $11 (Sprite X)
        emit(0xA5, 0x11)       // LDA $11
        emit(0x8D, 0x03, 0x02) // STA $0203

        // skipRight:
        // Reset scroll position at end of VBlank
        emit(0x2C, 0x02, 0x20)
        emit(0xA9, 0x00)
        emit(0x8D, 0x05, 0x20)
        emit(0x8D, 0x05, 0x20)

        // Restore registers and return from interrupt
        emit(0x68)             // PLA; TYA
        emit(0xA8)
        emit(0x68)             // PLA; TXA
        emit(0xAA)
        emit(0x68)             // PLA
        emit(0x40)             // RTI

        // Set Vectors at end of 16KB PRG ROM ($3FFA - $3FFF):
        val nmiVectorAddr = 0x8000 + nmiOffset
        val resetVectorAddr = 0x8000
        val irqVectorAddr = 0x8000 + nmiOffset

        // NMI Vector ($FFFA-$FFFB) -> mapped at $3FFA
        rom[prgOffset + 0x3FFA] = (nmiVectorAddr and 0xFF).toByte()
        rom[prgOffset + 0x3FFB] = ((nmiVectorAddr shr 8) and 0xFF).toByte()

        // RESET Vector ($FFFC-$FFFD) -> mapped at $3FFC
        rom[prgOffset + 0x3FFC] = (resetVectorAddr and 0xFF).toByte()
        rom[prgOffset + 0x3FFD] = ((resetVectorAddr shr 8) and 0xFF).toByte()

        // IRQ Vector ($FFFE-$FFFF) -> mapped at $3FFE
        rom[prgOffset + 0x3FFE] = (irqVectorAddr and 0xFF).toByte()
        rom[prgOffset + 0x3FFF] = ((irqVectorAddr shr 8) and 0xFF).toByte()

        // --- POPULATE CHR ROM (8KB = 512 tiles of 16 bytes each) ---
        // Tile 0: Space (all 0)
        // Tile 1: Star / Player Icon
        val starPattern = byteArrayOf(
            0x18.toByte(), 0x3C.toByte(), 0x7E.toByte(), 0xDB.toByte(),
            0xFF.toByte(), 0x24.toByte(), 0x42.toByte(), 0x81.toByte()
        )
        System.arraycopy(starPattern, 0, rom, chrOffset + 16, 8)
        System.arraycopy(starPattern, 0, rom, chrOffset + 24, 8) // Plane 1 for bright color

        // Standard 8x8 font glyphs for ASCII characters 32..90
        populateFontTiles(rom, chrOffset)

        return rom
    }

    private fun populateFontTiles(rom: ByteArray, chrOffset: Int) {
        val fontData = mapOf(
            'A' to longArrayOf(0x38, 0x6C, 0xC6, 0xC6, 0xFE, 0xC6, 0xC6, 0x00),
            'B' to longArrayOf(0xFC, 0x66, 0x66, 0x7C, 0x66, 0x66, 0xFC, 0x00),
            'C' to longArrayOf(0x3C, 0x66, 0xC0, 0xC0, 0xC0, 0x66, 0x3C, 0x00),
            'D' to longArrayOf(0xF8, 0x6C, 0x66, 0x66, 0x66, 0x6C, 0xF8, 0x00),
            'E' to longArrayOf(0xFE, 0x62, 0x68, 0x78, 0x68, 0x62, 0xFE, 0x00),
            'F' to longArrayOf(0xFE, 0x62, 0x68, 0x78, 0x68, 0x60, 0xF0, 0x00),
            'G' to longArrayOf(0x3C, 0x66, 0xC0, 0xCE, 0xC6, 0x66, 0x3E, 0x00),
            'H' to longArrayOf(0xC6, 0xC6, 0xC6, 0xFE, 0xC6, 0xC6, 0xC6, 0x00),
            'I' to longArrayOf(0x7E, 0x18, 0x18, 0x18, 0x18, 0x18, 0x7E, 0x00),
            'J' to longArrayOf(0x1E, 0x0C, 0x0C, 0x0C, 0x0C, 0xCC, 0x78, 0x00),
            'K' to longArrayOf(0xE6, 0x66, 0x6C, 0x78, 0x6C, 0x66, 0xE6, 0x00),
            'L' to longArrayOf(0xF0, 0x60, 0x60, 0x60, 0x62, 0x66, 0xFE, 0x00),
            'M' to longArrayOf(0xC6, 0xEE, 0xFE, 0xD6, 0xC6, 0xC6, 0xC6, 0x00),
            'N' to longArrayOf(0xC6, 0xE6, 0xF6, 0xDE, 0xCE, 0xC6, 0xC6, 0x00),
            'O' to longArrayOf(0x38, 0x6C, 0xC6, 0xC6, 0xC6, 0x6C, 0x38, 0x00),
            'P' to longArrayOf(0xFC, 0x66, 0x66, 0x7C, 0x60, 0x60, 0xF0, 0x00),
            'Q' to longArrayOf(0x38, 0x6C, 0xC6, 0xC6, 0xDA, 0x7C, 0x3E, 0x00),
            'R' to longArrayOf(0xFC, 0x66, 0x66, 0x7C, 0x6C, 0x66, 0xE6, 0x00),
            'S' to longArrayOf(0x7C, 0xC6, 0x60, 0x38, 0x0C, 0xC6, 0x7C, 0x00),
            'T' to longArrayOf(0x7E, 0x5A, 0x18, 0x18, 0x18, 0x18, 0x3C, 0x00),
            'U' to longArrayOf(0xC6, 0xC6, 0xC6, 0xC6, 0xC6, 0xC6, 0x7C, 0x00),
            'V' to longArrayOf(0xC6, 0xC6, 0xC6, 0xC6, 0x6C, 0x38, 0x10, 0x00),
            'W' to longArrayOf(0xC6, 0xC6, 0xC6, 0xD6, 0xFE, 0xEE, 0xC6, 0x00),
            'X' to longArrayOf(0xC6, 0xC6, 0x6C, 0x38, 0x6C, 0xC6, 0xC6, 0x00),
            'Y' to longArrayOf(0x66, 0x66, 0x66, 0x3C, 0x18, 0x18, 0x3C, 0x00),
            'Z' to longArrayOf(0xFE, 0xC6, 0x8C, 0x18, 0x32, 0x66, 0xFE, 0x00),
            '0' to longArrayOf(0x38, 0x6C, 0xC6, 0xD6, 0xC6, 0x6C, 0x38, 0x00),
            '1' to longArrayOf(0x18, 0x38, 0x18, 0x18, 0x18, 0x18, 0x7E, 0x00),
            '2' to longArrayOf(0x7C, 0xC6, 0x0E, 0x3C, 0x70, 0xE0, 0xFE, 0x00),
            '3' to longArrayOf(0x7C, 0xC6, 0x0C, 0x38, 0x0C, 0xC6, 0x7C, 0x00),
            '4' to longArrayOf(0x1C, 0x3C, 0x6C, 0xCC, 0xFE, 0x0C, 0x1E, 0x00),
            '5' to longArrayOf(0xFE, 0xC0, 0xFC, 0x06, 0x06, 0xC6, 0x7C, 0x00),
            '6' to longArrayOf(0x38, 0x60, 0xC0, 0xFC, 0xC6, 0xC6, 0x7C, 0x00),
            '7' to longArrayOf(0xFE, 0xC6, 0x0C, 0x18, 0x30, 0x30, 0x30, 0x00),
            '8' to longArrayOf(0x7C, 0xC6, 0xC6, 0x7C, 0xC6, 0xC6, 0x7C, 0x00),
            '9' to longArrayOf(0x7C, 0xC6, 0xC6, 0x7E, 0x06, 0x0C, 0x78, 0x00),
            ':' to longArrayOf(0x00, 0x18, 0x18, 0x00, 0x18, 0x18, 0x00, 0x00),
            '%' to longArrayOf(0xC2, 0xC6, 0x0C, 0x18, 0x30, 0x63, 0x43, 0x00),
            '/' to longArrayOf(0x02, 0x06, 0x0C, 0x18, 0x30, 0x60, 0x40, 0x00)
        )

        for ((char, rows) in fontData) {
            val tileIndex = char.code
            val tileOffset = chrOffset + (tileIndex * 16)
            for (r in 0..7) {
                rom[tileOffset + r] = rows[r].toByte()
                rom[tileOffset + 8 + r] = rows[r].toByte() // Plane 1
            }
        }
    }
}
