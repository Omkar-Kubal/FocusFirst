package com.focusfirst.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfirst.data.SettingsRepository
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
        initialValue = "Dark",
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
}
