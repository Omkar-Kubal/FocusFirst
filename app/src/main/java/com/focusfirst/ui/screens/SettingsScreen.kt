package com.focusfirst.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.BuildConfig
import com.focusfirst.R
import com.focusfirst.billing.BillingViewModel
import com.focusfirst.billing.ProBadge
import com.focusfirst.data.model.AmbientSound
import com.focusfirst.ui.components.SoundSelectorSheet
import com.focusfirst.ui.theme.FocusColors
import com.focusfirst.ui.theme.LocalFocusDarkTheme
import com.focusfirst.util.DndManager
import com.focusfirst.util.DndManagerEntryPoint
import com.focusfirst.viewmodel.SettingsViewModel
import com.focusfirst.viewmodel.SyncState
import com.focusfirst.viewmodel.SyncViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlin.math.roundToInt

// ============================================================================
// SettingsScreen — Stitch-inspired layout
// ============================================================================

@Composable
fun SettingsScreen(
    settingsViewModel:    SettingsViewModel = hiltViewModel(),
    billingViewModel:     BillingViewModel  = hiltViewModel(),
    syncViewModel:        SyncViewModel     = hiltViewModel(),
    onNavigateToLicenses: () -> Unit        = {},
) {
    val context = LocalContext.current

    val dndManager: DndManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DndManagerEntryPoint::class.java,
        ).dndManager()
    }

    val focusMinutes    by settingsViewModel.focusMinutes.collectAsStateWithLifecycle()
    val shortBreak      by settingsViewModel.shortBreakMinutes.collectAsStateWithLifecycle()
    val longBreak       by settingsViewModel.longBreakMinutes.collectAsStateWithLifecycle()
    val sessionsBefore  by settingsViewModel.sessionsBeforeLongBreak.collectAsStateWithLifecycle()
    val dailyGoal       by settingsViewModel.dailyGoal.collectAsStateWithLifecycle()
    val autoStart       by settingsViewModel.autoStart.collectAsStateWithLifecycle()
    val vibrateEnabled  by settingsViewModel.vibrate.collectAsStateWithLifecycle()
    val themeMode       by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val amoledMode      by settingsViewModel.amoledMode.collectAsStateWithLifecycle()
    val dndEnabled      by settingsViewModel.dndEnabled.collectAsStateWithLifecycle()
    val isPro           by billingViewModel.isPro.collectAsStateWithLifecycle()
    val ambientSound    by settingsViewModel.ambientSound.collectAsStateWithLifecycle()
    val ambientVolume   by settingsViewModel.ambientVolume.collectAsStateWithLifecycle()

    var showSoundSheet  by remember { mutableStateOf(false) }
    var showFocusGuard  by remember { mutableStateOf(false) }

    // Checked once per composition — user must navigate away/back to refresh after granting
    val dndPermissionGranted by remember { mutableStateOf(dndManager.isDndPermissionGranted()) }

    val scheme = MaterialTheme.colorScheme
    val dark   = LocalFocusDarkTheme.current

    // Focus Guard screen navigation — shown as an overlay
    if (showFocusGuard) {
        FocusGuardScreen(onBack = { showFocusGuard = false })
        return
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        SettingsTopBar()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            ProFeaturesCard(
                isPro     = isPro,
                onUpgrade = { billingViewModel.openUpgradeSheet() },
            )

            Spacer(Modifier.height(12.dp))

            // ── Focus Guard entry ─────────────────────────────────────────
            SettingsSectionCard(dark = dark) {
                PlaceholderChevronRow(
                    icon     = Icons.Outlined.Lock,
                    iconTint = Color(0xFF6C63FF),
                    title    = "Focus Guard",
                    subtitle = if (isPro) "Block distracting apps during sessions"
                               else "Pro — Block apps during focus sessions",
                    proBadge = !isPro,
                    scheme   = scheme,
                    onClick  = {
                        if (isPro) showFocusGuard = true
                        else billingViewModel.openUpgradeSheet()
                    },
                )
            }

            Spacer(Modifier.height(20.dp))


            SectionLabel("TIMER", color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SettingsSectionCard(dark = dark) {
                TimerSettingsRow(
                    icon     = Icons.Outlined.Speed,
                    iconTint = Color(0xFF007AFF),
                    label    = "Auto-Start Next Session",
                    trailing = {
                        Switch(
                            checked         = autoStart,
                            onCheckedChange = { settingsViewModel.updateAutoStart(it) },
                            colors          = switchColors(scheme),
                        )
                    },
                )
                Spacer(Modifier.height(16.dp))
                // ── DND toggle ────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.weight(1f),
                    ) {
                        BadgeIcon(
                            icon = Icons.Outlined.NotificationsOff,
                            tint = Color(0xFFFF3B30),
                        )
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(
                                text     = "Auto-enable DND",
                                fontSize = 15.sp,
                                color    = scheme.onSurface,
                            )
                            Text(
                                text     = if (dndPermissionGranted)
                                    "Silence notifications during focus"
                                else
                                    "Tap to grant permission",
                                fontSize = 12.sp,
                                color    = scheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (dndPermissionGranted) {
                        Switch(
                            checked         = dndEnabled,
                            onCheckedChange = { settingsViewModel.updateDndEnabled(it) },
                            colors          = switchColors(scheme),
                        )
                    } else {
                        TextButton(onClick = { dndManager.requestDndPermission() }) {
                            Text(
                                text     = "Grant",
                                color    = Color(0xFF1A9E5F),
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
                // ─────────────────────────────────────────────────────────
                Spacer(Modifier.height(16.dp))
                SliderSettingRow(
                    icon      = Icons.Outlined.Timer,
                    iconTint  = Color(0xFF34C759),
                    label     = "Focus duration",
                    valueText = "${focusMinutes.coerceIn(10, 60)}m",
                    value     = focusMinutes.coerceIn(10, 60).toFloat(),
                    range     = 10f..60f,
                    steps     = 49,
                    onChange  = { settingsViewModel.updateFocusMinutes(it.roundToInt().coerceIn(10, 60)) },
                    scheme    = scheme,
                )
                Spacer(Modifier.height(16.dp))
                SliderSettingRow(
                    icon      = Icons.Outlined.Schedule,
                    iconTint  = Color(0xFFFF9500),
                    label     = "Short break",
                    valueText = "${shortBreak.coerceIn(1, 15)}m",
                    value     = shortBreak.coerceIn(1, 15).toFloat(),
                    range     = 1f..15f,
                    steps     = 13,
                    onChange  = { settingsViewModel.updateShortBreakMinutes(it.roundToInt().coerceIn(1, 15)) },
                    scheme    = scheme,
                )
                Spacer(Modifier.height(16.dp))
                SliderSettingRow(
                    icon      = Icons.Outlined.Timer,
                    iconTint  = Color(0xFFAF52DE),
                    label     = "Long break",
                    valueText = "${longBreak.coerceIn(10, 30)}m",
                    value     = longBreak.coerceIn(10, 30).toFloat(),
                    range     = 10f..30f,
                    steps     = 19,
                    onChange  = { settingsViewModel.updateLongBreakMinutes(it.roundToInt().coerceIn(10, 30)) },
                    scheme    = scheme,
                )
                Spacer(Modifier.height(16.dp))
                SegmentedIntRow(
                    icon     = Icons.Outlined.GraphicEq,
                    iconTint = Color(0xFFFF2D55),
                    label    = "Sessions before long break",
                    options  = listOf(2, 3, 4, 6),
                    selected = sessionsBefore.let { s ->
                        if (s in listOf(2, 3, 4, 6)) s else 4
                    },
                    onSelect = { settingsViewModel.updateSessionsBeforeLongBreak(it) },
                    scheme   = scheme,
                )
                Spacer(Modifier.height(16.dp))
                SegmentedIntRow(
                    icon     = Icons.Outlined.Speed,
                    iconTint = Color(0xFF5AC8FA),
                    label    = "Daily goal",
                    options  = listOf(4, 6, 8, 10, 12),
                    selected = dailyGoal.let { g ->
                        if (g in listOf(4, 6, 8, 10, 12)) g else 8
                    },
                    onSelect = { settingsViewModel.updateDailyGoal(it) },
                    scheme   = scheme,
                )
            }

            Spacer(Modifier.height(20.dp))

            SectionLabel("SOUND & HAPTICS", color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SettingsSectionCard(dark = dark) {
                SettingsClickRow(
                    icon     = Icons.Outlined.GraphicEq,
                    iconTint = Color(0xFF5856D6),
                    title    = "Ambient Sound",
                    subtitle = ambientSound.displayName,
                    scheme   = scheme,
                    onClick  = { showSoundSheet = true },
                )
                if (ambientSound != AmbientSound.NONE) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🔈", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value         = ambientVolume,
                            onValueChange = { settingsViewModel.updateAmbientVolume(it) },
                            modifier      = Modifier.weight(1f),
                            colors        = SliderDefaults.colors(
                                thumbColor         = scheme.primary,
                                activeTrackColor   = scheme.primary,
                                inactiveTrackColor = scheme.onSurfaceVariant.copy(alpha = 0.25f),
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("🔊", fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                TimerSettingsRow(
                    icon     = Icons.Outlined.Vibration,
                    iconTint = Color(0xFFFF9500),
                    label    = "Haptic Feedback",
                    trailing = {
                        Switch(
                            checked         = vibrateEnabled,
                            onCheckedChange = { settingsViewModel.updateVibrate(it) },
                            colors          = switchColors(scheme),
                        )
                    },
                )
            }

            if (showSoundSheet) {
                SoundSelectorSheet(
                    currentSound    = ambientSound,
                    currentVolume   = ambientVolume,
                    isPro           = isPro,
                    onSoundSelected = { settingsViewModel.updateAmbientSound(it) },
                    onVolumeChanged = { settingsViewModel.updateAmbientVolume(it) },
                    onDismiss       = { showSoundSheet = false },
                    onUpgradeClick  = {
                        showSoundSheet = false
                        billingViewModel.openUpgradeSheet()
                    },
                )
            }

            Spacer(Modifier.height(20.dp))

            SectionLabel("APPEARANCE", color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SettingsSectionCard(dark = dark) {
                ThemeVisualSelector(
                    themeMode = themeMode,
                    onSelect  = { settingsViewModel.updateThemeMode(it) },
                    scheme    = scheme,
                )
                if (themeMode == "Dark" || (themeMode == "System" && dark)) {
                    Spacer(Modifier.height(16.dp))
                    TimerSettingsRow(
                        icon     = Icons.Outlined.Timer,
                        iconTint = Color(0xFF8E8E93),
                        label    = "AMOLED black",
                        proBadge = !isPro,
                        trailing = {
                            Switch(
                                checked         = amoledMode && isPro,
                                onCheckedChange = { newValue ->
                                    if (isPro) settingsViewModel.updateAmoledMode(newValue)
                                    else billingViewModel.openUpgradeSheet()
                                },
                                colors          = switchColors(scheme),
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            val syncState = syncViewModel.syncState
            val isSyncPro by syncViewModel.isPro.collectAsStateWithLifecycle()

            if (isSyncPro) {
                SectionLabel("CLOUD SYNC", color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color    = if (dark) Color.White.copy(alpha = 0.07f) else scheme.surfaceVariant.copy(alpha = 0.2f),
                    shape    = SectionShape,
                    border   = if (dark) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                               else androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Backup sessions",
                                    fontSize = 15.sp,
                                    color    = scheme.onSurface
                                )
                                Text(
                                    when (syncState) {
                                        is SyncState.Idle    -> "Sync to cloud"
                                        is SyncState.Syncing -> "Syncing..."
                                        is SyncState.Success -> "Synced successfully ✓"
                                        is SyncState.Error   -> "Sync failed — tap retry"
                                    },
                                    fontSize = 13.sp,
                                    color    = when (syncState) {
                                        is SyncState.Success -> Color(0xFF1A9E5F)
                                        is SyncState.Error   -> Color(0xFFE84B1A)
                                        else                 -> scheme.onSurfaceVariant
                                    }
                                )
                            }

                            if (syncState is SyncState.Syncing) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(24.dp).padding(end = 8.dp),
                                    strokeWidth = 2.dp,
                                    color       = scheme.primary
                                )
                            } else {
                                Button(
                                    onClick = { syncViewModel.syncNow() },
                                    colors  = ButtonDefaults.buttonColors(
                                        containerColor = scheme.primary,
                                        contentColor   = scheme.onPrimary
                                    )
                                ) {
                                    Text("Sync", fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color    = scheme.onSurface.copy(alpha = 0.06f)
                        )

                        TextButton(onClick = { syncViewModel.restoreFromCloud() }) {
                            Text(
                                "Restore from cloud",
                                color    = scheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Your data is stored anonymously. No account required.",
                            fontSize = 11.sp,
                            color    = scheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            SectionLabel("ABOUT", color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SettingsSectionCard(dark = dark) {
                // 1. Version
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("Version", fontSize = 15.sp, color = scheme.onSurface)
                    Text(BuildConfig.VERSION_NAME, fontSize = 14.sp, color = scheme.onSurfaceVariant)
                }
                HorizontalDivider(color = scheme.onSurface.copy(alpha = 0.06f))
                // 2. Rate Toki
                SettingsClickRow(
                    title    = "Rate Toki ⭐",
                    subtitle = "Enjoying the app? Leave a review",
                    scheme   = scheme,
                    onClick  = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${context.packageName}"),
                        )
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        "https://play.google.com/store/apps/details?id=${context.packageName}",
                                    ),
                                ),
                            )
                        }
                    },
                )
                HorizontalDivider(color = scheme.onSurface.copy(alpha = 0.06f))
                // 3. Privacy Policy
                SettingsClickRow(
                    title    = "Privacy Policy",
                    subtitle = "How we handle your data",
                    scheme   = scheme,
                    onClick  = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://tokifocus.blogspot.com/privacy-policy"),
                            ),
                        )
                    },
                )
                HorizontalDivider(color = scheme.onSurface.copy(alpha = 0.06f))
                // 4. Terms of Service
                SettingsClickRow(
                    title    = "Terms of Service",
                    subtitle = "Usage terms and conditions",
                    scheme   = scheme,
                    onClick  = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://tokifocus.blogspot.com/terms"),
                            ),
                        )
                    },
                )
                HorizontalDivider(color = scheme.onSurface.copy(alpha = 0.06f))
                // 5. Open Source Licenses
                SettingsClickRow(
                    title    = "Open Source Licenses",
                    subtitle = "Third-party libraries we use",
                    scheme   = scheme,
                    onClick  = onNavigateToLicenses,
                )
            }

            Spacer(Modifier.height(12.dp))

            // 6. Copyright
            Text(
                text      = "© 2026 Toki. All rights reserved.",
                fontSize  = 12.sp,
                color     = scheme.onSurface.copy(alpha = 0.35f),
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun switchColors(scheme: androidx.compose.material3.ColorScheme) = SwitchDefaults.colors(
    checkedThumbColor   = scheme.primary,
    checkedTrackColor   = scheme.primary.copy(alpha = 0.5f),
    uncheckedThumbColor = scheme.outline,
    uncheckedTrackColor = scheme.surfaceVariant,
)

@Composable
private fun SettingsTopBar() {
    val scheme = MaterialTheme.colorScheme
    val dark   = LocalFocusDarkTheme.current
    val barBg  = if (dark) Color.White.copy(alpha = 0.08f) else scheme.surfaceVariant.copy(alpha = 0.5f)

    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .background(barBg)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = "Settings",
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color      = scheme.onSurface,
        )
    }
}

@Composable
private fun ProFeaturesCard(
    isPro: Boolean,
    onUpgrade: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FocusColors.ProCardBackground)
            .padding(20.dp),
    ) {
        if (isPro) {
            // Active Pro state
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = "Toki Pro",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF34C759).copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text          = "ACTIVE",
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color         = Color(0xFF34C759),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text     = "All features unlocked. Thank you for supporting Toki!",
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.7f),
            )
        } else {
            // Upgrade prompt
            Text(
                text       = "Pro Features",
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text          = "UNLOCK FULL POTENTIAL",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color         = Color.White.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text     = "Sounds, analytics, AMOLED mode, export, and more — one-time ₹149.",
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable(onClick = onUpgrade)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "Upgrade Now — ₹149",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.Black,
                )
            }
        }
    }
}

