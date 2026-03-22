package com.focusfirst.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfirst.ui.theme.FocusColors
import kotlin.math.roundToInt

// ============================================================================
// SettingsScreen
//
// V1: all settings stored in local remember state — fast to build and navigate.
// V2: replace remember state with DataStore-backed SettingsViewModel bindings.
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {

    // ── Local settings state (V1 — no persistence) ────────────────────────────
    var sessionsBeforeLongBreak by remember { mutableStateOf(4) }
    var shortBreak              by remember { mutableStateOf(5) }
    var longBreak               by remember { mutableStateOf(15) }
    var soundType               by remember { mutableStateOf("Bell") }
    var vibrateEnabled          by remember { mutableStateOf(true) }
    var themeMode               by remember { mutableStateOf("System") }
    var amoledMode              by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {

            // ── SECTION: Timer ────────────────────────────────────────────────
            SectionHeader("Timer")

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // Sessions before long break
                Text(
                    text  = "Sessions before long break",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4, 6).forEach { option ->
                        val selected = sessionsBeforeLongBreak == option
                        TextButton(
                            onClick  = { sessionsBeforeLongBreak = option },
                            modifier = Modifier.border(
                                width = 1.dp,
                                color = if (selected) FocusColors.TomatoRed
                                        else MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.small,
                            ),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (selected) FocusColors.TomatoRed
                                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            ),
                        ) {
                            Text("$option")
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Short break duration
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("Short break", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text  = "${shortBreak}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FocusColors.TomatoRed,
                    )
                }
                Slider(
                    value         = shortBreak.toFloat(),
                    onValueChange = { shortBreak = it.roundToInt() },
                    valueRange    = 1f..15f,
                    steps         = 13,            // integers 1..15 → 13 intermediate stops
                    modifier      = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                // Long break duration
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("Long break", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text  = "${longBreak}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FocusColors.TomatoRed,
                    )
                }
                Slider(
                    value         = longBreak.toFloat(),
                    onValueChange = { longBreak = it.roundToInt() },
                    valueRange    = 10f..30f,
                    steps         = 19,            // integers 10..30 → 19 intermediate stops
                    modifier      = Modifier.fillMaxWidth(),
                )
            }

            // ── SECTION: Sound & Haptics ──────────────────────────────────────
            SectionHeader("Sound & Haptics")

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Text("Completion sound", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("None", "Bell", "Digital").forEach { option ->
                        FilterChip(
                            selected = soundType == option,
                            onClick  = { soundType = option },
                            label    = { Text(option) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("Vibrate on completion", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked         = vibrateEnabled,
                        onCheckedChange = { vibrateEnabled = it },
                    )
                }
            }

            // ── SECTION: Appearance ───────────────────────────────────────────
            SectionHeader("Appearance")

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("System", "Light", "Dark").forEach { option ->
                        val selected = themeMode == option
                        TextButton(
                            onClick  = {
                                themeMode = option
                                if (option != "Dark") amoledMode = false
                            },
                            modifier = Modifier.border(
                                width = 1.dp,
                                color = if (selected) FocusColors.TomatoRed
                                        else MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.small,
                            ),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (selected) FocusColors.TomatoRed
                                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            ),
                        ) {
                            Text(option)
                        }
                    }
                }

                // AMOLED mode — only meaningful in Dark theme
                AnimatedVisibility(visible = themeMode == "Dark") {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("AMOLED mode", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text  = "True black saves battery on OLED screens",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        Switch(
                            checked         = amoledMode,
                            onCheckedChange = { amoledMode = it },
                        )
                    }
                }
            }

            // ── SECTION: Pro Features ─────────────────────────────────────────
            SectionHeader("Pro Features")

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(
                        width = 1.5.dp,
                        color = FocusColors.TomatoRed,
                        shape = MaterialTheme.shapes.medium,
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text  = "Unlock Pro — ₹149 once",
                        style = MaterialTheme.typography.titleMedium,
                        color = FocusColors.TomatoRed,
                    )
                    Spacer(Modifier.height(12.dp))

                    listOf(
                        "Unlimited session history",
                        "10+ focus sounds",
                        "AMOLED black mode",
                        "CSV export",
                    ).forEach { feature ->
                        Row(
                            modifier          = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("✅", fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(feature, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    FilledTonalButton(
                        onClick  = { /* TODO: wire to BillingClient in V2 */ },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Unlock Pro")
                    }
                }
            }

            // ── SECTION: About ────────────────────────────────────────────────
            SectionHeader("About")

            ListItem(
                headlineContent  = { Text("Version") },
                trailingContent  = {
                    Text(
                        text  = "1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent  = { Text("Rate on Play Store") },
                trailingContent  = {
                    Icon(
                        imageVector        = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                },
                modifier = Modifier.clickable { /* TODO: open Play Store listing */ },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ListItem(
                headlineContent  = { Text("Privacy Policy") },
                trailingContent  = {
                    Icon(
                        imageVector        = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                },
                modifier = Modifier.clickable { /* TODO: open privacy policy URL */ },
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ============================================================================
// SectionHeader
// ============================================================================

/**
 * Reusable section divider used throughout [SettingsScreen].
 *
 * Renders the [title] in ALL CAPS with [FocusColors.TomatoRed], followed by
 * a full-width [HorizontalDivider].  Padding matches Material 3 list-section
 * header conventions (top 24 dp, bottom 8 dp, sides 16 dp).
 */
@Composable
fun SectionHeader(title: String) {
    Column {
        Text(
            text     = title.uppercase(),
            style    = MaterialTheme.typography.labelLarge,
            color    = FocusColors.TomatoRed,
            modifier = Modifier.padding(
                start  = 16.dp,
                end    = 16.dp,
                top    = 24.dp,
                bottom = 8.dp,
            ),
        )
        HorizontalDivider()
    }
}
