/* NES emulator By ArDev */
package com.example.nes.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Master NES Emulator Engine.
 * Coordinates CPU, PPU, APU, Bus, Cartridge, Controllers and the 60 FPS master loop.
 * NES emulator By ArDev
 */
class NesEmulator {

    val bus = NesBus()
    val cpu = Cpu6502(bus)
    val ppu = Ppu2C02(null)
    val apu = Apu2A03()

    var cartridge: Cartridge? = null
        private set

    val isRunning = AtomicBoolean(false)
    val isPaused = AtomicBoolean(false)
    var fastForward: Boolean = false

    var currentFps: Int = 0
        private set

    private var emulationJob: Job? = null
    private var onFrameRendered: ((IntArray) -> Unit)? = null

    init {
        bus.cpu = cpu
        bus.ppu = ppu
        bus.apu = apu
    }

    fun setOnFrameRenderedListener(listener: (IntArray) -> Unit) {
        this.onFrameRendered = listener
    }

    fun loadCartridge(cart: Cartridge) {
        pause()
        this.cartridge = cart
        bus.cartridge = cart
        ppu.setCartridge(cart)
        reset()
    }

    fun reset() {
        bus.reset()
    }

    /**
     * Executes enough CPU and PPU cycles to complete one video frame (256x240).
     */
    fun runFrame(): IntArray {
        ppu.frameComplete = false

        // A NES frame has 262 scanlines * 341 cycles = 89,342 PPU cycles
        // Approximately 29,780 CPU cycles
        var cycleBudget = 30000

        while (!ppu.frameComplete && cycleBudget > 0) {
            val cpuCycles = cpu.step() + bus.dmaCycles
            bus.dmaCycles = 0
            cycleBudget -= cpuCycles

            // 3 PPU cycles per CPU cycle
            for (p in 0 until (cpuCycles * 3)) {
                val nmi = ppu.step()
                if (nmi) {
                    cpu.nmi()
                }
            }

            // APU sound synthesis
            apu.step(cpuCycles)
        }

        return ppu.frameBuffer
    }

    fun start(scope: CoroutineScope) {
        if (isRunning.getAndSet(true)) return
        isPaused.set(false)

        emulationJob = scope.launch(Dispatchers.Default) {
            var lastTime = System.nanoTime()
            var frameCount = 0
            var fpsTimer = System.currentTimeMillis()

            val targetFrameTimeNs = 16_666_666L // ~60 FPS (16.6ms)

            while (isActive && isRunning.get()) {
                if (isPaused.get()) {
                    Thread.sleep(20)
                    lastTime = System.nanoTime()
                    continue
                }

                val frameStart = System.nanoTime()
                val buffer = runFrame()
                onFrameRendered?.invoke(buffer)

                frameCount++
                val now = System.currentTimeMillis()
                if (now - fpsTimer >= 1000) {
                    currentFps = frameCount
                    frameCount = 0
                    fpsTimer = now
                }

                val elapsedNs = System.nanoTime() - frameStart
                val frameDelayNs = if (fastForward) (targetFrameTimeNs / 2) else targetFrameTimeNs
                val sleepNs = frameDelayNs - elapsedNs

                if (sleepNs > 0) {
                    val sleepMs = sleepNs / 1_000_000L
                    val sleepRemNs = (sleepNs % 1_000_000L).toInt()
                    try {
                        Thread.sleep(sleepMs, sleepRemNs)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }
    }

    fun pause() {
        isPaused.set(true)
    }

    fun resume() {
        isPaused.set(false)
    }

    fun stop() {
        isRunning.set(false)
        isPaused.set(false)
        emulationJob?.cancel()
        emulationJob = null
    }

    fun release() {
        stop()
        apu.release()
    }
}