private val SectionShape = RoundedCornerShape(16.dp)

@Composable
private fun SettingsSectionCard(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val bg     = if (dark) Color.White.copy(alpha = 0.07f) else scheme.surface
    val border = if (dark) Color.White.copy(alpha = 0.1f) else scheme.outlineVariant.copy(alpha = 0.35f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, border, SectionShape)
            .background(bg, SectionShape)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text          = text,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        color         = color,
    )
}

@Composable
private fun TimerSettingsRow(
    icon:      ImageVector,
    iconTint:  Color,
    label:     String,
    proBadge:  Boolean = false,
    trailing:  @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgeIcon(icon = icon, tint = iconTint)
            Spacer(Modifier.size(12.dp))
            Text(
                text     = label,
                fontSize = 15.sp,
                color    = scheme.onSurface,
            )
            if (proBadge) {
                Spacer(Modifier.width(6.dp))
                ProBadge()
            }
        }
        trailing()
    }
}

@Composable
private fun BadgeIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SliderSettingRow(
    icon:      ImageVector,
    iconTint:  Color,
    label:     String,
    valueText: String,
    value:     Float,
    range:     ClosedFloatingPointRange<Float>,
    steps:     Int,
    onChange:  (Float) -> Unit,
    scheme:    androidx.compose.material3.ColorScheme,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgeIcon(icon = icon, tint = iconTint)
                Spacer(Modifier.size(12.dp))
                Text(
                    text     = label,
                    fontSize = 15.sp,
                    color    = scheme.onSurface,
                )
            }
            Text(
                text     = valueText,
                fontSize = 14.sp,
                color    = scheme.onSurfaceVariant,
            )
        }
        Slider(
            value         = value,
            onValueChange = onChange,
            valueRange    = range,
            steps         = steps,
            modifier      = Modifier.fillMaxWidth(),
            colors        = SliderDefaults.colors(
                thumbColor         = scheme.primary,
                activeTrackColor   = scheme.primary,
                inactiveTrackColor = scheme.onSurfaceVariant.copy(alpha = 0.25f),
            ),
        )
    }
}

