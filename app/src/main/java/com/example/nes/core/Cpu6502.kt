/* NES emulator By ArDev */
package com.example.nes.core

/**
 * Complete MOS 6502 CPU implementation in pure Kotlin for the NES (Ricoh 2A03 core).
 * Supports all official opcodes, 13 addressing modes, interrupts (RESET, NMI, IRQ, BRK),
 * and cycle counting.
 * NES emulator By ArDev
 */
class Cpu6502(private val bus: NesBus) {

    // Registers
    var a: Int = 0          // Accumulator (8-bit)
    var x: Int = 0          // X Index (8-bit)
    var y: Int = 0          // Y Index (8-bit)
    var pc: Int = 0         // Program Counter (16-bit)
    var sp: Int = 0xFD      // Stack Pointer (8-bit)
    var status: Int = 0x34  // Status Flags (NV_BDIZC)

    // Cycle tracking
    var cycles: Long = 0
    var remainingCycles: Int = 0

    // Interrupt flags
    var nmiPending: Boolean = false
    var irqPending: Boolean = false

    companion object {
        const val FLAG_C = 0x01 // Carry
        const val FLAG_Z = 0x02 // Zero
        const val FLAG_I = 0x04 // Interrupt Disable
        const val FLAG_D = 0x08 // Decimal Mode (ignored in 2A03 ALU, but flag exists)
        const val FLAG_B = 0x10 // Break
        const val FLAG_U = 0x20 // Unused (always 1 when pushed)
        const val FLAG_V = 0x40 // Overflow
        const val FLAG_N = 0x80 // Negative
    }

    fun getFlag(flag: Int): Boolean = (status and flag) != 0

    fun setFlag(flag: Int, value: Boolean) {
        status = if (value) (status or flag) else (status and flag.inv())
    }

    private fun updateZeroAndNegative(value: Int) {
        val v = value and 0xFF
        setFlag(FLAG_Z, v == 0)
        setFlag(FLAG_N, (v and 0x80) != 0)
    }

    fun reset() {
        a = 0
        x = 0
        y = 0
        sp = 0xFD
        status = 0x34 // Interrupt flag set, unused bit set
        // Reset vector at 0xFFFC - 0xFFFD
        val lo = bus.cpuRead(0xFFFC)
        val hi = bus.cpuRead(0xFFFD)
        pc = (hi shl 8) or lo
        remainingCycles = 8
        nmiPending = false
        irqPending = false
    }

    fun nmi() {
        nmiPending = true
    }

    fun irq() {
        if (!getFlag(FLAG_I)) {
            irqPending = true
        }
    }

    private fun handleNmi() {
        push16(pc)
        push((status and FLAG_B.inv()) or FLAG_U)
        setFlag(FLAG_I, true)
        val lo = bus.cpuRead(0xFFFA)
        val hi = bus.cpuRead(0xFFFB)
        pc = (hi shl 8) or lo
        remainingCycles += 7
        nmiPending = false
    }

    private fun handleIrq() {
        push16(pc)
        push((status and FLAG_B.inv()) or FLAG_U)
        setFlag(FLAG_I, true)
        val lo = bus.cpuRead(0xFFFE)
        val hi = bus.cpuRead(0xFFFF)
        pc = (hi shl 8) or lo
        remainingCycles += 7
        irqPending = false
    }

    // Stack operations (Stack is at 0x0100 - 0x01FF)
    private fun push(value: Int) {
        bus.cpuWrite(0x0100 or (sp and 0xFF), value and 0xFF)
        sp = (sp - 1) and 0xFF
    }

    private fun pop(): Int {
        sp = (sp + 1) and 0xFF
        return bus.cpuRead(0x0100 or sp) and 0xFF
    }

    private fun push16(value: Int) {
        push((value shr 8) and 0xFF)
        push(value and 0xFF)
    }

    private fun pop16(): Int {
        val lo = pop()
        val hi = pop()
        return (hi shl 8) or lo
    }

    private fun read16(addr: Int): Int {
        val lo = bus.cpuRead(addr)
        val hi = bus.cpuRead(addr + 1)
        return (hi shl 8) or lo
    }

    // Bug in 6502 indirect jump: if address is on page boundary (0x??FF), hi byte comes from 0x??00
    private fun read16Bug(addr: Int): Int {
        val lo = bus.cpuRead(addr)
        val hiAddr = if ((addr and 0x00FF) == 0x00FF) (addr and 0xFF00.toInt()) else (addr + 1)
        val hi = bus.cpuRead(hiAddr)
        return (hi shl 8) or lo
    }

