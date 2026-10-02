/* NES emulator By ArDev */
package com.example.nes.core

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream

/**
 * Handles NES Cartridge loading and parsing of the iNES file format (.nes).
 * Supports Mapper 0 (NROM), Mapper 1 (MMC1), Mapper 2 (UxROM), and Mapper 3 (CNROM).
 * NES emulator By ArDev
 */
class Cartridge(
    val title: String,
    val mapper: Mapper,
    val prgRomSize: Int,
    val chrRomSize: Int,
    val mapperId: Int,
    val mirroring: MirroringMode
) {
    fun cpuRead(address: Int): Int = mapper.cpuRead(address)
    fun cpuWrite(address: Int, value: Int): Boolean = mapper.cpuWrite(address, value)

    fun ppuRead(address: Int): Int = mapper.ppuRead(address)
    fun ppuWrite(address: Int, value: Int): Boolean = mapper.ppuWrite(address, value)

    fun stepScanline() = mapper.stepScanline()
    fun irqState(): Boolean = mapper.irqState()
    fun clearIrq() = mapper.clearIrq()

    companion object {
        @Throws(IllegalArgumentException::class)
        fun fromBytes(bytes: ByteArray, title: String = "Untitled ROM"): Cartridge {
            return fromInputStream(ByteArrayInputStream(bytes), title)
        }

        @Throws(IllegalArgumentException::class)
        fun fromInputStream(input: InputStream, title: String = "Untitled ROM"): Cartridge {
            val header = ByteArray(16)
            var bytesRead = 0
            while (bytesRead < 16) {
                val r = input.read(header, bytesRead, 16 - bytesRead)
                if (r == -1) break
                bytesRead += r
            }

            if (bytesRead < 16) {
                throw IllegalArgumentException("الملف صغير جداً ولا يحتوي على ترويسة iNES صالحة.")
            }

            // Check "NES\x1A"
            if (header[0] != 0x4E.toByte() || header[1] != 0x45.toByte() ||
                header[2] != 0x53.toByte() || header[3] != 0x1A.toByte()
            ) {
                throw IllegalArgumentException("تنسيق الملف غير صالح: ترويسة iNES غير صحيحة.")
            }

            val prgBanks = header[4].toInt() and 0xFF
            val chrBanks = header[5].toInt() and 0xFF
            val flags6 = header[6].toInt() and 0xFF
            val flags7 = header[7].toInt() and 0xFF

            val mapperId = ((flags7 and 0xF0)) or ((flags6 and 0xF0) shr 4)
            val hasTrainer = (flags6 and 0x04) != 0
            val isFourScreen = (flags6 and 0x08) != 0
            val isVerticalMirroring = (flags6 and 0x01) != 0

            val mirroringMode = when {
                isFourScreen -> MirroringMode.FOUR_SCREEN
                isVerticalMirroring -> MirroringMode.VERTICAL
                else -> MirroringMode.HORIZONTAL
            }

            // Skip 512 bytes trainer if present
            if (hasTrainer) {
                var skipped = 0L
                while (skipped < 512) {
                    val s = input.skip(512 - skipped)
                    if (s <= 0) break
                    skipped += s
                }
            }

            // Read PRG ROM (16KB * prgBanks)
            val prgSize = prgBanks * 16384
            val prgRom = ByteArray(prgSize)
            var readPrg = 0
            while (readPrg < prgSize) {
                val r = input.read(prgRom, readPrg, prgSize - readPrg)
                if (r == -1) break
                readPrg += r
            }

            // Read CHR ROM (8KB * chrBanks). If chrBanks == 0, use 8KB CHR RAM
            val isChrRam = (chrBanks == 0)
            val chrSize = if (isChrRam) 8192 else (chrBanks * 8192)
            val chrRom = ByteArray(chrSize)
            if (!isChrRam) {
                var readChr = 0
                while (readChr < chrSize) {
                    val r = input.read(chrRom, readChr, chrSize - readChr)
                    if (r == -1) break
                    readChr += r
                }
            }

            val mapper: Mapper = when (mapperId) {
                0 -> Mapper0(
                    prgRom = prgRom,
                    chrRom = chrRom,
                    mirroring = mirroringMode,
                    prgBanks = prgBanks,
                    isChrRam = isChrRam
                )
                1 -> Mapper1(
                    prgRom = prgRom,
                    chrRom = chrRom,
                    mirroring = mirroringMode,
                    prgBanks = prgBanks,
                    isChrRam = isChrRam
                )
                2 -> Mapper2(
                    prgRom = prgRom,
                    chrRom = chrRom,
                    mirroring = mirroringMode,
                    prgBanks = prgBanks,
                    isChrRam = isChrRam
                )
                3 -> Mapper3(
                    prgRom = prgRom,
                    chrRom = chrRom,
                    mirroring = mirroringMode,
                    prgBanks = prgBanks,
                    isChrRam = isChrRam
                )
                4 -> Mapper4(
                    prgRom = prgRom,
                    chrRom = chrRom,
                    mirroring = mirroringMode,
                    prgBanks = prgBanks,
                    isChrRam = isChrRam
                )
                7 -> Mapper7(
                    prgRom = prgRom,
                    chrRom = chrRom,
                    mirroring = mirroringMode,
                    prgBanks = prgBanks,
                    isChrRam = isChrRam
                )
                else -> throw IllegalArgumentException("Mapper $mapperId غير مدعوم حالياً (المدعوم: Mappers 0, 1, 2, 3, 4 MMC3, 7 AxROM).")
            }

            return Cartridge(
                title = title,
                mapper = mapper,
                prgRomSize = prgSize,
                chrRomSize = chrSize,
                mapperId = mapperId,
                mirroring = mirroringMode
            )
        }
    }
}