@Composable
private fun SegmentedIntRow(
    icon:     ImageVector,
    iconTint: Color,
    label:    String,
    options:  List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    scheme:   androidx.compose.material3.ColorScheme,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgeIcon(icon = icon, tint = iconTint)
            Spacer(Modifier.size(12.dp))
            Text(
                text     = label,
                fontSize = 15.sp,
                color    = scheme.onSurface,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { opt ->
                val isSel = opt == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSel) scheme.primary
                            else scheme.surfaceVariant.copy(alpha = 0.5f),
                        )
                        .clickable { onSelect(opt) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = opt.toString(),
                        fontSize   = 14.sp,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (isSel) scheme.onPrimary else scheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderChevronRow(
    icon:      ImageVector,
    iconTint:  Color,
    title:     String,
    subtitle:  String,
    proBadge:  Boolean = false,
    scheme:    androidx.compose.material3.ColorScheme,
    onClick:   () -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            BadgeIcon(icon = icon, tint = iconTint)
            Spacer(Modifier.size(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text     = title,
                        fontSize = 15.sp,
                        color    = scheme.onSurface,
                    )
                    if (proBadge) {
                        Spacer(Modifier.width(6.dp))
                        ProBadge()
                    }
                }
                Text(
                    text     = subtitle,
                    fontSize = 12.sp,
                    color    = scheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint               = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemeVisualSelector(
    themeMode: String,
    onSelect:  (String) -> Unit,
    scheme:    androidx.compose.material3.ColorScheme,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ThemeCard(
            modifier  = Modifier.weight(1f),
            label     = "LIGHT",
            previewBg = Color.White,
            border    = Color(0xFF000000),
            selected  = themeMode == "Light",
            onClick   = { onSelect("Light") },
            scheme    = scheme,
        )
        ThemeCard(
            modifier  = Modifier.weight(1f),
            label     = "DARK",
            previewBg = Color(0xFF000000),
            border    = Color.White,
            selected  = themeMode == "Dark",
            onClick   = { onSelect("Dark") },
            scheme    = scheme,
        )
        ThemeCard(
            modifier  = Modifier.weight(1f),
            label     = "SYSTEM",
            previewBg = Color(0xFF8E8E93),
            border    = scheme.primary,
            selected  = themeMode == "System",
            onClick   = { onSelect("System") },
            scheme    = scheme,
        )
    }
}

@Composable
private fun ThemeCard(
    modifier:  Modifier,
    label:     String,
    previewBg: Color,
    border:    Color,
    selected:  Boolean,
    onClick:   () -> Unit,
    scheme:    androidx.compose.material3.ColorScheme,
) {
    val stroke      = if (selected) 2.dp else 1.dp
    val strokeColor = if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.4f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(stroke, strokeColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(previewBg)
                .then(
                    if (selected) Modifier.border(2.dp, border, RoundedCornerShape(10.dp))
                    else Modifier,
                ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text       = label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color      = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsClickRow(
    title:    String,
    subtitle: String,
    scheme:   androidx.compose.material3.ColorScheme,
    icon:     ImageVector? = null,
    iconTint: Color        = Color.Unspecified,
    onClick:  () -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.weight(1f),
        ) {
            if (icon != null) {
                BadgeIcon(icon = icon, tint = iconTint)
                Spacer(Modifier.size(12.dp))
            }
            Column {
                Text(text = title,    fontSize = 15.sp, color = scheme.onSurface)
                Text(text = subtitle, fontSize = 12.sp, color = scheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint               = scheme.onSurfaceVariant,
        )
    }
}