    /**
     * Executes one instruction and returns the cycles consumed.
     */
    fun step(): Int {
        if (nmiPending) {
            handleNmi()
        } else if (irqPending && !getFlag(FLAG_I)) {
            handleIrq()
        }

        val initialCycles = remainingCycles
        val opcode = bus.cpuRead(pc) and 0xFF
        pc = (pc + 1) and 0xFFFF

        var baseCycles = OPCODE_CYCLES[opcode]
        var extraCycles = 0

        when (opcode) {
            // ADC
            0x69 -> { extraCycles += adc(addrImmediate()) }
            0x65 -> { extraCycles += adc(addrZeroPage()) }
            0x75 -> { extraCycles += adc(addrZeroPageX()) }
            0x6D -> { extraCycles += adc(addrAbsolute()) }
            0x7D -> { val p = addrAbsoluteX(); extraCycles += adc(p.first); if (p.second) extraCycles += 1 }
            0x79 -> { val p = addrAbsoluteY(); extraCycles += adc(p.first); if (p.second) extraCycles += 1 }
            0x61 -> { extraCycles += adc(addrIndirectX()) }
            0x71 -> { val p = addrIndirectY(); extraCycles += adc(p.first); if (p.second) extraCycles += 1 }

            // AND
            0x29 -> { extraCycles += andOp(addrImmediate()) }
            0x25 -> { extraCycles += andOp(addrZeroPage()) }
            0x35 -> { extraCycles += andOp(addrZeroPageX()) }
            0x2D -> { extraCycles += andOp(addrAbsolute()) }
            0x3D -> { val p = addrAbsoluteX(); extraCycles += andOp(p.first); if (p.second) extraCycles += 1 }
            0x39 -> { val p = addrAbsoluteY(); extraCycles += andOp(p.first); if (p.second) extraCycles += 1 }
            0x21 -> { extraCycles += andOp(addrIndirectX()) }
            0x31 -> { val p = addrIndirectY(); extraCycles += andOp(p.first); if (p.second) extraCycles += 1 }

            // ASL
            0x0A -> { a = asl(a) }
            0x06 -> { val addr = addrZeroPage(); bus.cpuWrite(addr, asl(bus.cpuRead(addr))) }
            0x16 -> { val addr = addrZeroPageX(); bus.cpuWrite(addr, asl(bus.cpuRead(addr))) }
            0x0E -> { val addr = addrAbsolute(); bus.cpuWrite(addr, asl(bus.cpuRead(addr))) }
            0x1E -> { val p = addrAbsoluteX(); bus.cpuWrite(p.first, asl(bus.cpuRead(p.first))) }

            // BCC / BCS / BEQ / BMI / BNE / BPL / BVC / BVS (Branching)
            0x90 -> { branch(!getFlag(FLAG_C)) }
            0xB0 -> { branch(getFlag(FLAG_C)) }
            0xF0 -> { branch(getFlag(FLAG_Z)) }
            0x30 -> { branch(getFlag(FLAG_N)) }
            0xD0 -> { branch(!getFlag(FLAG_Z)) }
            0x10 -> { branch(!getFlag(FLAG_N)) }
            0x50 -> { branch(!getFlag(FLAG_V)) }
            0x70 -> { branch(getFlag(FLAG_V)) }

            // BIT
            0x24 -> { bit(addrZeroPage()) }
            0x2C -> { bit(addrAbsolute()) }

            // BRK
            0x00 -> {
                pc = (pc + 1) and 0xFFFF
                push16(pc)
                push(status or FLAG_B or FLAG_U)
                setFlag(FLAG_I, true)
                pc = read16(0xFFFE)
            }

            // CLC / CLD / CLI / CLV
            0x18 -> setFlag(FLAG_C, false)
            0xD8 -> setFlag(FLAG_D, false)
            0x58 -> setFlag(FLAG_I, false)
            0xB8 -> setFlag(FLAG_V, false)

            // CMP
            0xC9 -> { cmp(a, addrImmediate()) }
            0xC5 -> { cmp(a, addrZeroPage()) }
            0xD5 -> { cmp(a, addrZeroPageX()) }
            0xCD -> { cmp(a, addrAbsolute()) }
            0xDD -> { val p = addrAbsoluteX(); cmp(a, p.first); if (p.second) extraCycles += 1 }
            0xD9 -> { val p = addrAbsoluteY(); cmp(a, p.first); if (p.second) extraCycles += 1 }
            0xC1 -> { cmp(a, addrIndirectX()) }
            0xD1 -> { val p = addrIndirectY(); cmp(a, p.first); if (p.second) extraCycles += 1 }

            // CPX
            0xE0 -> cmp(x, addrImmediate())
            0xE4 -> cmp(x, addrZeroPage())
            0xEC -> cmp(x, addrAbsolute())

            // CPY
            0xC0 -> cmp(y, addrImmediate())
            0xC4 -> cmp(y, addrZeroPage())
            0xCC -> cmp(y, addrAbsolute())

            // DEC
            0xC6 -> { val addr = addrZeroPage(); val res = (bus.cpuRead(addr) - 1) and 0xFF; bus.cpuWrite(addr, res); updateZeroAndNegative(res) }
            0xD6 -> { val addr = addrZeroPageX(); val res = (bus.cpuRead(addr) - 1) and 0xFF; bus.cpuWrite(addr, res); updateZeroAndNegative(res) }
            0xCE -> { val addr = addrAbsolute(); val res = (bus.cpuRead(addr) - 1) and 0xFF; bus.cpuWrite(addr, res); updateZeroAndNegative(res) }
            0xDE -> { val p = addrAbsoluteX(); val res = (bus.cpuRead(p.first) - 1) and 0xFF; bus.cpuWrite(p.first, res); updateZeroAndNegative(res) }

            // DEX / DEY
            0xCA -> { x = (x - 1) and 0xFF; updateZeroAndNegative(x) }
            0x88 -> { y = (y - 1) and 0xFF; updateZeroAndNegative(y) }

            // EOR
            0x49 -> { extraCycles += eor(addrImmediate()) }
            0x45 -> { extraCycles += eor(addrZeroPage()) }
            0x55 -> { extraCycles += eor(addrZeroPageX()) }
            0x4D -> { extraCycles += eor(addrAbsolute()) }
            0x5D -> { val p = addrAbsoluteX(); extraCycles += eor(p.first); if (p.second) extraCycles += 1 }
            0x59 -> { val p = addrAbsoluteY(); extraCycles += eor(p.first); if (p.second) extraCycles += 1 }
            0x41 -> { extraCycles += eor(addrIndirectX()) }
            0x51 -> { val p = addrIndirectY(); extraCycles += eor(p.first); if (p.second) extraCycles += 1 }

            // INC
            0xE6 -> { val addr = addrZeroPage(); val res = (bus.cpuRead(addr) + 1) and 0xFF; bus.cpuWrite(addr, res); updateZeroAndNegative(res) }
            0xF6 -> { val addr = addrZeroPageX(); val res = (bus.cpuRead(addr) + 1) and 0xFF; bus.cpuWrite(addr, res); updateZeroAndNegative(res) }
            0xEE -> { val addr = addrAbsolute(); val res = (bus.cpuRead(addr) + 1) and 0xFF; bus.cpuWrite(addr, res); updateZeroAndNegative(res) }
            0xFE -> { val p = addrAbsoluteX(); val res = (bus.cpuRead(p.first) + 1) and 0xFF; bus.cpuWrite(p.first, res); updateZeroAndNegative(res) }

            // INX / INY
            0xE8 -> { x = (x + 1) and 0xFF; updateZeroAndNegative(x) }
            0xC8 -> { y = (y + 1) and 0xFF; updateZeroAndNegative(y) }

            // JMP
            0x4C -> { pc = addrAbsolute() }
            0x6C -> { val target = addrAbsolute(); pc = read16Bug(target) }

            // JSR / RTS
            0x20 -> {
                val target = addrAbsolute()
                push16((pc - 1) and 0xFFFF)
                pc = target
            }
            0x60 -> {
                pc = (pop16() + 1) and 0xFFFF
            }

            // LDA
            0xA9 -> { a = bus.cpuRead(addrImmediate()); updateZeroAndNegative(a) }
            0xA5 -> { a = bus.cpuRead(addrZeroPage()); updateZeroAndNegative(a) }
            0xB5 -> { a = bus.cpuRead(addrZeroPageX()); updateZeroAndNegative(a) }
            0xAD -> { a = bus.cpuRead(addrAbsolute()); updateZeroAndNegative(a) }
            0xBD -> { val p = addrAbsoluteX(); a = bus.cpuRead(p.first); updateZeroAndNegative(a); if (p.second) extraCycles += 1 }
            0xB9 -> { val p = addrAbsoluteY(); a = bus.cpuRead(p.first); updateZeroAndNegative(a); if (p.second) extraCycles += 1 }
            0xA1 -> { a = bus.cpuRead(addrIndirectX()); updateZeroAndNegative(a) }
            0xB1 -> { val p = addrIndirectY(); a = bus.cpuRead(p.first); updateZeroAndNegative(a); if (p.second) extraCycles += 1 }

            // LDX
            0xA2 -> { x = bus.cpuRead(addrImmediate()); updateZeroAndNegative(x) }
            0xA6 -> { x = bus.cpuRead(addrZeroPage()); updateZeroAndNegative(x) }
            0xB6 -> { x = bus.cpuRead(addrZeroPageY()); updateZeroAndNegative(x) }
            0xAE -> { x = bus.cpuRead(addrAbsolute()); updateZeroAndNegative(x) }
            0xBE -> { val p = addrAbsoluteY(); x = bus.cpuRead(p.first); updateZeroAndNegative(x); if (p.second) extraCycles += 1 }

            // LDY
            0xA0 -> { y = bus.cpuRead(addrImmediate()); updateZeroAndNegative(y) }
            0xA4 -> { y = bus.cpuRead(addrZeroPage()); updateZeroAndNegative(y) }
            0xB4 -> { y = bus.cpuRead(addrZeroPageX()); updateZeroAndNegative(y) }
            0xAC -> { y = bus.cpuRead(addrAbsolute()); updateZeroAndNegative(y) }
            0xBC -> { val p = addrAbsoluteX(); y = bus.cpuRead(p.first); updateZeroAndNegative(y); if (p.second) extraCycles += 1 }

            // LSR
            0x4A -> { a = lsr(a) }
            0x46 -> { val addr = addrZeroPage(); bus.cpuWrite(addr, lsr(bus.cpuRead(addr))) }
            0x56 -> { val addr = addrZeroPageX(); bus.cpuWrite(addr, lsr(bus.cpuRead(addr))) }
            0x4E -> { val addr = addrAbsolute(); bus.cpuWrite(addr, lsr(bus.cpuRead(addr))) }
            0x5E -> { val p = addrAbsoluteX(); bus.cpuWrite(p.first, lsr(bus.cpuRead(p.first))) }

            // NOP
            0xEA -> { /* No operation */ }

            // ORA
            0x09 -> { extraCycles += ora(addrImmediate()) }
            0x05 -> { extraCycles += ora(addrZeroPage()) }
            0x15 -> { extraCycles += ora(addrZeroPageX()) }
            0x0D -> { extraCycles += ora(addrAbsolute()) }
            0x1D -> { val p = addrAbsoluteX(); extraCycles += ora(p.first); if (p.second) extraCycles += 1 }
            0x19 -> { val p = addrAbsoluteY(); extraCycles += ora(p.first); if (p.second) extraCycles += 1 }
            0x01 -> { extraCycles += ora(addrIndirectX()) }
            0x11 -> { val p = addrIndirectY(); extraCycles += ora(p.first); if (p.second) extraCycles += 1 }

            // PHA / PHP / PLA / PLP
            0x48 -> push(a)
            0x08 -> push(status or FLAG_B or FLAG_U)
            0x68 -> { a = pop(); updateZeroAndNegative(a) }
            0x28 -> { status = (pop() and FLAG_B.inv()) or FLAG_U }

            // ROL
            0x2A -> { a = rol(a) }
            0x26 -> { val addr = addrZeroPage(); bus.cpuWrite(addr, rol(bus.cpuRead(addr))) }
            0x36 -> { val addr = addrZeroPageX(); bus.cpuWrite(addr, rol(bus.cpuRead(addr))) }
            0x2E -> { val addr = addrAbsolute(); bus.cpuWrite(addr, rol(bus.cpuRead(addr))) }
            0x3E -> { val p = addrAbsoluteX(); bus.cpuWrite(p.first, rol(bus.cpuRead(p.first))) }

            // ROR
            0x6A -> { a = ror(a) }
            0x66 -> { val addr = addrZeroPage(); bus.cpuWrite(addr, ror(bus.cpuRead(addr))) }
            0x76 -> { val addr = addrZeroPageX(); bus.cpuWrite(addr, ror(bus.cpuRead(addr))) }
            0x6E -> { val addr = addrAbsolute(); bus.cpuWrite(addr, ror(bus.cpuRead(addr))) }
            0x7E -> { val p = addrAbsoluteX(); bus.cpuWrite(p.first, ror(bus.cpuRead(p.first))) }

            // RTI
            0x40 -> {
                status = (pop() and FLAG_B.inv()) or FLAG_U
                pc = pop16()
            }

            // SBC
            0xE9 -> { extraCycles += sbc(addrImmediate()) }
            0xE5 -> { extraCycles += sbc(addrZeroPage()) }
            0xF5 -> { extraCycles += sbc(addrZeroPageX()) }
            0xED -> { extraCycles += sbc(addrAbsolute()) }
            0xFD -> { val p = addrAbsoluteX(); extraCycles += sbc(p.first); if (p.second) extraCycles += 1 }
            0xF9 -> { val p = addrAbsoluteY(); extraCycles += sbc(p.first); if (p.second) extraCycles += 1 }
            0xE1 -> { extraCycles += sbc(addrIndirectX()) }
            0xF1 -> { val p = addrIndirectY(); extraCycles += sbc(p.first); if (p.second) extraCycles += 1 }

            // SEC / SED / SEI
            0x38 -> setFlag(FLAG_C, true)
            0xF8 -> setFlag(FLAG_D, true)
            0x78 -> setFlag(FLAG_I, true)

            // STA
            0x85 -> bus.cpuWrite(addrZeroPage(), a)
            0x95 -> bus.cpuWrite(addrZeroPageX(), a)
            0x8D -> bus.cpuWrite(addrAbsolute(), a)
            0x9D -> bus.cpuWrite(addrAbsoluteX().first, a)
            0x99 -> bus.cpuWrite(addrAbsoluteY().first, a)
            0x81 -> bus.cpuWrite(addrIndirectX(), a)
            0x91 -> bus.cpuWrite(addrIndirectY().first, a)

            // STX
            0x86 -> bus.cpuWrite(addrZeroPage(), x)
            0x96 -> bus.cpuWrite(addrZeroPageY(), x)
            0x8E -> bus.cpuWrite(addrAbsolute(), x)

            // STY
            0x84 -> bus.cpuWrite(addrZeroPage(), y)
            0x94 -> bus.cpuWrite(addrZeroPageX(), y)
            0x8C -> bus.cpuWrite(addrAbsolute(), y)

            // TAX / TAY / TSX / TXA / TXS / TYA
            0xAA -> { x = a; updateZeroAndNegative(x) }
            0xA8 -> { y = a; updateZeroAndNegative(y) }
            0xBA -> { x = sp; updateZeroAndNegative(x) }
            0x8A -> { a = x; updateZeroAndNegative(a) }
            0x9A -> { sp = x }
            0x98 -> { a = y; updateZeroAndNegative(a) }

            else -> {
                // Unofficial or unrecognized opcodes treated as NOP
            }
        }

        val totalCycles = baseCycles + extraCycles
        remainingCycles += totalCycles
        cycles += totalCycles
        return totalCycles
    }

