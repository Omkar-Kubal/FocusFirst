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
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
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

// ─────────────────────────────────────────────────────────────────────────────

private val CardShape = RoundedCornerShape(18.dp)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

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

    val focusMinutes   by settingsViewModel.focusMinutes.collectAsStateWithLifecycle()
    val shortBreak     by settingsViewModel.shortBreakMinutes.collectAsStateWithLifecycle()
    val longBreak      by settingsViewModel.longBreakMinutes.collectAsStateWithLifecycle()
    val sessionsBefore by settingsViewModel.sessionsBeforeLongBreak.collectAsStateWithLifecycle()
    val dailyGoal      by settingsViewModel.dailyGoal.collectAsStateWithLifecycle()
    val autoStart      by settingsViewModel.autoStart.collectAsStateWithLifecycle()
    val vibrateEnabled by settingsViewModel.vibrate.collectAsStateWithLifecycle()
    val themeMode      by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val amoledMode     by settingsViewModel.amoledMode.collectAsStateWithLifecycle()
    val dndEnabled     by settingsViewModel.dndEnabled.collectAsStateWithLifecycle()
    val isPro          by billingViewModel.isPro.collectAsStateWithLifecycle()
    val proPrice       by billingViewModel.proPrice.collectAsStateWithLifecycle()
    val ambientSound   by settingsViewModel.ambientSound.collectAsStateWithLifecycle()
    val ambientVolume  by settingsViewModel.ambientVolume.collectAsStateWithLifecycle()

    var showSoundSheet by remember { mutableStateOf(false) }
    var showFocusGuard by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var dndPermissionGranted by remember { mutableStateOf(dndManager.isDndPermissionGranted()) }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                dndPermissionGranted = dndManager.isDndPermissionGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val cs           = MaterialTheme.colorScheme
    val dark         = LocalFocusDarkTheme.current
    val privacyUrl   = stringResource(R.string.privacy_policy_url)
    val termsUrl     = stringResource(R.string.terms_url)
    val supportEmail = stringResource(R.string.support_email)

    if (showFocusGuard) {
        FocusGuardScreen(onBack = { showFocusGuard = false })
        return
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 46.dp, bottom = 4.dp),
        ) {
            Text(
                text       = "Settings",
                fontSize   = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = cs.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text     = "v${BuildConfig.VERSION_NAME}",
                fontSize = 13.sp,
                color    = cs.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Pro card ───────────────────────────────────────────────────
            ProFeaturesCard(
                isPro     = isPro,
                proPrice  = proPrice,
                onUpgrade = { billingViewModel.openUpgradeSheet() },
            )

            // ── Productivity ───────────────────────────────────────────────
            SectionGap()
            SectionLabel("PRODUCTIVITY")
            Spacer(Modifier.height(10.dp))
            SettingsSectionCard {
                SettingsChevronRow(
                    icon     = Icons.Outlined.Lock,
                    iconTint = Color(0xFF6C63FF),
                    title    = "Focus Guard",
                    subtitle = if (isPro) "Block distracting apps during sessions"
                               else       "Block apps during focus sessions",
                    proBadge = !isPro,
                    onClick  = {
                        if (isPro) showFocusGuard = true
                        else billingViewModel.openUpgradeSheet()
                    },
                )
            }

            // ── Sessions ───────────────────────────────────────────────────
            SectionGap()
            SectionLabel("SESSIONS")
            Spacer(Modifier.height(10.dp))
            SettingsSectionCard {
                SettingsSwitchRow(
                    icon     = Icons.Outlined.Speed,
                    iconTint = Color(0xFF007AFF),
                    label    = "Auto-Start Next Session",
                    subtitle = "Begin breaks and focus rounds automatically",
                    checked  = autoStart,
                    onCheckedChange = { settingsViewModel.updateAutoStart(it) },
                )
                RowDivider()
                // DND — trailing adapts to permission state
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.weight(1f),
                    ) {
                        BadgeIcon(icon = Icons.Outlined.NotificationsOff, tint = Color(0xFFFF3B30))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text       = "Auto-enable DND",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color      = cs.onSurface,
                            )
                            Text(
                                text     = if (dndPermissionGranted) "Silence notifications during focus"
                                           else "Permission required — tap Grant",
                                fontSize = 12.sp,
                                color    = cs.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    if (dndPermissionGranted) {
                        Switch(
                            checked         = dndEnabled,
                            onCheckedChange = { v ->
                                settingsViewModel.updateDndEnabled(v)
                                com.focusfirst.analytics.TokiAnalytics.logDndToggled(v)
                            },
                            colors = switchColors(cs),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .border(1.dp, Color(0xFF1A9E5F), RoundedCornerShape(50.dp))
                                .clickable { dndManager.requestDndPermission() }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text       = "Grant",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF1A9E5F),
                            )
                        }
                    }
                }
            }

            // ── Durations ──────────────────────────────────────────────────
            SectionGap()
            SectionLabel("DURATIONS")
            Spacer(Modifier.height(10.dp))
            SettingsSectionCard {
                SliderSettingRow(
                    icon      = Icons.Outlined.Timer,
                    iconTint  = Color(0xFF34C759),
                    label     = "Focus",
                    valueText = "${focusMinutes.coerceIn(10, 60)} min",
                    value     = focusMinutes.coerceIn(10, 60).toFloat(),
                    range     = 10f..60f,
                    steps     = 49,
                    onChange  = { settingsViewModel.updateFocusMinutes(it.roundToInt().coerceIn(10, 60)) },
                )
                RowDivider()
                SliderSettingRow(
                    icon      = Icons.Outlined.Schedule,
                    iconTint  = Color(0xFFFF9500),
                    label     = "Short break",
                    valueText = "${shortBreak.coerceIn(1, 15)} min",
                    value     = shortBreak.coerceIn(1, 15).toFloat(),
                    range     = 1f..15f,
                    steps     = 13,
                    onChange  = { settingsViewModel.updateShortBreakMinutes(it.roundToInt().coerceIn(1, 15)) },
                )
                RowDivider()
                SliderSettingRow(
                    icon      = Icons.Outlined.Timer,
                    iconTint  = Color(0xFFAF52DE),
                    label     = "Long break",
                    valueText = "${longBreak.coerceIn(10, 30)} min",
                    value     = longBreak.coerceIn(10, 30).toFloat(),
                    range     = 10f..30f,
                    steps     = 19,
                    onChange  = { settingsViewModel.updateLongBreakMinutes(it.roundToInt().coerceIn(10, 30)) },
                )
                RowDivider()
                SegmentedIntRow(
                    icon     = Icons.Outlined.GraphicEq,
                    iconTint = Color(0xFFFF2D55),
                    label    = "Sessions before long break",
                    options  = listOf(2, 3, 4, 6),
                    selected = sessionsBefore.let { s -> if (s in listOf(2, 3, 4, 6)) s else 4 },
                    onSelect = { settingsViewModel.updateSessionsBeforeLongBreak(it) },
                )
                RowDivider()
                SegmentedIntRow(
                    icon     = Icons.Outlined.Speed,
                    iconTint = Color(0xFF5AC8FA),
                    label    = "Daily goal",
                    options  = listOf(4, 6, 8, 10, 12),
                    selected = dailyGoal.let { g -> if (g in listOf(4, 6, 8, 10, 12)) g else 8 },
                    onSelect = { settingsViewModel.updateDailyGoal(it) },
                )
            }

            // ── Sound & Haptics ────────────────────────────────────────────
            SectionGap()
            SectionLabel("SOUND & HAPTICS")
            Spacer(Modifier.height(10.dp))
            SettingsSectionCard {
                SettingsChevronRow(
                    icon     = Icons.Outlined.GraphicEq,
                    iconTint = Color(0xFF5856D6),
                    title    = "Ambient Sound",
                    subtitle = ambientSound.displayName,
                    onClick  = { showSoundSheet = true },
                )
                if (ambientSound != AmbientSound.NONE) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🔈", fontSize = 14.sp)
                        Spacer(Modifier.width(10.dp))
                        Slider(
                            value         = ambientVolume,
                            onValueChange = { settingsViewModel.updateAmbientVolume(it) },
                            modifier      = Modifier.weight(1f),
                            colors        = SliderDefaults.colors(
                                thumbColor         = cs.primary,
                                activeTrackColor   = cs.primary,
                                inactiveTrackColor = cs.onSurfaceVariant.copy(alpha = 0.25f),
                            ),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("🔊", fontSize = 14.sp)
                    }
                }
                RowDivider()
                SettingsSwitchRow(
                    icon     = Icons.Outlined.Vibration,
                    iconTint = Color(0xFFFF9500),
                    label    = "Haptic Feedback",
                    checked  = vibrateEnabled,
                    onCheckedChange = { settingsViewModel.updateVibrate(it) },
                )
            }

            if (showSoundSheet) {
                SoundSelectorSheet(
                    currentSound    = ambientSound,
                    currentVolume   = ambientVolume,
                    isPro           = isPro,
                    onSoundSelected = {
                        settingsViewModel.updateAmbientSound(it)
                        com.focusfirst.analytics.TokiAnalytics.logSoundSelected(it.displayName)
                    },
                    onVolumeChanged = { settingsViewModel.updateAmbientVolume(it) },
                    onDismiss       = { showSoundSheet = false },
                    onUpgradeClick  = {
                        showSoundSheet = false
                        billingViewModel.openUpgradeSheet()
                    },
                )
            }

            // ── Appearance ─────────────────────────────────────────────────
            SectionGap()
            SectionLabel("APPEARANCE")
            Spacer(Modifier.height(10.dp))
            SettingsSectionCard {
                ThemeVisualSelector(
                    themeMode = themeMode,
                    onSelect  = { settingsViewModel.updateThemeMode(it) },
                )
                if (themeMode == "Dark" || (themeMode == "System" && dark)) {
                    RowDivider()
                    SettingsSwitchRow(
                        icon     = Icons.Outlined.Timer,
                        iconTint = Color(0xFF8E8E93),
                        label    = "AMOLED black",
                        subtitle = "Pure black — saves battery on OLED screens",
                        proBadge = !isPro,
                        checked  = amoledMode && isPro,
                        onCheckedChange = { v ->
                            if (isPro) settingsViewModel.updateAmoledMode(v)
                            else billingViewModel.openUpgradeSheet()
                        },
                    )
                }
            }

            // ── Cloud Sync ─────────────────────────────────────────────────
            val syncState  = syncViewModel.syncState
            val isSyncPro by syncViewModel.isPro.collectAsStateWithLifecycle()

            if (isSyncPro) {
                SectionGap()
                SectionLabel("CLOUD SYNC")
                Spacer(Modifier.height(10.dp))
                SettingsSectionCard {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = "Backup sessions",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color      = cs.onSurface,
                            )
                            Text(
                                text     = when (syncState) {
                                    is SyncState.Idle    -> "Sync your data to the cloud"
                                    is SyncState.Syncing -> "Syncing…"
                                    is SyncState.Success -> "Synced successfully ✓"
                                    is SyncState.Error   -> "Sync failed — tap retry"
                                },
                                fontSize = 12.sp,
                                color    = when (syncState) {
                                    is SyncState.Success -> Color(0xFF1A9E5F)
                                    is SyncState.Error   -> Color(0xFFE84B1A)
                                    else                 -> cs.onSurfaceVariant
                                },
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        if (syncState is SyncState.Syncing) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color       = cs.primary,
                            )
                        } else {
                            Button(
                                onClick = { syncViewModel.syncNow() },
                                shape   = RoundedCornerShape(50.dp),
                                colors  = ButtonDefaults.buttonColors(
                                    containerColor = cs.primary,
                                    contentColor   = cs.onPrimary,
                                ),
                            ) {
                                Text(
                                    text       = "Sync",
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    RowDivider()
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = "Restore from cloud",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color      = cs.onSurface,
                            )
                            Text(
                                text     = "Data is stored anonymously",
                                fontSize = 12.sp,
                                color    = cs.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { syncViewModel.restoreFromCloud() }) {
                            Text(
                                text     = "Restore",
                                fontSize = 13.sp,
                                color    = cs.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }

            // ── About ──────────────────────────────────────────────────────
            SectionGap()
            SectionLabel("ABOUT")
            Spacer(Modifier.height(10.dp))
            SettingsSectionCard {
                SettingsInfoRow(label = "Version", value = BuildConfig.VERSION_NAME)
                RowDivider()
                SettingsChevronRow(
                    title    = "Rate Toki ⭐",
                    subtitle = "Enjoying the app? Leave a review",
                    onClick  = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${context.packageName}"),
                        )
                        try { context.startActivity(intent) } catch (e: Exception) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(
                                    "https://play.google.com/store/apps/details?id=${context.packageName}",
                                )),
                            )
                        }
                    },
                )
                RowDivider()
                SettingsChevronRow(
                    title    = "Contact Support",
                    subtitle = supportEmail,
                    onClick  = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                            putExtra(Intent.EXTRA_SUBJECT, "Toki Support - v${BuildConfig.VERSION_NAME}")
                        }
                        try { context.startActivity(intent) }
                        catch (e: Exception) { Firebase.crashlytics.recordException(e) }
                    },
                )
                RowDivider()
                SettingsChevronRow(
                    title    = "Privacy Policy",
                    subtitle = "How we handle your data",
                    onClick  = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
                    },
                )
                RowDivider()
                SettingsChevronRow(
                    title    = "Terms of Service",
                    subtitle = "Usage terms and conditions",
                    onClick  = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl)))
                    },
                )
                RowDivider()
                SettingsChevronRow(
                    title    = "Open Source Licenses",
                    subtitle = "Third-party libraries we use",
                    onClick  = onNavigateToLicenses,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text          = "© 2026 Toki. All rights reserved.",
                fontSize      = 11.sp,
                letterSpacing = 0.3.sp,
                color         = cs.onSurface.copy(alpha = 0.5f),
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layout helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Consistent vertical gap between sections: label + card pairs. */
@Composable
private fun SectionGap() = Spacer(Modifier.height(28.dp))

/** Thin rule between rows inside a card. */
@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(vertical = 10.dp),
        thickness = 1.dp,
        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    )
}

