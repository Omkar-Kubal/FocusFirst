package com.focusfirst

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.focusfirst.billing.BillingViewModel
import com.focusfirst.billing.ProUpgradeSheet
import com.focusfirst.data.SettingsRepository
import com.focusfirst.ui.screens.HomeScreen
import com.focusfirst.ui.screens.PlanetScreen
import com.focusfirst.ui.screens.SettingsScreen
import com.focusfirst.ui.screens.StatsScreen
import com.focusfirst.ui.theme.FocusFirstTheme
import com.focusfirst.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// Navigation
// ============================================================================

private enum class Tab { HOME, PLANET, STATS, SETTINGS }

private data class TabItem(
    val tab:   Tab,
    val label: String,
    val icon:  ImageVector,
)

private val tabs = listOf(
    TabItem(Tab.HOME,     "TIMER",   Icons.Outlined.Timer),
    TabItem(Tab.PLANET,   "WORLD",   Icons.Outlined.Public),
    TabItem(Tab.STATS,    "STATS",   Icons.Outlined.BarChart),
    TabItem(Tab.SETTINGS, "PROFILE", Icons.Outlined.Person),
)

// ============================================================================
// MainActivity
// ============================================================================

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    /**
     * Mutable state read by the Compose layer to trigger a snackbar when the
     * POST_NOTIFICATIONS permission is denied.
     */
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

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val billingViewModel: BillingViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val amoledMode by settingsViewModel.amoledMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "Dark"  -> true
                "Light" -> false
                else    -> systemDark
            }

            var selectedTab by rememberSaveable { mutableStateOf(Tab.HOME) }

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
                        onDismiss          = { billingViewModel.dismissUpgradeSheet() },
                        onNavigateToPlanet = {
                            selectedTab = Tab.PLANET
                            billingViewModel.dismissUpgradeSheet()
                        },
                        billingViewModel   = billingViewModel,
                    )
                }
            }
        }
    }

    /**
     * Requests POST_NOTIFICATIONS on Android 13+ exactly once.
     *
     * The "already asked" flag is persisted in [SettingsRepository] so the
     * system dialog never appears on subsequent launches.  If the user denies
     * the permission, a Snackbar is shown via [showNotificationRationale].
     */
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
}

// ============================================================================
// App shell — bottom navigation + snackbar host
// ============================================================================

@Composable
private fun FocusFirstAppContent(
    showNotificationRationale: State<Boolean>,
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val shouldShowRationale by showNotificationRationale

    // Show snackbar once when the permission result comes back as denied.
    LaunchedEffect(shouldShowRationale) {
        if (shouldShowRationale) {
            val result = snackbarHostState.showSnackbar(
                message    = "Notifications needed for timer alerts",
                actionLabel = "Enable",
                duration   = SnackbarDuration.Long,
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
                    snackbarData     = data,
                    containerColor   = scheme.surfaceContainerHigh,
                    contentColor     = scheme.onSurface,
                    actionColor      = scheme.primary,
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
                Tab.PLANET   -> PlanetScreen()
                Tab.STATS    -> StatsScreen(
                    onNavigateToSettings = { onTabSelected(Tab.SETTINGS) },
                )
                Tab.SETTINGS -> SettingsScreen()
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
    val scheme = MaterialTheme.colorScheme

    NavigationBar(
        containerColor = scheme.surfaceContainerLow,
    ) {
        tabs.forEach { item ->
            val isSelected = selectedTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick  = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector        = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text     = item.label,
                        fontSize = 10.sp,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor       = scheme.onSurface,
                    unselectedIconColor     = scheme.onSurfaceVariant,
                    selectedTextColor       = scheme.onSurface,
                    unselectedTextColor     = scheme.onSurfaceVariant,
                    indicatorColor          = scheme.primary.copy(alpha = 0.12f),
                ),
            )
        }
    }
}
