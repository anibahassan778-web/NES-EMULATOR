package com.example.nes.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.sin

/**
 * NES Audio Processing Unit (Ricoh 2A03).
 * Synthesizes Pulse 1, Pulse 2, Triangle, and Noise channels and streams
 * low-latency 16-bit PCM audio directly to an Android AudioTrack.
 */
class Apu2A03 {

    var enabled: Boolean = true
    var volume: Float = 0.8f

    // Sample generation timing (NES CPU clock = 1.789773 MHz, Audio output = 44100 Hz)
    private val sampleRate = 44100
    private val cpuClockRate = 1789773.0
    private val cyclesPerSample = cpuClockRate / sampleRate
    private var cycleAccumulator = 0.0

    // Pulse 1 channel
    private var pulse1Enabled = false
    private var pulse1Duty = 0
    private var pulse1Timer = 0
    private var pulse1Volume = 0
    private var pulse1Phase = 0.0

    // Pulse 2 channel
    private var pulse2Enabled = false
    private var pulse2Duty = 0
    private var pulse2Timer = 0
    private var pulse2Volume = 0
    private var pulse2Phase = 0.0

    // Triangle channel
    private var triangleEnabled = false
    private var triangleTimer = 0
    private var trianglePhase = 0.0

    // Noise channel
    private var noiseEnabled = false
    private var noiseTimer = 0
    private var noiseVolume = 0
    private var noiseShiftRegister = 1

    // Audio streaming buffer
    private val bufferSize = 2048
    private val audioBuffer = ShortArray(bufferSize)
    private var bufferIndex = 0

    private var audioTrack: AudioTrack? = null

    init {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val finalBufSize = minBufSize.coerceAtLeast(bufferSize * 4)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(finalBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (_: Exception) {
            audioTrack = null
        }
    }

    fun cpuWrite(address: Int, value: Int) {
        val v8 = value and 0xFF
        when (address) {
            0x4000 -> { // Pulse 1 control
                pulse1Duty = (v8 shr 6) and 0x03
                pulse1Volume = v8 and 0x0F
            }
            0x4002 -> { // Pulse 1 timer low
                pulse1Timer = (pulse1Timer and 0x700) or v8
            }
            0x4003 -> { // Pulse 1 timer high & length counter
                pulse1Timer = (pulse1Timer and 0x0FF) or ((v8 and 0x07) shl 8)
            }
            0x4004 -> { // Pulse 2 control
                pulse2Duty = (v8 shr 6) and 0x03
                pulse2Volume = v8 and 0x0F
            }
            0x4006 -> { // Pulse 2 timer low
                pulse2Timer = (pulse2Timer and 0x700) or v8
            }
            0x4007 -> { // Pulse 2 timer high & length counter
                pulse2Timer = (pulse2Timer and 0x0FF) or ((v8 and 0x07) shl 8)
            }
            0x4008 -> { // Triangle linear counter
                // linear counter setup
            }
            0x400A -> { // Triangle timer low
                triangleTimer = (triangleTimer and 0x700) or v8
            }
            0x400B -> { // Triangle timer high
                triangleTimer = (triangleTimer and 0x0FF) or ((v8 and 0x07) shl 8)
            }
            0x400C -> { // Noise control
                noiseVolume = v8 and 0x0F
            }
            0x400E -> { // Noise period
                noiseTimer = NOISE_PERIOD_TABLE[v8 and 0x0F]
            }
            0x4015 -> { // Channel enable
                pulse1Enabled = (v8 and 0x01) != 0
                pulse2Enabled = (v8 and 0x02) != 0
                triangleEnabled = (v8 and 0x04) != 0
                noiseEnabled = (v8 and 0x08) != 0
            }
        }
    }

    fun cpuRead(address: Int): Int {
        if (address == 0x4015) {
            var status = 0
            if (pulse1Enabled) status = status or 0x01
            if (pulse2Enabled) status = status or 0x02
            if (triangleEnabled) status = status or 0x04
            if (noiseEnabled) status = status or 0x08
            return status
        }
        return 0
    }

    /**
     * Called on each CPU step to generate synthesized audio samples.
     */
    fun step(cpuCycles: Int) {
        cycleAccumulator += cpuCycles
        while (cycleAccumulator >= cyclesPerSample) {
            cycleAccumulator -= cyclesPerSample
            generateSample()
        }
    }

    private fun generateSample() {
        if (!enabled) {
            audioBuffer[bufferIndex++] = 0
            checkFlushBuffer()
            return
        }

        // Pulse 1 synthesis
        var p1Out = 0.0
        if (pulse1Enabled && pulse1Timer > 8) {
            val freq = cpuClockRate / (16.0 * (pulse1Timer + 1))
            pulse1Phase += freq / sampleRate
            if (pulse1Phase >= 1.0) pulse1Phase -= 1.0
            val dutyThreshold = DUTY_CYCLES[pulse1Duty]
            val high = pulse1Phase < dutyThreshold
            p1Out = (if (high) 1.0 else -1.0) * (pulse1Volume / 15.0)
        }

        // Pulse 2 synthesis
        var p2Out = 0.0
        if (pulse2Enabled && pulse2Timer > 8) {
            val freq = cpuClockRate / (16.0 * (pulse2Timer + 1))
            pulse2Phase += freq / sampleRate
            if (pulse2Phase >= 1.0) pulse2Phase -= 1.0
            val dutyThreshold = DUTY_CYCLES[pulse2Duty]
            val high = pulse2Phase < dutyThreshold
            p2Out = (if (high) 1.0 else -1.0) * (pulse2Volume / 15.0)
        }

        // Triangle synthesis
        var triOut = 0.0
        if (triangleEnabled && triangleTimer > 2) {
            val freq = cpuClockRate / (32.0 * (triangleTimer + 1))
            trianglePhase += freq / sampleRate
            if (trianglePhase >= 1.0) trianglePhase -= 1.0
            // Triangle wave from -1 to 1
            triOut = if (trianglePhase < 0.5) {
                (trianglePhase * 4.0) - 1.0
            } else {
                3.0 - (trianglePhase * 4.0)
            }
        }

        // Noise synthesis
        var noiseOut = 0.0
        if (noiseEnabled && noiseTimer > 0) {
            val feedback = (noiseShiftRegister and 0x01) xor ((noiseShiftRegister shr 1) and 0x01)
            noiseShiftRegister = (noiseShiftRegister shr 1) or (feedback shl 14)
            val bit = (noiseShiftRegister and 0x01)
            noiseOut = (if (bit == 0) 1.0 else -1.0) * (noiseVolume / 15.0)
        }

        // Mix channels
        val mixed = (p1Out * 0.25 + p2Out * 0.25 + triOut * 0.35 + noiseOut * 0.15) * volume
        val pcmSample = (mixed.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()

        audioBuffer[bufferIndex++] = pcmSample
        checkFlushBuffer()
    }

    private fun checkFlushBuffer() {
        if (bufferIndex >= bufferSize) {
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.write(audioBuffer, 0, bufferSize)
                }
            }
            bufferIndex = 0
        }
    }

