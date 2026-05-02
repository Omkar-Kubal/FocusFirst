package com.focusfirst.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    val billingManager: BillingManager,
) : ViewModel() {

    /** True once the user has a valid, acknowledged purchase for toki_pro. */
    val isPro: StateFlow<Boolean> = billingManager.isPro

    /** Current state of the billing client connection + purchase lifecycle. */
    val billingState: StateFlow<BillingState> = billingManager.billingState

    /** Localized price fetched from Google Play for the active Toki Pro base plan. */
    val proPrice: StateFlow<String?> = billingManager.proPrice

    /** True when subscription product details and offer token are ready for checkout. */
    val isProductReady: StateFlow<Boolean> = billingManager.isProductReady

    /** Emits Unit when the user cancels the billing flow — used to reset loading spinners. */
    val billingCancelled: SharedFlow<Unit> = billingManager.billingCancelled

    /** Controls visibility of the Pro upgrade bottom sheet across the whole app. */
    private val _showUpgradeSheet = MutableStateFlow(false)
    val showUpgradeSheet: StateFlow<Boolean> = _showUpgradeSheet.asStateFlow()

    /**
     * Non-null while a purchase launch error message should be shown.
     * Cleared by [clearPurchaseError] after the snackbar is displayed.
     */
    private val _purchaseError = MutableStateFlow<String?>(null)
    val purchaseError: StateFlow<String?> = _purchaseError.asStateFlow()

    // ─── Sheet control ──────────────────────────────────────────────────────────

    fun openUpgradeSheet() {
        _showUpgradeSheet.value = true
    }

    fun dismissUpgradeSheet() {
        _showUpgradeSheet.value = false
    }

    // ─── Purchase actions ───────────────────────────────────────────────────────

    /**
     * Launches the Play billing flow from [activity].
     * Must be called on the main thread (BillingClient requirement — satisfied by Compose onClick).
     * Any non-OK result is surfaced via [purchaseError].
     */
    fun launchPurchase(activity: Activity) {
        val result = billingManager.launchBillingFlow(activity)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseError.value = "Purchase failed. Try again."
        }
    }

    fun restorePurchases() {
        viewModelScope.launch { billingManager.restorePurchases() }
    }

    fun clearPurchaseError() {
        _purchaseError.value = null
    }
}
