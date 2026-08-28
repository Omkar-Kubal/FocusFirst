package com.focusfirst.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.BuildConfig
import com.focusfirst.R
import com.focusfirst.billing.BillingViewModel
import com.focusfirst.data.model.AmbientSound
import com.focusfirst.ui.components.FocusedVoid
import com.focusfirst.ui.components.SoundSelectorSheet
import com.focusfirst.ui.components.TokiListRow
import com.focusfirst.ui.components.TokiPill
import com.focusfirst.ui.components.TokiSection
import com.focusfirst.util.DndManager
import com.focusfirst.util.DndManagerEntryPoint
import com.focusfirst.viewmodel.SettingsViewModel
import com.focusfirst.viewmodel.SyncState
import com.focusfirst.viewmodel.SyncViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    billingViewModel: BillingViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel(),
    onNavigateToLicenses: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dndManager: DndManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DndManagerEntryPoint::class.java,
        ).dndManager()
    }

    val focusMinutes by settingsViewModel.focusMinutes.collectAsStateWithLifecycle()
    val shortBreak by settingsViewModel.shortBreakMinutes.collectAsStateWithLifecycle()
    val longBreak by settingsViewModel.longBreakMinutes.collectAsStateWithLifecycle()
    val sessionsBefore by settingsViewModel.sessionsBeforeLongBreak.collectAsStateWithLifecycle()
    val dailyGoal by settingsViewModel.dailyGoal.collectAsStateWithLifecycle()
    val vibrateEnabled by settingsViewModel.vibrate.collectAsStateWithLifecycle()
    val amoledMode by settingsViewModel.amoledMode.collectAsStateWithLifecycle()
    val dndEnabled by settingsViewModel.dndEnabled.collectAsStateWithLifecycle()
    val ambientSound by settingsViewModel.ambientSound.collectAsStateWithLifecycle()
    val ambientVolume by settingsViewModel.ambientVolume.collectAsStateWithLifecycle()
    val isPro by billingViewModel.isPro.collectAsStateWithLifecycle()
    val proPrice by billingViewModel.proPrice.collectAsStateWithLifecycle()

    var showSoundSheet by remember { mutableStateOf(false) }
    var showFocusGuard by remember { mutableStateOf(false) }
    var dndPermissionGranted by remember { mutableStateOf(dndManager.isDndPermissionGranted()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dndPermissionGranted = dndManager.isDndPermissionGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showFocusGuard) {
        FocusGuardScreen(onBack = { showFocusGuard = false })
        return
    }

    if (showSoundSheet) {
        SoundSelectorSheet(
            currentSound = ambientSound,
            currentVolume = ambientVolume,
            isPro = isPro,
            onSoundSelected = {
                settingsViewModel.updateAmbientSound(it)
                com.focusfirst.analytics.TokiAnalytics.logSoundSelected(it.displayName)
            },
            onVolumeChanged = { settingsViewModel.updateAmbientVolume(it) },
            onDismiss = { showSoundSheet = false },
            onUpgradeClick = {
                showSoundSheet = false
                billingViewModel.openUpgradeSheet()
            },
        )
    }

    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val termsUrl = stringResource(R.string.terms_url)
    val supportEmail = stringResource(R.string.support_email)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 42.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SettingsHeader()
        ProPanel(
            isPro = isPro,
            proPrice = proPrice,
            onUpgrade = { billingViewModel.openUpgradeSheet() },
        )

        SettingsGroup(title = "Timer") {
            DurationSlider(
                icon = Icons.Outlined.Timer,
                label = "Focus",
                valueText = "${focusMinutes.coerceIn(10, 60)} min",
                value = focusMinutes.coerceIn(10, 60).toFloat(),
                range = 10f..60f,
                steps = 49,
                onChange = { settingsViewModel.updateFocusMinutes(it.roundToInt().coerceIn(10, 60)) },
            )
            GroupDivider()
            DurationSlider(
                icon = Icons.Outlined.Schedule,
                label = "Short break",
                valueText = "${shortBreak.coerceIn(1, 15)} min",
                value = shortBreak.coerceIn(1, 15).toFloat(),
                range = 1f..15f,
                steps = 13,
                onChange = { settingsViewModel.updateShortBreakMinutes(it.roundToInt().coerceIn(1, 15)) },
            )
            GroupDivider()
            DurationSlider(
                icon = Icons.Outlined.Schedule,
                label = "Long break",
                valueText = "${longBreak.coerceIn(10, 30)} min",
                value = longBreak.coerceIn(10, 30).toFloat(),
                range = 10f..30f,
                steps = 19,
                onChange = { settingsViewModel.updateLongBreakMinutes(it.roundToInt().coerceIn(10, 30)) },
            )
            GroupDivider()
            SegmentedSetting(
                label = "Sessions before long break",
                options = listOf(2, 3, 4, 6),
                selected = sessionsBefore.takeIf { it in listOf(2, 3, 4, 6) } ?: 4,
                onSelect = { settingsViewModel.updateSessionsBeforeLongBreak(it) },
            )
            GroupDivider()
            SegmentedSetting(
                label = "Daily goal",
                options = listOf(4, 6, 8, 10, 12),
                selected = dailyGoal.takeIf { it in listOf(4, 6, 8, 10, 12) } ?: 8,
                onSelect = { settingsViewModel.updateDailyGoal(it) },
            )
            GroupDivider()
            TokiListRow(
                title = "Automatic phase progression",
                subtitle = "Toki moves from focus to breaks automatically",
            )
        }

        SettingsGroup(title = "Focus aids") {
            ActionRow(
                icon = Icons.Outlined.Lock,
                title = "Focus Guard",
                subtitle = if (isPro) "Block distracting apps during focus" else "Pro feature",
                onClick = {
                    if (isPro) showFocusGuard = true else billingViewModel.openUpgradeSheet()
                },
            )
            GroupDivider()
            if (dndPermissionGranted) {
                SwitchRow(
                    icon = Icons.Outlined.NotificationsOff,
                    title = "Auto-enable DND",
                    subtitle = "Silence notifications during focus",
                    checked = dndEnabled,
                    onCheckedChange = {
                        settingsViewModel.updateDndEnabled(it)
                        com.focusfirst.analytics.TokiAnalytics.logDndToggled(it)
                    },
                )
            } else {
                ActionRow(
                    icon = Icons.Outlined.NotificationsOff,
                    title = "Do Not Disturb",
                    subtitle = "Permission required",
                    trailingText = "Grant",
                    onClick = { dndManager.requestDndPermission() },
                )
            }
        }

        SettingsGroup(title = "Sound & haptics") {
            ActionRow(
                icon = Icons.Outlined.GraphicEq,
                title = "Ambient sound",
                subtitle = ambientSound.displayName,
                onClick = { showSoundSheet = true },
            )
            if (ambientSound != AmbientSound.NONE) {
                Slider(
                    value = ambientVolume,
                    onValueChange = { settingsViewModel.updateAmbientVolume(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
            GroupDivider()
            SwitchRow(
                icon = Icons.Outlined.Vibration,
                title = "Haptic feedback",
                subtitle = "Vibrate for important timer events",
                checked = vibrateEnabled,
                onCheckedChange = { settingsViewModel.updateVibrate(it) },
            )
        }

        SettingsGroup(title = "Appearance") {
            SwitchRow(
                icon = Icons.Outlined.Settings,
                title = "AMOLED black",
                subtitle = if (isPro) "Use pure black surfaces" else "Pro feature",
                checked = amoledMode && isPro,
                onCheckedChange = {
                    if (isPro) settingsViewModel.updateAmoledMode(it)
                    else billingViewModel.openUpgradeSheet()
                },
            )
            GroupDivider()
            TokiListRow(
                title = "Light mode",
                subtitle = "Design tokens are ready; UI toggle ships in a later polish pass",
            )
        }

        SettingsGroup(title = "Data & sync") {
            SyncPanel(
                isPro = isPro,
                state = syncViewModel.syncState,
                onSync = {
                    if (isPro) syncViewModel.syncNow()
                    else billingViewModel.openUpgradeSheet()
                },
                onRestore = {
                    if (isPro) syncViewModel.restoreFromCloud()
                    else billingViewModel.openUpgradeSheet()
                },
            )
            GroupDivider()
            ActionRow(
                icon = Icons.Outlined.FileDownload,
                title = "Export CSV",
                subtitle = if (isPro) "Export all focus sessions" else "Pro feature",
                onClick = {
                    if (isPro) settingsViewModel.exportData()
                    else billingViewModel.openUpgradeSheet()
                },
            )
        }

        SettingsGroup(title = "About") {
            TokiListRow("Version", "v${BuildConfig.VERSION_NAME}")
            GroupDivider()
            ActionRow(
                title = "Contact support",
                subtitle = supportEmail,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                        putExtra(Intent.EXTRA_SUBJECT, "Toki Support - v${BuildConfig.VERSION_NAME}")
                    }
                    runCatching { context.startActivity(intent) }
                },
            )
            GroupDivider()
            ActionRow(
                title = "Privacy Policy",
                subtitle = "How Toki handles data",
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))) },
            )
            GroupDivider()
            ActionRow(
                title = "Terms of Service",
                subtitle = "Usage terms and conditions",
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl))) },
            )
            GroupDivider()
            ActionRow(
                title = "Open Source Licenses",
                subtitle = "Third-party libraries",
                onClick = onNavigateToLicenses,
            )
        }

        Text(
            text = "© 2026 Toki. All rights reserved.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SettingsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Settings",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = "Configure once. Focus faster next time.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ProPanel(isPro: Boolean, proPrice: String?, onUpgrade: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Toki Pro",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = if (isPro) {
                "All focus tools are unlocked. Thank you for supporting Toki."
            } else {
                proPrice?.let { "Unlock sounds, Focus Guard, sync, export, AMOLED mode - $it / month." }
                    ?: "Unlock sounds, Focus Guard, sync, export, and AMOLED mode."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!isPro) {
            Button(
                onClick = onUpgrade,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(proPrice?.let { "Upgrade - $it / month" } ?: "Upgrade")
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        TokiSection(
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
private fun DurationSlider(
    icon: ImageVector,
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallIcon(icon)
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            ValuePill(valueText)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@Composable
private fun SegmentedSetting(
    label: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                TokiPill(
                    text = option.toString(),
                    selected = selected == option,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallIcon(icon)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    trailingText: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            SmallIcon(icon)
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingText != null) {
            Text(trailingText, color = FocusedVoid.Success, style = MaterialTheme.typography.labelLarge)
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun SyncPanel(
    isPro: Boolean,
    state: SyncState,
    onSync: () -> Unit,
    onRestore: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallIcon(Icons.Outlined.CloudSync)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Cloud sync", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = syncStateText(isPro, state),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state == SyncState.Syncing) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onSync, modifier = Modifier.weight(1f)) { Text("Sync now") }
            TextButton(onClick = onRestore, modifier = Modifier.weight(1f)) { Text("Restore") }
        }
    }
}

private fun syncStateText(isPro: Boolean, state: SyncState): String = when {
    !isPro -> "Pro feature"
    state == SyncState.Idle -> "Data is stored anonymously"
    state == SyncState.Syncing -> "Syncing..."
    state == SyncState.Success -> "Sync complete"
    state is SyncState.Error -> state.message
    else -> "Ready"
}

@Composable
private fun SmallIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun ValuePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 2.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    )
}
