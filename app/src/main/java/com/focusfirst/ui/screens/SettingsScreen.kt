package com.focusfirst.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusfirst.ui.theme.FocusColors
import com.focusfirst.ui.theme.LocalFocusDarkTheme
import com.focusfirst.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

// ============================================================================
// SettingsScreen — Stitch-inspired layout
// ============================================================================

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val focusMinutes by settingsViewModel.focusMinutes.collectAsStateWithLifecycle()
    val shortBreak by settingsViewModel.shortBreakMinutes.collectAsStateWithLifecycle()
    val longBreak by settingsViewModel.longBreakMinutes.collectAsStateWithLifecycle()
    val sessionsBefore by settingsViewModel.sessionsBeforeLongBreak.collectAsStateWithLifecycle()
    val dailyGoal by settingsViewModel.dailyGoal.collectAsStateWithLifecycle()
    val autoStart by settingsViewModel.autoStart.collectAsStateWithLifecycle()
    val vibrateEnabled by settingsViewModel.vibrate.collectAsStateWithLifecycle()
    val soundType by settingsViewModel.soundType.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val amoledMode by settingsViewModel.amoledMode.collectAsStateWithLifecycle()

    val scheme = MaterialTheme.colorScheme
    val dark   = LocalFocusDarkTheme.current

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

            ProFeaturesCard()

            Spacer(Modifier.height(20.dp))

            SectionLabel("TIMER", color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SettingsSectionCard(dark = dark) {
                TimerSettingsRow(
                    icon        = Icons.Outlined.Speed,
                    iconTint    = Color(0xFF007AFF),
                    label       = "Auto-Start Next Session",
                    trailing    = {
                        Switch(
                            checked         = autoStart,
                            onCheckedChange = { settingsViewModel.updateAutoStart(it) },
                            colors          = switchColors(scheme),
                        )
                    },
                )
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
                PlaceholderChevronRow(
                    icon     = Icons.Outlined.GraphicEq,
                    iconTint = Color(0xFF5856D6),
                    title    = "Background Noise",
                    subtitle = soundType,
                    scheme   = scheme,
                    onClick  = { /* V2 sound picker */ },
                )
                Spacer(Modifier.height(12.dp))
                TimerSettingsRow(
                    icon        = Icons.Outlined.Vibration,
                    iconTint    = Color(0xFFFF9500),
                    label       = "Haptic Feedback",
                    trailing    = {
                        Switch(
                            checked         = vibrateEnabled,
                            onCheckedChange = { settingsViewModel.updateVibrate(it) },
                            colors          = switchColors(scheme),
                        )
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
                        icon        = Icons.Outlined.Timer,
                        iconTint    = Color(0xFF8E8E93),
                        label       = "AMOLED black",
                        trailing    = {
                            Switch(
                                checked         = amoledMode,
                                onCheckedChange = { settingsViewModel.updateAmoledMode(it) },
                                colors          = switchColors(scheme),
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            SectionLabel("ABOUT", color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SupportReferralCard(
                    modifier = Modifier.weight(1f),
                    dark     = dark,
                    scheme   = scheme,
                    icon     = Icons.Outlined.Help,
                    iconTint = Color(0xFF007AFF),
                    title    = "Support",
                    subtitle = "Help & contact",
                    onClick  = {
                        val mailUri = android.net.Uri.parse("mailto:support@focusfirst.app")
                        context.startActivity(Intent(Intent.ACTION_SENDTO, mailUri))
                    },
                )
                SupportReferralCard(
                    modifier = Modifier.weight(1f),
                    dark     = dark,
                    scheme   = scheme,
                    icon     = Icons.Outlined.Share,
                    iconTint = Color(0xFF34C759),
                    title    = "Referral",
                    subtitle = "Share the app",
                    onClick  = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out FocusFirst — a clean Pomodoro timer with zero ads! " +
                                    "https://play.google.com/store/apps/details?id=com.focusfirst",
                            )
                        }
                        context.startActivity(Intent.createChooser(send, "Share FocusFirst"))
                    },
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text       = "FOCUSFIRST V1.0.0",
                fontSize   = 12.sp,
                letterSpacing = 1.sp,
                color      = scheme.onSurfaceVariant,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth(),
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
private fun ProFeaturesCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FocusColors.ProCardBackground)
            .padding(20.dp),
    ) {
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
            text     = "Custom focus sounds, detailed analytics, and more — coming soon.",
            fontSize = 13.sp,
            color    = Color.White.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { /* billing V2 */ }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = "Upgrade Now",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.Black,
            )
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
    icon:     ImageVector,
    iconTint: Color,
    label:    String,
    trailing: @Composable () -> Unit,
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
            value            = value,
            onValueChange    = onChange,
            valueRange       = range,
            steps            = steps,
            modifier         = Modifier.fillMaxWidth(),
            colors           = SliderDefaults.colors(
                thumbColor           = scheme.primary,
                activeTrackColor     = scheme.primary,
                inactiveTrackColor   = scheme.onSurfaceVariant.copy(alpha = 0.25f),
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
    icon:     ImageVector,
    iconTint: Color,
    title:    String,
    subtitle: String,
    scheme:   androidx.compose.material3.ColorScheme,
    onClick:  () -> Unit,
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
                Text(
                    text     = title,
                    fontSize = 15.sp,
                    color    = scheme.onSurface,
                )
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
    val stroke = if (selected) 2.dp else 1.dp
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
private fun SupportReferralCard(
    modifier: Modifier,
    dark:     Boolean,
    scheme:   androidx.compose.material3.ColorScheme,
    icon:     ImageVector,
    iconTint: Color,
    title:    String,
    subtitle: String,
    onClick:  () -> Unit,
) {
    val bg     = if (dark) Color.White.copy(alpha = 0.07f) else scheme.surface
    val border = if (dark) Color.White.copy(alpha = 0.1f) else scheme.outlineVariant.copy(alpha = 0.35f)

    Column(
        modifier = modifier
            .clip(SectionShape)
            .border(1.dp, border, SectionShape)
            .background(bg, SectionShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        BadgeIcon(icon = icon, tint = iconTint)
        Spacer(Modifier.height(12.dp))
        Text(
            text       = title,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = scheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text     = subtitle,
            fontSize = 11.sp,
            color    = scheme.onSurfaceVariant,
        )
    }
}