    // Addressing mode helper functions
    private fun addrImmediate(): Int {
        val addr = pc
        pc = (pc + 1) and 0xFFFF
        return addr
    }

    private fun addrZeroPage(): Int {
        val addr = bus.cpuRead(pc) and 0xFF
        pc = (pc + 1) and 0xFFFF
        return addr
    }

    private fun addrZeroPageX(): Int {
        val base = bus.cpuRead(pc) and 0xFF
        pc = (pc + 1) and 0xFFFF
        return (base + x) and 0xFF
    }

    private fun addrZeroPageY(): Int {
        val base = bus.cpuRead(pc) and 0xFF
        pc = (pc + 1) and 0xFFFF
        return (base + y) and 0xFF
    }

    private fun addrAbsolute(): Int {
        val lo = bus.cpuRead(pc) and 0xFF
        val hi = bus.cpuRead((pc + 1) and 0xFFFF) and 0xFF
        pc = (pc + 2) and 0xFFFF
        return (hi shl 8) or lo
    }

    private fun addrAbsoluteX(): Pair<Int, Boolean> {
        val base = addrAbsolute()
        val addr = (base + x) and 0xFFFF
        val pageCrossed = (base and 0xFF00) != (addr and 0xFF00)
        return Pair(addr, pageCrossed)
    }

