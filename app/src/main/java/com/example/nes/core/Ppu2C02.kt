/* NES emulator By ArDev */
package com.example.nes.core

import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * NES Picture Processing Unit (Ricoh 2C02).
 * Renders 256x240 resolution at 60 FPS with full palette, nametable mirroring,
 * scrolling, sprite evaluation, sprite 0 hit detection, and VBlank NMI.
 * NES emulator By ArDev
 */
class Ppu2C02(private var cartridge: Cartridge?) {

    val frameBuffer = IntArray(256 * 240)

    // 2KB Internal CIRAM (Nametables)
    val vram = ByteArray(2048)

    // 32-byte Palette RAM
    val paletteRam = ByteArray(32)

    // 256-byte OAM (64 sprites * 4 bytes)
    val oam = ByteArray(256)

    // PPU Registers
    var ppuCtrl: Int = 0
    var ppuMask: Int = 0
    var ppuStatus: Int = 0
    var oamAddr: Int = 0

    // Loopy VRAM registers (v, t, x, w)
    var v: Int = 0 // Current VRAM address (15 bits)
    var t: Int = 0 // Temporary VRAM address (15 bits)
    var fineX: Int = 0 // Fine X scroll (3 bits)
    var w: Boolean = false // First or second write toggle

    // Read buffer for PPUDATA (0x2007)
    private var dataBuffer: Int = 0

    // Timing
    var scanline: Int = 0 // 0 to 261
    var cycle: Int = 0    // 0 to 340
    var frameCount: Long = 0

    var nmiTriggered: Boolean = false
    var frameComplete: Boolean = false

    fun setCartridge(cart: Cartridge?) {
        this.cartridge = cart
    }

    fun reset() {
        ppuCtrl = 0
        ppuMask = 0
        ppuStatus = 0
        oamAddr = 0
        v = 0
        t = 0
        fineX = 0
        w = false
        dataBuffer = 0
        scanline = 0
        cycle = 0
        nmiTriggered = false
        frameComplete = false
    }

    // PPU Register Access from CPU (0x2000 - 0x2007)
    fun cpuRead(reg: Int): Int {
        return when (reg and 0x07) {
            0x02 -> { // PPUSTATUS (0x2002)
                val status = (ppuStatus and 0xE0) or (dataBuffer and 0x1F)
                // Clear VBlank flag on read
                ppuStatus = ppuStatus and 0x80.inv()
                w = false // Reset write toggle
                status
            }
            0x04 -> { // OAMDATA (0x2004)
                oam[oamAddr and 0xFF].toInt() and 0xFF
            }
            0x07 -> { // PPUDATA (0x2007)
                var data = ppuRead(v)
                // Reads from 0x0000..0x3EFF are delayed by one cycle through buffer
                if ((v and 0x3FFF) < 0x3F00) {
                    val temp = dataBuffer
                    dataBuffer = data
                    data = temp
                } else {
                    dataBuffer = ppuRead(v - 0x1000)
                }
                // Auto-increment VRAM address
                v = (v + (if ((ppuCtrl and 0x04) != 0) 32 else 1)) and 0x7FFF
                data
            }
            else -> 0
        }
    }

    fun cpuWrite(reg: Int, value: Int) {
        val v8 = value and 0xFF
        when (reg and 0x07) {
            0x00 -> { // PPUCTRL (0x2000)
                val oldNmi = (ppuCtrl and 0x80) != 0
                ppuCtrl = v8
                val newNmi = (ppuCtrl and 0x80) != 0
                if (!oldNmi && newNmi && (ppuStatus and 0x80) != 0) {
                    nmiTriggered = true
                }
                // t: ...GH.. ........ = d: ......GH
                t = (t and 0x73FF) or ((v8 and 0x03) shl 10)
            }
            0x01 -> { // PPUMASK (0x2001)
                ppuMask = v8
            }
            0x03 -> { // OAMADDR (0x2003)
                oamAddr = v8
            }
            0x04 -> { // OAMDATA (0x2004)
                oam[oamAddr and 0xFF] = v8.toByte()
                oamAddr = (oamAddr + 1) and 0xFF
            }
            0x05 -> { // PPUSCROLL (0x2005)
                if (!w) {
                    // First write: X scroll
                    fineX = v8 and 0x07
                    t = (t and 0x7FE0) or (v8 shr 3)
                    w = true
                } else {
                    // Second write: Y scroll
                    t = (t and 0x0C1F) or ((v8 and 0x07) shl 12) or ((v8 and 0xF8) shl 2)
                    w = false
                }
            }
            0x06 -> { // PPUADDR (0x2006)
                if (!w) {
                    // First write: high byte
                    t = (t and 0x00FF) or ((v8 and 0x3F) shl 8)
                    w = true
                } else {
                    // Second write: low byte
                    t = (t and 0x7F00) or v8
                    v = t
                    w = false
                }
            }
            0x07 -> { // PPUDATA (0x2007)
                ppuWrite(v, v8)
                v = (v + (if ((ppuCtrl and 0x04) != 0) 32 else 1)) and 0x7FFF
            }
        }
    }

