/* NES emulator By ArDev */
package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.nes.ui.EmulatorViewModel
import com.example.nes.ui.GameScreen
import com.example.nes.ui.LibraryScreen
import com.example.nes.ui.UiScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NesDarkBackground

class MainActivity : ComponentActivity() {

    private val viewModel: EmulatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val screen by viewModel.currentScreen.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NesDarkBackground)
                ) {
                    when (val current = screen) {
                        is UiScreen.Library -> {
                            LibraryScreen(
                                viewModel = viewModel,
                                onLaunchRom = { rom ->
                                    viewModel.launchRom(rom)
                                }
                            )
                        }
                        is UiScreen.Gameplay -> {
                            GameScreen(
                                rom = current.rom,
                                viewModel = viewModel,
                                onExit = {
                                    viewModel.exitGameplay()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Support for physical USB and Bluetooth Gamepads / Keyboards
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.handleKeyEvent(keyCode, true)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.handleKeyEvent(keyCode, false)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        event?.let {
            val dpadX = it.getAxisValue(MotionEvent.AXIS_HAT_X)
            val dpadY = it.getAxisValue(MotionEvent.AXIS_HAT_Y)
            if (dpadX != 0f || dpadY != 0f) {
                viewModel.setButtonPressed(com.example.nes.core.Controller.BUTTON_LEFT, dpadX < -0.5f)
                viewModel.setButtonPressed(com.example.nes.core.Controller.BUTTON_RIGHT, dpadX > 0.5f)
                viewModel.setButtonPressed(com.example.nes.core.Controller.BUTTON_UP, dpadY < -0.5f)
                viewModel.setButtonPressed(com.example.nes.core.Controller.BUTTON_DOWN, dpadY > 0.5f)
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }
}
