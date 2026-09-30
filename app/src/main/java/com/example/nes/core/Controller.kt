package com.example.nes.core

/**
 * Emulates the NES Controller (shift-register).
 * Buttons: A, B, Select, Start, Up, Down, Left, Right.
 */
class Controller {

    companion object {
        const val BUTTON_A = 0
        const val BUTTON_B = 1
        const val BUTTON_SELECT = 2
        const val BUTTON_START = 3
        const val BUTTON_UP = 4
        const val BUTTON_DOWN = 5
        const val BUTTON_LEFT = 6
        const val BUTTON_RIGHT = 7
    }

    // State of the 8 buttons (true if pressed)
    private val buttonStates = BooleanArray(8)

    // Current shift register latch
    private var shiftRegister: Int = 0
    private var strobe: Boolean = false

    fun setButtonPressed(buttonIndex: Int, pressed: Boolean) {
        if (buttonIndex in 0..7) {
            buttonStates[buttonIndex] = pressed
        }
    }

    fun isButtonPressed(buttonIndex: Int): Boolean {
        return if (buttonIndex in 0..7) buttonStates[buttonIndex] else false
    }

    /**
     * Strobe write from CPU (0x4016).
     * When strobe is 1, shift register continuously reloads button states.
     */
    fun writeStrobe(value: Int) {
        val newStrobe = (value and 0x01) != 0
        if (strobe && !newStrobe) {
            reloadShiftRegister()
        }
        strobe = newStrobe
        if (strobe) {
            reloadShiftRegister()
        }
    }

    /**
     * Read from CPU (0x4016 or 0x4017).
     * Returns 1 if pressed, 0 if not pressed.
     */
    fun read(): Int {
        if (strobe) {
            reloadShiftRegister()
        }
        val result = shiftRegister and 0x01
        shiftRegister = (shiftRegister shr 1) or 0x80 // After 8 reads, returns 1
        return result
    }

    private fun reloadShiftRegister() {
        var bits = 0
        for (i in 0..7) {
            if (buttonStates[i]) {
                bits = bits or (1 shl i)
            }
        }
        shiftRegister = bits
    }
}