    // PPU Bus Read (Pattern tables, Nametables, Palettes)
    fun ppuRead(address: Int): Int {
        val addr = address and 0x3FFF
        return when {
            addr < 0x2000 -> {
                cartridge?.ppuRead(addr) ?: 0
            }
            addr < 0x3F00 -> {
                val mirrAddr = mapNametableAddress(addr)
                vram[mirrAddr].toInt() and 0xFF
            }
            else -> {
                // Palette RAM (0x3F00 - 0x3F1F) mirrored to 0x3FFF
                val palAddr = mapPaletteAddress(addr)
                paletteRam[palAddr].toInt() and 0xFF
            }
        }
    }

    fun ppuWrite(address: Int, value: Int) {
        val addr = address and 0x3FFF
        val v8 = (value and 0xFF).toByte()
        when {
            addr < 0x2000 -> {
                cartridge?.ppuWrite(addr, value)
            }
            addr < 0x3F00 -> {
                val mirrAddr = mapNametableAddress(addr)
                vram[mirrAddr] = v8
            }
            else -> {
                val palAddr = mapPaletteAddress(addr)
                paletteRam[palAddr] = v8
            }
        }
    }

    private fun mapPaletteAddress(addr: Int): Int {
        var pal = addr and 0x1F
        // Addresses 0x10, 0x14, 0x18, 0x1C mirror to 0x00, 0x04, 0x08, 0x0C
        if (pal in intArrayOf(0x10, 0x14, 0x18, 0x1C)) {
            pal -= 0x10
        }
        return pal
    }

    private fun mapNametableAddress(addr: Int): Int {
        val nt = (addr - 0x2000) and 0x0FFF
        val mirroring = cartridge?.mirroring ?: MirroringMode.HORIZONTAL
        return when (mirroring) {
            MirroringMode.HORIZONTAL -> {
                when {
                    nt < 0x800 -> nt and 0x3FF
                    else -> 0x400 + (nt and 0x3FF)
                }
            }
            MirroringMode.VERTICAL -> {
                nt and 0x7FF
            }
            MirroringMode.SINGLE_SCREEN_LOWER -> nt and 0x3FF
            MirroringMode.SINGLE_SCREEN_UPPER -> 0x400 + (nt and 0x3FF)
            MirroringMode.FOUR_SCREEN -> nt and 0x7FF
        }
    }

    /**
     * Executes one PPU cycle.
     * NES PPU runs 3 cycles for each CPU cycle.
     */
    fun step(): Boolean {
        var nmi = false

        // Visible Scanlines (0 to 239)
        if (scanline in 0..239) {
            if (cycle == 256) {
                renderScanline(scanline)
            }
        }

        // Post-render scanline 240
        if (scanline == 240 && cycle == 0) {
            frameComplete = true
        }

        // VBlank Scanline 241
        if (scanline == 241 && cycle == 1) {
            ppuStatus = ppuStatus or 0x80 // Set VBlank flag
            if ((ppuCtrl and 0x80) != 0) {
                nmi = true
                nmiTriggered = true
            }
        }

        // Pre-render Scanline 261
        if (scanline == 261 && cycle == 1) {
            ppuStatus = ppuStatus and 0xE0.inv() // Clear VBlank, Sprite 0 hit, overflow
            nmiTriggered = false
            frameComplete = false
        }

        cycle++
        if (cycle >= 341) {
            cycle = 0
            scanline++
            if (scanline >= 262) {
                scanline = 0
                frameCount++
            }
        }

        return nmi
    }

