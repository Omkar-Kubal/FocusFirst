package com.focusfirst.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfirst.data.SettingsRepository
import com.focusfirst.data.remote.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// SyncState — sealed UI state for the cloud sync operation
// ─────────────────────────────────────────────────────────────────────────────

sealed class SyncState {
    object Idle    : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

// ─────────────────────────────────────────────────────────────────────────────
// SyncViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Drives the Cloud Sync section in [SettingsScreen].
 *
 * All Firestore operations are gated behind [isPro] so free users can never
 * accidentally trigger a sync or incur Firestore reads.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val settingsRepository:  SettingsRepository,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = settingsRepository.proUnlocked
        .stateIn(
            scope          = viewModelScope,
            started        = SharingStarted.Eagerly,
            initialValue   = false,
        )

    var syncState: SyncState by mutableStateOf(SyncState.Idle)
        private set

    // ── Public API ────────────────────────────────────────────────────────────

    /** Upload all unsynced sessions to Firestore. Pro only. */
    fun syncNow() {
        if (!isPro.value) return
        viewModelScope.launch {
            syncState = SyncState.Syncing
            try {
                firestoreRepository.ensureAuthenticated()
                firestoreRepository.syncSessionsToCloud()
                syncState = SyncState.Success
                delay(2_000)
                syncState = SyncState.Idle
            } catch (e: Exception) {
                syncState = SyncState.Error(e.message ?: "Sync failed")
            }
        }
    }

    /** Download cloud sessions back into Room. Pro only. */
    fun restoreFromCloud() {
        if (!isPro.value) return
        viewModelScope.launch {
            syncState = SyncState.Syncing
            try {
                firestoreRepository.ensureAuthenticated()
                firestoreRepository.restoreFromCloud()
                syncState = SyncState.Success
                delay(2_000)
                syncState = SyncState.Idle
            } catch (e: Exception) {
                syncState = SyncState.Error(e.message ?: "Restore failed")
            }
        }
    }
}
