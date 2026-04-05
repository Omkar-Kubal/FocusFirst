package com.focusfirst.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfirst.data.db.SessionEntity
import com.focusfirst.data.model.AllBadges
import com.focusfirst.data.model.Badge
import com.focusfirst.data.SettingsRepository
import com.focusfirst.util.BadgeEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * BadgeViewModel
 *
 * Lightweight ViewModel used exclusively by StatsScreen to display
 * the Achievements / Badges section.
 *
 * It merges:
 *  • The master badge definitions (AllBadges.list)
 *  • The set of unlocked badge IDs stored in SettingsRepository
 *
 * into a single [badges] StateFlow for the UI to observe.
 */
@HiltViewModel
class BadgeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /**
     * Full list of badges with unlock state applied.
     * Unlocked badges carry [Badge.isUnlocked] = true and [Badge.unlockedAt].
     */
    val badges: StateFlow<List<Badge>> = settingsRepository.unlockedBadges
        .combine(settingsRepository.unlockedBadgeTimes) { unlockedIds, times ->
            AllBadges.list.map { badge ->
                badge.copy(
                    isUnlocked = badge.id in unlockedIds,
                    unlockedAt = times[badge.id],
                )
            }
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AllBadges.list,
        )
}