    /**
     * Renders background and sprites for the specified scanline into frameBuffer.
     */
    private fun renderScanline(y: Int) {
        val showBg = (ppuMask and 0x08) != 0
        val showSprites = (ppuMask and 0x10) != 0
        val showBgLeft = (ppuMask and 0x02) != 0
        val showSpritesLeft = (ppuMask and 0x04) != 0

        val universalBgIndex = paletteRam[0].toInt() and 0x3F
        val defaultBgColor = NES_PALETTE[universalBgIndex]

        val bgPixels = IntArray(256) { 0 }
        val bgPaletteIndices = IntArray(256) { 0 }

        // Render Background Scanline
        if (showBg) {
            val bgPatternBase = if ((ppuCtrl and 0x10) != 0) 0x1000 else 0x0000

            // Coarse and fine scroll
            val scrollX = ((t and 0x001F) shl 3) or fineX
            val scrollY = (((t and 0x7000) shr 12) or ((t and 0x03E0) shr 2)) + y

            val fineY = (scrollY) and 0x07
            val tileY = ((scrollY) shr 3) % 30

            for (x in 0..255) {
                if (x < 8 && !showBgLeft) {
                    frameBuffer[y * 256 + x] = defaultBgColor
                    continue
                }

                val currentX = (scrollX + x) and 0x1FF
                val tileX = (currentX shr 3) and 0x1F
                val finePixelX = currentX and 0x07

                // Nametable select (0x2000, 0x2400, 0x2800, 0x2C00)
                val ntX = (currentX shr 8) and 0x01
                val ntY = (scrollY / 240) and 0x01
                val ntIndex = (ntY shl 1) or ntX
                val baseNtAddr = 0x2000 + (ntIndex * 0x400)

                // Nametable byte (tile ID)
                val tileAddr = baseNtAddr + (tileY * 32) + tileX
                val tileId = ppuRead(tileAddr)

                // Pattern table fetch (2 planes of 8 bits each)
                val patternAddr = bgPatternBase + (tileId * 16) + fineY
                val p0 = ppuRead(patternAddr)
                val p1 = ppuRead(patternAddr + 8)

                val bitShift = 7 - finePixelX
                val pixelColorIndex = (((p1 shr bitShift) and 0x01) shl 1) or ((p0 shr bitShift) and 0x01)

                // Attribute table (2x2 tile quadrant attribute)
                val attrAddr = baseNtAddr + 0x3C0 + ((tileY / 4) * 8) + (tileX / 4)
                val attrByte = ppuRead(attrAddr)
                val attrShift = ((tileY and 0x02) shl 1) or (tileX and 0x02)
                val paletteGroup = (attrByte shr attrShift) and 0x03

                bgPaletteIndices[x] = pixelColorIndex
                if (pixelColorIndex != 0) {
                    val palEntry = paletteRam[(paletteGroup * 4) + pixelColorIndex].toInt() and 0x3F
                    bgPixels[x] = NES_PALETTE[palEntry]
                } else {
                    bgPixels[x] = defaultBgColor
                }
                frameBuffer[y * 256 + x] = bgPixels[x]
            }
        } else {
            for (x in 0..255) {
                frameBuffer[y * 256 + x] = defaultBgColor
            }
        }

        // Render Sprites
        if (showSprites) {
            val spriteHeight = if ((ppuCtrl and 0x20) != 0) 16 else 8
            val spritePatternBase = if ((ppuCtrl and 0x08) != 0) 0x1000 else 0x0000

            // Evaluate up to 8 sprites on scanline (in reverse order for priority)
            for (i in 63 downTo 0) {
                val oamOffset = i * 4
                val spriteY = (oam[oamOffset].toInt() and 0xFF) + 1
                val tileIndex = oam[oamOffset + 1].toInt() and 0xFF
                val attributes = oam[oamOffset + 2].toInt() and 0xFF
                val spriteX = oam[oamOffset + 3].toInt() and 0xFF

                if (y in spriteY until (spriteY + spriteHeight)) {
                    val row = y - spriteY
                    val flipH = (attributes and 0x40) != 0
                    val flipV = (attributes and 0x80) != 0
                    val priority = (attributes and 0x20) != 0 // true = behind background
                    val paletteGroup = (attributes and 0x03) + 4

                    val patternRow = if (flipV) (spriteHeight - 1 - row) else row
                    val tileAddr = if (spriteHeight == 8) {
                        spritePatternBase + (tileIndex * 16) + patternRow
                    } else {
                        // 8x16 mode
                        val bank = if ((tileIndex and 0x01) != 0) 0x1000 else 0x0000
                        val actualTile = tileIndex and 0xFE
                        if (patternRow < 8) {
                            bank + (actualTile * 16) + patternRow
                        } else {
                            bank + ((actualTile + 1) * 16) + (patternRow - 8)
                        }
                    }

                    val sp0 = ppuRead(tileAddr)
                    val sp1 = ppuRead(tileAddr + 8)

                    for (col in 0..7) {
                        val px = spriteX + col
                        if (px !in 0..255) continue
                        if (px < 8 && !showSpritesLeft) continue

                        val bitShift = if (flipH) col else (7 - col)
                        val colorIdx = (((sp1 shr bitShift) and 0x01) shl 1) or ((sp0 shr bitShift) and 0x01)

                        if (colorIdx != 0) {
                            // Sprite 0 Hit detection
                            if (i == 0 && showBg && (ppuStatus and 0x40) == 0 && px < 255) {
                                if (bgPaletteIndices[px] != 0) {
                                    ppuStatus = ppuStatus or 0x40 // Set Sprite 0 Hit
                                }
                            }

                            // Render sprite pixel if in front or background is transparent
                            if (!priority || bgPaletteIndices[px] == 0) {
                                val palEntry = paletteRam[(paletteGroup * 4) + colorIdx].toInt() and 0x3F
                                frameBuffer[y * 256 + px] = NES_PALETTE[palEntry]
                            }
                        }
                    }
                }
            }
        }
    }