/** Uppercase muted label above a section — DESIGN.md metadata label style. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Card shell used for every settings group. */
@Composable
private fun SettingsSectionCard(content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(cs.surfaceContainerLow)
            .border(1.dp, cs.outline, CardShape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        content()
    }
}

/** Coloured square icon badge — widget tint is never modified. */
@Composable
private fun BadgeIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(19.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Row primitives
// ─────────────────────────────────────────────────────────────────────────────

/** Simple key / value info row — no chevron, not clickable. */
@Composable
private fun SettingsInfoRow(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = label,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Medium,
            color      = cs.onSurface,
        )
        Text(
            text     = value,
            fontSize = 14.sp,
            color    = cs.onSurfaceVariant,
        )
    }
}

/** Row with badge icon + label (+ optional subtitle) + Switch. */
@Composable
private fun SettingsSwitchRow(
    icon:            ImageVector,
    iconTint:        Color,
    label:           String,
    checked:         Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle:        String?  = null,
    proBadge:        Boolean  = false,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.weight(1f),
        ) {
            BadgeIcon(icon = icon, tint = iconTint)
            Spacer(Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = label,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color      = cs.onSurface,
                    )
                    if (proBadge) {
                        Spacer(Modifier.width(6.dp))
                        ProBadge()
                    }
                }
                if (subtitle != null) {
                    Text(
                        text     = subtitle,
                        fontSize = 12.sp,
                        color    = cs.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = switchColors(cs),
        )
    }
}

