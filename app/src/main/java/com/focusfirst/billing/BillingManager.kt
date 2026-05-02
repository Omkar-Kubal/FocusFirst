package com.focusfirst.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.focusfirst.analytics.TokiAnalytics
import com.focusfirst.data.SettingsRepository
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// ─── Billing state ─────────────────────────────────────────────────────────────

enum class BillingState { LOADING, READY, UNAVAILABLE, PURCHASED }

// ─── BillingManager ────────────────────────────────────────────────────────────

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    companion object {
        const val PRODUCT_ID = "toki_pro"
        private const val BASE_PLAN_ID = "01-toki-pro"
        private const val TAG = "BillingManager"
        private const val MAX_RETRIES = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _billingState = MutableStateFlow(BillingState.LOADING)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _billingCancelled = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val billingCancelled = _billingCancelled.asSharedFlow()

    private val _proPrice = MutableStateFlow<String?>(null)
    val proPrice: StateFlow<String?> = _proPrice.asStateFlow()

    private val _isProductReady = MutableStateFlow(false)
    val isProductReady: StateFlow<Boolean> = _isProductReady.asStateFlow()

    /** Cached after the first successful query so launchBillingFlow is synchronous. */
    private var cachedProductDetails: ProductDetails? = null
    private var cachedOfferToken: String? = null
    private var retryCount = 0

    // ─── Client setup ───────────────────────────────────────────────────────────

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch { handlePurchases(purchases.orEmpty()) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled billing flow")
                TokiAnalytics.logUpgradeCancelled()
                _billingCancelled.tryEmit(Unit)
            }
            else -> {
                val msg = "Billing error ${billingResult.responseCode}: ${billingResult.debugMessage}"
                Log.e(TAG, msg)
                Firebase.crashlytics.recordException(Exception(msg))
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    init {
        // Pre-load persisted Pro status so _isPro is accurate before billing connects
        scope.launch {
            if (settingsRepository.proUnlocked.first()) _isPro.value = true
        }
        startConnection()
    }

    // ─── Connection ─────────────────────────────────────────────────────────────

    private fun startConnection() {
        _billingState.value = BillingState.LOADING
        _isProductReady.value = false
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    retryCount = 0
                    _billingState.value = BillingState.READY
                    scope.launch {
                        cacheProductDetails(queryProductDetails())
                        restorePurchases()
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.responseCode}")
                    _isProductReady.value = false
                    _billingState.value = BillingState.UNAVAILABLE
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                _isProductReady.value = false
                _billingState.value = BillingState.UNAVAILABLE
                scheduleRetry()
            }
        })
    }

    /**
     * Exponential back-off reconnect: 2 s → 4 s → 8 s (max [MAX_RETRIES] attempts).
     */
    private fun scheduleRetry() {
        if (retryCount >= MAX_RETRIES) {
            Log.e(TAG, "Max billing retries ($MAX_RETRIES) reached")
            return
        }
        val delayMs = (1L shl retryCount) * 2_000L // 2 s, 4 s, 8 s
        retryCount++
        Log.d(TAG, "Reconnecting billing in ${delayMs}ms (attempt $retryCount)")
        scope.launch {
            delay(delayMs)
            if (!billingClient.isReady) startConnection()
        }
    }

    // ─── Product details ────────────────────────────────────────────────────────

    /**
     * Queries Play for [PRODUCT_ID] details.
     * Returns null if the client is not ready or the product is unavailable.
     */
    suspend fun queryProductDetails(): ProductDetails? {
        if (!billingClient.isReady) return null
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        val result = billingClient.queryProductDetails(params)
        return if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            result.productDetailsList?.firstOrNull()
        } else {
            Log.e(TAG, "queryProductDetails failed: ${result.billingResult.responseCode}")
            null
        }
    }

    private fun cacheProductDetails(productDetails: ProductDetails?) {
        cachedProductDetails = productDetails

        val offerDetails = productDetails?.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == BASE_PLAN_ID }
            ?: productDetails?.subscriptionOfferDetails?.firstOrNull()

        cachedOfferToken = offerDetails?.offerToken
        _proPrice.value = offerDetails
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
        _isProductReady.value = productDetails != null && cachedOfferToken != null

        if (productDetails != null && cachedOfferToken == null) {
            Log.e(TAG, "No subscription offer found for product $PRODUCT_ID")
        }
    }

    // ─── Purchase flow ──────────────────────────────────────────────────────────

    /**
     * Launches the Play billing flow for [PRODUCT_ID].
     *
     * Must be called on the **main thread** (BillingClient requirement).
     * Uses [cachedProductDetails] fetched on connection, so this call is synchronous.
     *
     * @return [BillingResult] — check [BillingResult.responseCode] == OK for a successful launch.
     */
    fun launchBillingFlow(activity: Activity): BillingResult {
        if (!billingClient.isReady) {
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                .setDebugMessage("Billing client is not connected")
                .build()
        }

        val details = cachedProductDetails
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .setDebugMessage("Product details not loaded — billing may still be connecting")
                .build()
        val offerToken = cachedOfferToken
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .setDebugMessage("Subscription offer not loaded for base plan $BASE_PLAN_ID")
                .build()

        val productDetailsParams = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build(),
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParams)
            .build()

        return billingClient.launchBillingFlow(activity, flowParams)
    }

    // ─── Purchase handling ──────────────────────────────────────────────────────

    /**
     * Processes a list of purchases from the listener or a restore query.
     * - PURCHASED + unacknowledged → acknowledge, then unlock Pro
     * - PURCHASED + already acknowledged → unlock Pro immediately
     * - PENDING → log only, do not unlock
     */
    suspend fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases.filter { PRODUCT_ID in it.products }) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) {
                        val acked = acknowledgePurchase(purchase)
                        if (acked) unlockPro()
                    } else {
                        unlockPro()
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    Log.d(TAG, "Purchase pending — payment not yet confirmed, not unlocking")
                }
                else -> Log.w(TAG, "Unknown purchase state: ${purchase.purchaseState}")
            }
        }
    }

    private suspend fun unlockPro() {
        _isPro.value = true
        _billingState.value = BillingState.PURCHASED
        settingsRepository.update(SettingsRepository.KEY_PRO_UNLOCKED, true)
        TokiAnalytics.logUpgradePurchased()
        Log.i(TAG, "Pro unlocked and persisted")
    }

    /**
     * Acknowledges a purchase with the Play backend.
     * @return true if the acknowledgement was accepted.
     */
    suspend fun acknowledgePurchase(purchase: Purchase): Boolean {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val result = billingClient.acknowledgePurchase(params)
        return if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Purchase acknowledged")
            true
        } else {
            Log.e(TAG, "Acknowledge failed: ${result.responseCode} — ${result.debugMessage}")
            false
        }
    }

    // ─── Restore purchases ──────────────────────────────────────────────────────

    /**
     * Queries all owned subscription purchases and re-processes them.
     * Called automatically on every connection (app start) and from the UI "Restore" button.
     */
    suspend fun restorePurchases() {
        if (!billingClient.isReady) {
            Log.w(TAG, "restorePurchases called before billing client is ready")
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            handlePurchases(result.purchasesList)
        } else {
            Log.e(TAG, "restorePurchases failed: ${result.billingResult.responseCode}")
        }
    }

    // ─── Cleanup ────────────────────────────────────────────────────────────────

    /**
     * Releases the billing connection and cancels the internal coroutine scope.
     * Call from [android.app.Application.onTerminate] or a lifecycle observer.
     */
    fun destroy() {
        scope.cancel()
        billingClient.endConnection()
        Log.d(TAG, "BillingManager destroyed")
    }
}
