package com.focusfirst.billing

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

private val SheetBackground = Color(0xFF121212)

private val proFeatures = listOf(
    "8 ambient focus sounds",
    "Night sky Pro skins — Ember, Sakura, Crystal",
    "Subject tagging — track time per topic",
    "Detailed analytics + monthly heatmap",
    "Unlimited session history",
    "CSV & PDF export",
    "AMOLED pure black mode",
    "Lock screen timer",
    "All future Pro features",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProUpgradeSheet(
    onDismiss: () -> Unit,
    onNavigateToPlanet: () -> Unit = {},
    billingViewModel: BillingViewModel = hiltViewModel(),
) {
    val activity = LocalContext.current as? Activity
    val isPro by billingViewModel.isPro.collectAsStateWithLifecycle()
    val purchaseError by billingViewModel.purchaseError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(false) }

    // Auto-dismiss 2 s after purchase succeeds
    LaunchedEffect(isPro) {
        if (isPro) {
            delay(2_000)
            onDismiss()
        }
    }

    // Reset loading spinner if billing client goes unavailable
    LaunchedEffect(billingViewModel.billingState) {
        billingViewModel.billingState.collect { state ->
            if (state == BillingState.UNAVAILABLE) isLoading = false
        }
    }

    // Show error snackbar when a purchase launch fails
    LaunchedEffect(purchaseError) {
        val error = purchaseError ?: return@LaunchedEffect
        isLoading = false
        snackbarHostState.showSnackbar(error)
        billingViewModel.clearPurchaseError()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
            )
        },
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isPro) {
                    PurchasedContent(
                        onNavigateToPlanet = onNavigateToPlanet,
                    )
                } else {
                    UpgradeContent(
                        isLoading  = isLoading,
                        onBuyClick = {
                            if (activity != null && !isLoading) {
                                isLoading = true
                                billingViewModel.launchPurchase(activity)
                                // Reset spinner after a short timeout in case the
                                // billing sheet appears without a quick response
                                isLoading = false
                            }
                        },
                        onRestoreClick  = { billingViewModel.restorePurchases() },
                        onDismissClick  = onDismiss,
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier  = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            ) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = Color(0xFF333333),
                    contentColor   = Color.White,
                )
            }
        }
    }
}

// ─── Purchased state ────────────────────────────────────────────────────────────

@Composable
private fun PurchasedContent(
    onNavigateToPlanet: () -> Unit,
) {
    Spacer(Modifier.height(32.dp))
    Icon(
        imageVector        = Icons.Outlined.CheckCircle,
        contentDescription = null,
        tint               = Color(0xFF34C759),
        modifier           = Modifier.size(72.dp),
    )
    Spacer(Modifier.height(20.dp))
    Text(
        text       = "You're Pro!",
        fontSize   = 28.sp,
        fontWeight = FontWeight.Bold,
        color      = Color.White,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text     = "You're Pro! Go choose your world.",
        fontSize = 15.sp,
        color    = Color.White.copy(alpha = 0.65f),
    )
    Spacer(Modifier.height(24.dp))
    Button(
        onClick  = onNavigateToPlanet,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape  = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor   = Color.Black,
        ),
    ) {
        Text(
            text       = "Choose skin",
            fontSize   = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    Spacer(Modifier.height(24.dp))
}

// ─── Upgrade (pre-purchase) state ───────────────────────────────────────────────

@Composable
private fun UpgradeContent(
    isLoading:      Boolean,
    onBuyClick:     () -> Unit,
    onRestoreClick: () -> Unit,
    onDismissClick: () -> Unit,
) {
    Spacer(Modifier.height(8.dp))

    // Title
    Text(
        text       = "Unlock Toki Pro",
        fontSize   = 24.sp,
        fontWeight = FontWeight.Bold,
        color      = Color.White,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text     = "One-time purchase. No subscription. Ever.",
        fontSize = 13.sp,
        color    = Color.White.copy(alpha = 0.5f),
    )

    Spacer(Modifier.height(20.dp))

    // Price badge
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Text(
            text       = "₹149",
            fontSize   = 36.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
        )
    }

    Spacer(Modifier.height(28.dp))

    // Feature list
    Column(
        modifier              = Modifier.fillMaxWidth(),
        verticalArrangement   = Arrangement.spacedBy(14.dp),
    ) {
        proFeatures.forEach { feature ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint               = Color(0xFF34C759),
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text     = feature,
                    fontSize = 14.sp,
                    color    = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }

    Spacer(Modifier.height(32.dp))

    // Primary CTA
    Button(
        onClick  = onBuyClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape  = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor   = Color.Black,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(22.dp),
                color       = Color.Black,
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(
                text       = "Unlock Pro — ₹149",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    Spacer(Modifier.height(4.dp))

    // Restore
    TextButton(
        onClick  = onRestoreClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text     = "Restore purchase",
            fontSize = 14.sp,
            color    = Color.White.copy(alpha = 0.5f),
        )
    }

    // Dismiss
    TextButton(
        onClick  = onDismissClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text     = "Maybe later",
            fontSize = 14.sp,
            color    = Color.White.copy(alpha = 0.35f),
        )
    }
}