/** Row with optional badge icon + title + subtitle + chevron. */
@Composable
private fun SettingsChevronRow(
    title:    String,
    subtitle: String,
    icon:     ImageVector? = null,
    iconTint: Color        = Color.Unspecified,
    proBadge: Boolean      = false,
    onClick:  () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.weight(1f),
        ) {
            if (icon != null) {
                BadgeIcon(icon = icon, tint = iconTint)
                Spacer(Modifier.width(14.dp))
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = title,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color      = cs.onSurface,
                    )
                    if (proBadge) {
                        Spacer(Modifier.width(6.dp))
                        ProBadge()
                    }
                }
                Text(
                    text     = subtitle,
                    fontSize = 12.sp,
                    color    = cs.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector        = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint               = cs.onSurfaceVariant,
            modifier           = Modifier.size(18.dp),
        )
    }
}

/** Slider row: icon + label + live value badge + track. */
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
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.weight(1f),
            ) {
                BadgeIcon(icon = icon, tint = iconTint)
                Spacer(Modifier.width(14.dp))
                Text(
                    text       = label,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color      = cs.onSurface,
                )
            }
            // Value pill — matches duration-selector pill style
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(cs.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text       = valueText,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = cs.onSurface,
                )
            }
        }
        Slider(
            value         = value,
            onValueChange = onChange,
            valueRange    = range,
            steps         = steps,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp),
            colors        = SliderDefaults.colors(
                thumbColor         = cs.primary,
                activeTrackColor   = cs.primary,
                inactiveTrackColor = cs.onSurfaceVariant.copy(alpha = 0.22f),
            ),
        )
    }
}

