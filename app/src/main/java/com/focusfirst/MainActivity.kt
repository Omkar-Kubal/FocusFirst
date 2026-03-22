package com.focusfirst

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.focusfirst.ui.screens.HomeScreen
import com.focusfirst.ui.screens.SettingsScreen
import com.focusfirst.ui.screens.StatsScreen
import com.focusfirst.ui.theme.FocusColors
import com.focusfirst.ui.theme.FocusFirstTheme
import dagger.hilt.android.AndroidEntryPoint

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
    TabItem(Tab.HOME,     "TIMER",   Icons.Outlined.Timer),
    TabItem(Tab.STATS,    "STATS",   Icons.Outlined.BarChart),
    TabItem(Tab.SETTINGS, "PROFILE", Icons.Outlined.Person),
)

// ============================================================================
// MainActivity
// ============================================================================

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Result intentionally ignored — timer works without POST_NOTIFICATIONS.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        requestPostNotificationsPermissionIfNeeded()

        setContent {
            FocusFirstTheme {
                FocusFirstAppContent()
            }
        }
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

// ============================================================================
// App shell — bottom navigation
// ============================================================================

@Composable
private fun FocusFirstAppContent() {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.HOME) }

    Scaffold(
        containerColor = FocusColors.Background,
        bottomBar = {
            FocusBottomNav(
                selectedTab   = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        },
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FocusColors.Background)
                .padding(scaffoldPadding),
        ) {
            when (selectedTab) {
                Tab.HOME     -> HomeScreen(
                    onNavigateToSettings = { selectedTab = Tab.SETTINGS },
                )
                Tab.STATS    -> StatsScreen()
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
    NavigationBar(
        containerColor = FocusColors.SurfaceContainerLow,
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
                    selectedIconColor   = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.4f),
                    selectedTextColor   = Color.White,
                    unselectedTextColor = Color.White.copy(alpha = 0.4f),
                    indicatorColor      = Color.White.copy(alpha = 0.12f),
                ),
            )
        }
    }
}
