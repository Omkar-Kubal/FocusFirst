package com.focusfirst

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.focusfirst.analytics.TokiAnalytics
import com.focusfirst.billing.BillingViewModel
import com.focusfirst.billing.ProUpgradeSheet
import com.focusfirst.data.SettingsRepository
import com.focusfirst.ui.components.FirstLaunchDialog
import androidx.activity.compose.BackHandler
import com.focusfirst.ui.screens.HomeScreen
import com.focusfirst.ui.screens.LicensesScreen
import com.focusfirst.ui.screens.SettingsScreen
import com.focusfirst.ui.screens.StatsScreen
import com.focusfirst.ui.theme.FocusFirstTheme
import com.focusfirst.viewmodel.SettingsViewModel
import com.focusfirst.viewmodel.TimerViewModel
import com.focusfirst.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// Navigation
// ============================================================================

private enum class Tab { HOME, STATS, SETTINGS }

private data class TabItem(
    val tab:   Tab,
    val label: String,
    val icon:  ImageVector,
)

private val tabs = listOf(
    TabItem(Tab.HOME,     "TIMER",    Icons.Outlined.Timer),
    TabItem(Tab.STATS,    "STATS",    Icons.Outlined.BarChart),
    TabItem(Tab.SETTINGS, "SETTINGS", Icons.Outlined.Person),
)

// ============================================================================
// MainActivity
// ============================================================================

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    /** Provides access to TimerViewModel for deep link dispatch before Compose is ready. */
    private val timerViewModelByDelegate: com.focusfirst.viewmodel.TimerViewModel by viewModels()

    private val showNotificationRationale = mutableStateOf(false)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                showNotificationRationale.value = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        requestPostNotificationsPermissionIfNeeded()

        // Handle deep link if activity was cold-launched via URI
        intent?.let { handleDeepLink(it) }

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val billingViewModel: BillingViewModel = hiltViewModel()
            val themeMode  by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val amoledMode by settingsViewModel.amoledMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme  = when (themeMode) {
                "Dark"  -> true
                "Light" -> false
                else    -> systemDark
            }

            var selectedTab by rememberSaveable { mutableStateOf(Tab.HOME) }

            // Log each tab switch to Firebase Analytics
            LaunchedEffect(selectedTab) {
                TokiAnalytics.logTabViewed(selectedTab.name)
            }

            val eulaAccepted by settingsViewModel.eulaAccepted
                .collectAsStateWithLifecycle(false)

            FocusFirstTheme(darkTheme = darkTheme, amoledMode = amoledMode) {
                FocusFirstAppContent(
                    showNotificationRationale = showNotificationRationale,
                    selectedTab               = selectedTab,
                    onTabSelected             = { selectedTab = it },
                )

                // Global Pro upgrade sheet — triggered from anywhere in the app
                val showUpgradeSheet by billingViewModel.showUpgradeSheet
                    .collectAsStateWithLifecycle()
                if (showUpgradeSheet) {
                    ProUpgradeSheet(
                        onDismiss        = { billingViewModel.dismissUpgradeSheet() },
                        billingViewModel = billingViewModel,
                    )
                }

                // EULA — shown once on first launch, non-dismissible
                if (!eulaAccepted) {
                    FirstLaunchDialog(
                        onAccept = {
                            settingsViewModel.acceptEula()
                        },
                        onLearnMore = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.privacy_policy_url)),
                            )
                            startActivity(intent)
                        },
                    )
                }
            }
        }
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) return

        lifecycleScope.launch {
            val alreadyAsked = settingsRepository.notificationPermissionAsked.first()
            if (!alreadyAsked) {
                settingsRepository.update(
                    SettingsRepository.KEY_NOTIFICATION_PERMISSION_ASKED,
                    true,
                )
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // ── Deep link handling ────────────────────────────────────────────────────

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    /**
     * Dispatch incoming toki:// URIs to the TimerViewModel.
     *
     * Supported URIs:
     *   toki://timer/start              — start 25-min session
     *   toki://timer/start?duration=15  — start custom duration
     *   toki://timer/start?task=Physics — start with task label
     *   toki://timer/stop               — stop current session
     *   toki://timer/pause              — pause
     *   toki://timer/resume             — resume
     */
    private fun handleDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != "toki") return

        Log.d("MainActivity", "handleDeepLink: $uri")

        when (uri.host) {
            "timer" -> {
                when (uri.path) {
                    "/start" -> {
                        val duration = uri.getQueryParameter("duration")?.toIntOrNull() ?: 25
                        val task     = uri.getQueryParameter("task")
                        timerViewModelByDelegate.startFromDeepLink(duration, task)
                    }
                    "/stop"   -> timerViewModelByDelegate.stop()
                    "/pause"  -> timerViewModelByDelegate.pause()
                    "/resume" -> timerViewModelByDelegate.resume()
                    else -> Log.w("MainActivity", "Unknown deep link path: ${uri.path}")
                }
            }
            else -> Log.w("MainActivity", "Unknown deep link host: ${uri.host}")
        }
    }
}

// ============================================================================
// App shell — bottom navigation + snackbar host
// ============================================================================

@Composable
private fun FocusFirstAppContent(
    showNotificationRationale: State<Boolean>,
    selectedTab:               Tab,
    onTabSelected:             (Tab) -> Unit,
) {
    val snackbarHostState  = remember { SnackbarHostState() }
    val context            = LocalContext.current
    val shouldShowRationale by showNotificationRationale
    var showLicenses by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(shouldShowRationale) {
        if (shouldShowRationale) {
            val result = snackbarHostState.showSnackbar(
                message     = "Notifications needed for timer alerts",
                actionLabel = "Enable",
                duration    = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data  = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = scheme.background,
        snackbarHost   = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = scheme.surfaceContainerHigh,
                    contentColor   = scheme.onSurface,
                    actionColor    = scheme.primary,
                )
            }
        },
        bottomBar = {
            FocusBottomNav(
                selectedTab   = selectedTab,
                onTabSelected = onTabSelected,
            )
        },
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.background)
                .padding(scaffoldPadding),
        ) {
            when (selectedTab) {
                Tab.HOME     -> HomeScreen(
                    onNavigateToSettings = { onTabSelected(Tab.SETTINGS) },
                )
                Tab.STATS    -> StatsScreen(
                    onNavigateToSettings = { onTabSelected(Tab.SETTINGS) },
                )
                Tab.SETTINGS -> if (showLicenses) {
                    BackHandler { showLicenses = false }
                    LicensesScreen(onBack = { showLicenses = false })
                } else {
                    SettingsScreen(
                        onNavigateToLicenses = { showLicenses = true },
                    )
                }
            }
        }
    }
}

// ============================================================================
// Bottom navigation bar
// ============================================================================

@Composable
private fun FocusBottomNav(
    selectedTab:   Tab,
    onTabSelected: (Tab) -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cs.background)
            .padding(horizontal = 24.dp)
            .padding(bottom = 18.dp, top = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(cs.surfaceContainerLow)
                .border(1.dp, cs.outline, RoundedCornerShape(50.dp))
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { item ->
                val isSelected = selectedTab == item.tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .clickable { onTabSelected(item.tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) cs.primary else cs.onSurfaceVariant,
                        modifier = Modifier.size(27.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        color = if (isSelected) cs.primary else cs.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isSelected) cs.primary else Color.Transparent),
                    )
                }
            }
        }
    }
}