/**
 * Segmented integer chips — pill shape, matches DurationSelector.
 * Active: primary fill + onPrimary text.
 * Inactive: surfaceContainerLow + outline border + onSurfaceVariant text.
 */
@Composable
private fun SegmentedIntRow(
    icon:     ImageVector,
    iconTint: Color,
    label:    String,
    options:  List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgeIcon(icon = icon, tint = iconTint)
            Spacer(Modifier.width(14.dp))
            Text(
                text       = label,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = cs.onSurface,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            options.forEach { opt ->
                val isSel = opt == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (isSel) cs.primary else cs.surfaceContainerLow)
                        .border(
                            width = if (isSel) 0.dp else 1.dp,
                            color = cs.outline,
                            shape = RoundedCornerShape(50.dp),
                        )
                        .clickable { onSelect(opt) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = opt.toString(),
                        fontSize   = 15.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                        color      = if (isSel) cs.onPrimary else cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Switch colours helper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun switchColors(cs: androidx.compose.material3.ColorScheme) = SwitchDefaults.colors(
    checkedThumbColor   = cs.primary,
    checkedTrackColor   = cs.primary.copy(alpha = 0.5f),
    uncheckedThumbColor = cs.outline,
    uncheckedTrackColor = cs.surfaceVariant,
)

// ─────────────────────────────────────────────────────────────────────────────
// Pro card — always dark, intentional brand card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProFeaturesCard(
    isPro:    Boolean,
    proPrice: String?,
    onUpgrade: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FocusColors.ProCardBackground)
            .padding(22.dp),
    ) {
        if (isPro) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = "Toki Pro",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White,
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFF34C759).copy(alpha = 0.18f))
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
            Spacer(Modifier.height(8.dp))
            Text(
                text     = "All features unlocked. Thank you for supporting Toki!",
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.68f),
            )
        } else {
            Text(
                text       = "Pro Features",
                fontSize   = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text          = "UNLOCK FULL POTENTIAL",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.8.sp,
                color         = Color.White.copy(alpha = 0.45f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text     = proPrice?.let {
                    "Sounds, analytics, AMOLED mode, export, and more — $it / month."
                } ?: "Sounds, analytics, AMOLED mode, export, and more.",
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White)
                    .clickable(onClick = onUpgrade)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = proPrice?.let { "Upgrade Now — $it / month" } ?: "Upgrade Now",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.Black,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Theme selector
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThemeVisualSelector(themeMode: String, onSelect: (String) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ThemeOptionCard(
            modifier  = Modifier.weight(1f),
            label     = "LIGHT",
            previewBg = Color(0xFFFFFFFF),
            selected  = themeMode == "Light",
            onClick   = { onSelect("Light") },
        )
        ThemeOptionCard(
            modifier  = Modifier.weight(1f),
            label     = "DARK",
            previewBg = Color(0xFF000000),
            selected  = themeMode == "Dark",
            onClick   = { onSelect("Dark") },
        )
        ThemeOptionCard(
            modifier  = Modifier.weight(1f),
            label     = "SYSTEM",
            previewBg = Color(0xFF8E8E93),
            selected  = themeMode == "System",
            onClick   = { onSelect("System") },
        )
    }
}

@Composable
private fun ThemeOptionCard(
    modifier:  Modifier,
    label:     String,
    previewBg: Color,
    selected:  Boolean,
    onClick:   () -> Unit,
) {
    val cs          = MaterialTheme.colorScheme
    val borderWidth = if (selected) 2.dp else 1.dp
    val borderColor = if (selected) cs.primary else cs.outline

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        // Preview swatch with a mini pill to suggest the UI
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(previewBg),
            contentAlignment = Alignment.Center,
        ) {
            // Mini play-button circle — visual hint of the app
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        if (previewBg == Color(0xFFFFFFFF)) Color(0xFF0D0D0D)
                        else Color(0xFFF7F7F7)
                    ),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text          = label,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            color         = if (selected) cs.primary else cs.onSurfaceVariant,
        )
        // Selected dot indicator
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(if (selected) cs.primary else Color.Transparent),
        )
    }
}