    private fun addrAbsoluteY(): Pair<Int, Boolean> {
        val base = addrAbsolute()
        val addr = (base + y) and 0xFFFF
        val pageCrossed = (base and 0xFF00) != (addr and 0xFF00)
        return Pair(addr, pageCrossed)
    }

    private fun addrIndirectX(): Int {
        val base = (bus.cpuRead(pc) + x) and 0xFF
        pc = (pc + 1) and 0xFFFF
        val lo = bus.cpuRead(base) and 0xFF
        val hi = bus.cpuRead((base + 1) and 0xFF) and 0xFF
        return (hi shl 8) or lo
    }

    private fun addrIndirectY(): Pair<Int, Boolean> {
        val ptr = bus.cpuRead(pc) and 0xFF
        pc = (pc + 1) and 0xFFFF
        val lo = bus.cpuRead(ptr) and 0xFF
        val hi = bus.cpuRead((ptr + 1) and 0xFF) and 0xFF
        val base = (hi shl 8) or lo
        val addr = (base + y) and 0xFFFF
        val pageCrossed = (base and 0xFF00) != (addr and 0xFF00)
        return Pair(addr, pageCrossed)
    }

    private fun branch(condition: Boolean) {
        val offset = bus.cpuRead(pc).toByte().toInt()
        pc = (pc + 1) and 0xFFFF
        if (condition) {
            remainingCycles += 1
            val newPc = (pc + offset) and 0xFFFF
            if ((pc and 0xFF00) != (newPc and 0xFF00)) {
                remainingCycles += 1 // Page cross penalty
            }
            pc = newPc
        }
    }

