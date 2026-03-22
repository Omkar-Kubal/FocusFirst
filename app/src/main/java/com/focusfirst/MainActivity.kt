package com.focusfirst

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.focusfirst.ui.screen.HomeScreen
import com.focusfirst.ui.screen.SettingsScreen
import com.focusfirst.ui.screen.StatsScreen
import com.focusfirst.ui.theme.FocusFirstTheme
import dagger.hilt.android.AndroidEntryPoint

// ── Navigation route constants ────────────────────────────────────────────────
// Defined at file level so they can be referenced from deep-link or test code
// without importing the Activity class itself.
const val ROUTE_HOME     = "home"
const val ROUTE_STATS    = "stats"
const val ROUTE_SETTINGS = "settings"

// ============================================================================
// MainActivity
// ============================================================================

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Registered before onCreate — required by the ActivityResult API contract.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Result intentionally ignored: the timer operates without POST_NOTIFICATIONS;
            // the user can re-enable from system settings at any time.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // enableEdgeToEdge() must be called BEFORE super.onCreate() so that
        // the window inset configuration is applied before the view hierarchy attaches.
        enableEdgeToEdge()
        // Explicitly opt out of decor-fits-system-windows so that our Compose
        // safeDrawingPadding / WindowInsets handling takes full control.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        requestPostNotificationsPermissionIfNeeded()

        setContent {
            // V1: darkTheme from system setting, amoledMode disabled.
            // V2: both values come from a DataStore-backed SettingsViewModel
            //     and are hoisted here so the entire NavHost re-themes atomically.
            FocusFirstTheme(
                darkTheme  = isSystemInDarkTheme(),
                amoledMode = false,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()
                    FocusFirstNavHost(navController)
                }
            }
        }
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

            if (!alreadyGranted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

// ============================================================================
// Navigation host
// ============================================================================

@Composable
private fun FocusFirstNavHost(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = ROUTE_HOME,
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                onNavigateToStats    = { navController.navigate(ROUTE_STATS) },
                onNavigateToSettings = { navController.navigate(ROUTE_SETTINGS) },
            )
        }

        composable(ROUTE_STATS) {
            StatsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