    fun saveState(out: DataOutputStream) {
        out.write(vram)
        out.write(paletteRam)
        out.write(oam)
        out.writeInt(ppuCtrl)
        out.writeInt(ppuMask)
        out.writeInt(ppuStatus)
        out.writeInt(oamAddr)
        out.writeInt(v)
        out.writeInt(t)
        out.writeInt(fineX)
        out.writeBoolean(w)
        out.writeInt(dataBuffer)
        out.writeInt(scanline)
        out.writeInt(cycle)
        out.writeLong(frameCount)
    }

    fun loadState(inp: DataInputStream) {
        inp.readFully(vram)
        inp.readFully(paletteRam)
        inp.readFully(oam)
        ppuCtrl = inp.readInt()
        ppuMask = inp.readInt()
        ppuStatus = inp.readInt()
        oamAddr = inp.readInt()
        v = inp.readInt()
        t = inp.readInt()
        fineX = inp.readInt()
        w = inp.readBoolean()
        dataBuffer = inp.readInt()
        scanline = inp.readInt()
        cycle = inp.readInt()
        frameCount = inp.readLong()
    }

    companion object {
        // Authentic NES 64-color ARGB Master Palette
        val NES_PALETTE = intArrayOf(
            0xFF666666.toInt(), 0xFF002A88.toInt(), 0xFF1412A7.toInt(), 0xFF3B00A4.toInt(),
            0xFF5C007E.toInt(), 0xFF6E0040.toInt(), 0xFF6C0600.toInt(), 0xFF561D00.toInt(),
            0xFF333500.toInt(), 0xFF0B4800.toInt(), 0xFF005200.toInt(), 0xFF004F08.toInt(),
            0xFF00404D.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),

            0xFFADADAD.toInt(), 0xFF155FD9.toInt(), 0xFF4240FF.toInt(), 0xFF7527FE.toInt(),
            0xFFA01ACC.toInt(), 0xFFB71E7B.toInt(), 0xFFB53120.toInt(), 0xFF994E00.toInt(),
            0xFF6B6D00.toInt(), 0xFF388700.toInt(), 0xFF0C9300.toInt(), 0xFF008F32.toInt(),
            0xFF007C8D.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),

            0xFFFFFEFF.toInt(), 0xFF64B0FF.toInt(), 0xFF9290FF.toInt(), 0xFFC676FF.toInt(),
            0xFFF36AFF.toInt(), 0xFFFE6ECC.toInt(), 0xFFFE8170.toInt(), 0xFFEA9E22.toInt(),
            0xFFBCBE00.toInt(), 0xFF88D800.toInt(), 0xFF5CE430.toInt(), 0xFF45E082.toInt(),
            0xFF48CDDE.toInt(), 0xFF4F4F4F.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(),

            0xFFFFFEFF.toInt(), 0xFFC0DFFF.toInt(), 0xFFD3D2FF.toInt(), 0xFFE8C8FF.toInt(),
            0xFFFBC2FF.toInt(), 0xFFFEC4EA.toInt(), 0xFFFECCC5.toInt(), 0xFFF7D8A5.toInt(),
            0xFFE4E594.toInt(), 0xFFCFEF96.toInt(), 0xFFBDF4AB.toInt(), 0xFFB3F3CC.toInt(),
            0xFFB5EBF2.toInt(), 0xFFB8B8B8.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()
        )
    }
}
