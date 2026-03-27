package com.focusfirst.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfirst.data.SettingsRepository
import com.focusfirst.data.model.AmbientSound
import com.focusfirst.data.model.PlanetSkin
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val focusMinutes: StateFlow<Int> = settingsRepository.focusMinutes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 25,
    )

    val shortBreakMinutes: StateFlow<Int> = settingsRepository.shortBreakMinutes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 5,
    )

    val longBreakMinutes: StateFlow<Int> = settingsRepository.longBreakMinutes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 15,
    )

    val sessionsBeforeLongBreak: StateFlow<Int> =
        settingsRepository.sessionsBeforeLongBreak.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 4,
        )

    val soundType: StateFlow<String> = settingsRepository.soundType.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Bell",
    )

    val vibrate: StateFlow<Boolean> = settingsRepository.vibrate.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true,
    )

    val themeMode: StateFlow<String> = settingsRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "System",
    )

    val amoledMode: StateFlow<Boolean> = settingsRepository.amoledMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    val batteryPromptDismissed: StateFlow<Boolean> =
        settingsRepository.batteryPromptDismissed.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val notificationPermissionAsked: StateFlow<Boolean> =
        settingsRepository.notificationPermissionAsked.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    val proUnlocked: StateFlow<Boolean> = settingsRepository.proUnlocked.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    val dailyGoal: StateFlow<Int> = settingsRepository.dailyGoal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 8,
    )

    val autoStart: StateFlow<Boolean> = settingsRepository.autoStart.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    val ambientSound: StateFlow<AmbientSound> = settingsRepository.ambientSound.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AmbientSound.NONE,
    )

    val ambientVolume: StateFlow<Float> = settingsRepository.ambientVolume.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.5f,
    )

    val dndEnabled: StateFlow<Boolean> = settingsRepository.dndEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    // Default true so existing installs don't show the dialog on update
    val eulaAccepted: StateFlow<Boolean> = settingsRepository.eulaAccepted.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    fun updateFocusMinutes(value: Int) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_FOCUS_MINUTES, value)
        }
    }

    fun updateShortBreakMinutes(value: Int) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_SHORT_BREAK, value)
        }
    }

    fun updateLongBreakMinutes(value: Int) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_LONG_BREAK, value)
        }
    }

    fun updateSessionsBeforeLongBreak(value: Int) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_SESSIONS_BEFORE_LONG_BREAK, value)
        }
    }

    fun updateSoundType(value: String) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_SOUND_TYPE, value)
        }
    }

    fun updateVibrate(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_VIBRATE, value)
        }
    }

    fun updateThemeMode(value: String) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_THEME_MODE, value)
        }
    }

    fun updateAmoledMode(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_AMOLED_MODE, value)
        }
    }

    fun updateBatteryPromptDismissed(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_BATTERY_PROMPT_DISMISSED, value)
        }
    }

    fun updateNotificationPermissionAsked(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_NOTIFICATION_PERMISSION_ASKED, value)
        }
    }

    fun updateProUnlocked(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_PRO_UNLOCKED, value)
        }
    }

    fun updateDailyGoal(value: Int) {
        viewModelScope.launch {
            settingsRepository.update(
                SettingsRepository.KEY_DAILY_GOAL,
                value.coerceIn(1, 20),
            )
        }
    }

    fun updateAutoStart(value: Boolean) {
        viewModelScope.launch {
            settingsRepository.update(SettingsRepository.KEY_AUTO_START, value)
        }
    }

    fun updateAmbientSound(sound: AmbientSound) {
        viewModelScope.launch {
            settingsRepository.updateAmbientSound(sound)
        }
    }

    fun updateAmbientVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.updateAmbientVolume(volume)
        }
    }

    fun updateDndEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDndEnabled(enabled)
        }
    }

    fun acceptEula() {
        viewModelScope.launch {
            settingsRepository.acceptEula()
        }
    }

    val planetSkin: StateFlow<PlanetSkin> = settingsRepository.planetSkin
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlanetSkin.EARTH,
        )

    fun updatePlanetSkin(skin: PlanetSkin) {
        viewModelScope.launch {
            settingsRepository.updatePlanetSkin(skin)
        }
    }
}