    // ALU operations
    private fun adc(addr: Int): Int {
        val value = bus.cpuRead(addr) and 0xFF
        val carry = if (getFlag(FLAG_C)) 1 else 0
        val sum = a + value + carry
        setFlag(FLAG_C, sum > 0xFF)
        setFlag(FLAG_V, ((a xor sum) and (value xor sum) and 0x80) != 0)
        a = sum and 0xFF
        updateZeroAndNegative(a)
        return 0
    }

    private fun sbc(addr: Int): Int {
        val value = (bus.cpuRead(addr) and 0xFF) xor 0xFF
        val carry = if (getFlag(FLAG_C)) 1 else 0
        val sum = a + value + carry
        setFlag(FLAG_C, sum > 0xFF)
        setFlag(FLAG_V, ((a xor sum) and (value xor sum) and 0x80) != 0)
        a = sum and 0xFF
        updateZeroAndNegative(a)
        return 0
    }

    private fun andOp(addr: Int): Int {
        a = a and bus.cpuRead(addr)
        updateZeroAndNegative(a)
        return 0
    }

    private fun ora(addr: Int): Int {
        a = a or bus.cpuRead(addr)
        updateZeroAndNegative(a)
        return 0
    }

    private fun eor(addr: Int): Int {
        a = a xor bus.cpuRead(addr)
        updateZeroAndNegative(a)
        return 0
    }