    fun release() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}
    }

    fun saveState(out: DataOutputStream) {
        out.writeBoolean(enabled)
        out.writeFloat(volume)
        out.writeBoolean(pulse1Enabled)
        out.writeInt(pulse1Duty)
        out.writeInt(pulse1Timer)
        out.writeInt(pulse1Volume)
        out.writeBoolean(pulse2Enabled)
        out.writeInt(pulse2Duty)
        out.writeInt(pulse2Timer)
        out.writeInt(pulse2Volume)
        out.writeBoolean(triangleEnabled)
        out.writeInt(triangleTimer)
        out.writeBoolean(noiseEnabled)
        out.writeInt(noiseTimer)
        out.writeInt(noiseVolume)
        out.writeInt(noiseShiftRegister)
    }

    fun loadState(inp: DataInputStream) {
        enabled = inp.readBoolean()
        volume = inp.readFloat()
        pulse1Enabled = inp.readBoolean()
        pulse1Duty = inp.readInt()
        pulse1Timer = inp.readInt()
        pulse1Volume = inp.readInt()
        pulse2Enabled = inp.readBoolean()
        pulse2Duty = inp.readInt()
        pulse2Timer = inp.readInt()
        pulse2Volume = inp.readInt()
        triangleEnabled = inp.readBoolean()
        triangleTimer = inp.readInt()
        noiseEnabled = inp.readBoolean()
        noiseTimer = inp.readInt()
        noiseVolume = inp.readInt()
        noiseShiftRegister = inp.readInt()
    }

    companion object {
        private val DUTY_CYCLES = doubleArrayOf(0.125, 0.25, 0.50, 0.75)
        private val NOISE_PERIOD_TABLE = intArrayOf(
            4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
        )
    }
}
