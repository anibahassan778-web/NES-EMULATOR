/* NES emulator By ArDev */
package com.example.nes.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nes.core.Cartridge
import com.example.nes.core.Controller
import com.example.nes.core.HomebrewTestRom
import com.example.nes.core.NesEmulator
import com.example.nes.core.SaveStateManager
import com.example.nes.data.EmulatorSettings
import com.example.nes.data.GameRepository
import com.example.nes.data.RomItem
import com.example.nes.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UiScreen {
    object Library : UiScreen()
    data class Gameplay(val rom: RomItem) : UiScreen()
}

class EmulatorViewModel(application: Application) : AndroidViewModel(application) {

    private val gameRepo = GameRepository(application)
    private val settingsRepo = SettingsRepository(application)
    val saveStateManager = SaveStateManager(application)

    val emulator = NesEmulator()

    private val _currentScreen = MutableStateFlow<UiScreen>(UiScreen.Library)
    val currentScreen: StateFlow<UiScreen> = _currentScreen.asStateFlow()

    private val _games = MutableStateFlow<List<RomItem>>(emptyList())
    val games: StateFlow<List<RomItem>> = _games.asStateFlow()

    private val _settings = MutableStateFlow(settingsRepo.loadSettings())
    val settings: StateFlow<EmulatorSettings> = _settings.asStateFlow()

    private val _currentRom = MutableStateFlow<RomItem?>(null)
    val currentRom: StateFlow<RomItem?> = _currentRom.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isFastForward = MutableStateFlow(false)
    val isFastForward: StateFlow<Boolean> = _isFastForward.asStateFlow()

    private val _currentFps = MutableStateFlow(0)
    val currentFps: StateFlow<Int> = _currentFps.asStateFlow()

    init {
        refreshGames()
        applySettings(_settings.value)
    }

    fun refreshGames() {
        _games.value = gameRepo.getGames()
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun updateSettings(newSettings: EmulatorSettings) {
        _settings.value = newSettings
        settingsRepo.saveSettings(newSettings)
        applySettings(newSettings)
    }

    private fun applySettings(settings: EmulatorSettings) {
        emulator.apu.enabled = settings.audioEnabled
        emulator.apu.volume = settings.audioVolume
    }

    fun launchBuiltInRom() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cartridge = Cartridge.fromBytes(HomebrewTestRom.ROM_BYTES, "NES Hardware Test")
                withContext(Dispatchers.Main) {
                    val rom = gameRepo.getGames().first { it.id == GameRepository.BUILT_IN_ID }
                    _currentRom.value = rom
                    gameRepo.updateLastPlayed(rom.id)
                    emulator.loadCartridge(cartridge)
                    emulator.start(viewModelScope)
                    _isPaused.value = false
                    _currentScreen.value = UiScreen.Gameplay(rom)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("خطأ أثناء تحميل ROM الاختبار: ${e.localizedMessage}")
                }
            }
        }
    }

    fun launchRom(rom: RomItem) {
        if (rom.isBuiltIn || rom.uriString == null) {
            launchBuiltInRom()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(rom.uriString)
                val context = getApplication<Application>()
                val stream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("تعذر فتح مسار الملف.")
                val cartridge = stream.use { Cartridge.fromInputStream(it, rom.title) }

                withContext(Dispatchers.Main) {
                    _currentRom.value = rom
                    gameRepo.updateLastPlayed(rom.id)
                    emulator.loadCartridge(cartridge)
                    emulator.start(viewModelScope)
                    _isPaused.value = false
                    _currentScreen.value = UiScreen.Gameplay(rom)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("فشل تشغيل ${rom.title}: ${e.localizedMessage}")
                }
            }
        }
    }

    fun importRomFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                var fileName = "ROM_${System.currentTimeMillis()}"
                var fileSize = 0L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) fileName = cursor.getString(nameIdx)
                        if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                    }
                }

                // Verify ROM can be parsed
                val checkStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("لا يمكن قراءة الملف المختار.")
                val cart = checkStream.use { Cartridge.fromInputStream(it, fileName) }

                val cleanTitle = fileName.removeSuffix(".nes").removeSuffix(".NES")
                val newRom = gameRepo.addGame(cleanTitle, uri.toString(), fileSize)

                withContext(Dispatchers.Main) {
                    refreshGames()
                    showMessage("تم إضافة اللعبة: $cleanTitle بنجاح!")
                    launchRom(newRom)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showMessage("خطأ أثناء استيراد ملف NES: ${e.localizedMessage}")
                }
            }
        }
    }

    fun toggleFavorite(rom: RomItem) {
        gameRepo.toggleFavorite(rom.id)
        refreshGames()
    }

    fun deleteRom(rom: RomItem) {
        gameRepo.deleteGame(rom.id)
        refreshGames()
    }

    fun exitGameplay() {
        emulator.pause()
        emulator.stop()
        _currentScreen.value = UiScreen.Library
    }

    fun togglePause() {
        if (_isPaused.value) {
            emulator.resume()
            _isPaused.value = false
        } else {
            emulator.pause()
            _isPaused.value = true
        }
    }

    fun resetGame() {
        emulator.reset()
        showMessage("تم إعادة ضبط اللعبة (Reset)")
    }

    fun toggleFastForward() {
        val next = !_isFastForward.value
        _isFastForward.value = next
        emulator.fastForward = next
    }

    fun saveCurrentState(slot: Int) {
        val rom = _currentRom.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val success = saveStateManager.save(rom.id, slot, emulator)
            withContext(Dispatchers.Main) {
                if (success) {
                    showMessage("تم حفظ الحالة في الخانة $slot بنجاح")
                } else {
                    showMessage("فشل حفظ الحالة في الخانة $slot")
                }
            }
        }
    }

    fun loadCurrentState(slot: Int) {
        val rom = _currentRom.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (!saveStateManager.hasSave(rom.id, slot)) {
                withContext(Dispatchers.Main) {
                    showMessage("لا توجد حالة محفوظة في الخانة $slot")
                }
                return@launch
            }
            val success = saveStateManager.load(rom.id, slot, emulator)
            withContext(Dispatchers.Main) {
                if (success) {
                    showMessage("تم استرجاع الحالة من الخانة $slot")
                } else {
                    showMessage("فشل استرجاع الحالة من الخانة $slot")
                }
            }
        }
    }

    fun setButtonPressed(buttonIndex: Int, pressed: Boolean) {
        emulator.bus.controller1.setButtonPressed(buttonIndex, pressed)
    }

    fun handleKeyEvent(keyCode: Int, isDown: Boolean): Boolean {
        val btn = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> Controller.BUTTON_UP
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> Controller.BUTTON_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> Controller.BUTTON_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> Controller.BUTTON_RIGHT
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_X -> Controller.BUTTON_A
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_Z -> Controller.BUTTON_B
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_SHIFT_LEFT -> Controller.BUTTON_SELECT
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER -> Controller.BUTTON_START
            else -> return false
        }
        setButtonPressed(btn, isDown)
        return true
    }

    override fun onCleared() {
        super.onCleared()
        emulator.release()
    }
}