    private fun cmp(reg: Int, addr: Int) {
        val value = bus.cpuRead(addr) and 0xFF
        val diff = reg - value
        setFlag(FLAG_C, reg >= value)
        setFlag(FLAG_Z, (diff and 0xFF) == 0)
        setFlag(FLAG_N, (diff and 0x80) != 0)
    }

    private fun bit(addr: Int) {
        val value = bus.cpuRead(addr) and 0xFF
        setFlag(FLAG_Z, (a and value) == 0)
        setFlag(FLAG_N, (value and 0x80) != 0)
        setFlag(FLAG_V, (value and 0x40) != 0)
    }

    private fun asl(value: Int): Int {
        val v = value and 0xFF
        setFlag(FLAG_C, (v and 0x80) != 0)
        val result = (v shl 1) and 0xFF
        updateZeroAndNegative(result)
        return result
    }

    private fun lsr(value: Int): Int {
        val v = value and 0xFF
        setFlag(FLAG_C, (v and 0x01) != 0)
        val result = v shr 1
        updateZeroAndNegative(result)
        return result
    }

    private fun rol(value: Int): Int {
        val v = value and 0xFF
        val carry = if (getFlag(FLAG_C)) 1 else 0
        setFlag(FLAG_C, (v and 0x80) != 0)
        val result = ((v shl 1) or carry) and 0xFF
        updateZeroAndNegative(result)
        return result
    }

    private fun ror(value: Int): Int {
        val v = value and 0xFF
        val carry = if (getFlag(FLAG_C)) 0x80 else 0
        setFlag(FLAG_C, (v and 0x01) != 0)
        val result = (v shr 1) or carry
        updateZeroAndNegative(result)
        return result
    }

    // Lookup table for standard opcode base cycles
    private val OPCODE_CYCLES = intArrayOf(
        7, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        6, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
        2, 6, 2, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5,
        2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
        2, 5, 2, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4,
        2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7
    )
}
