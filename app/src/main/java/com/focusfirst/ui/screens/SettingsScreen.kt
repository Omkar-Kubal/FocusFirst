package com.focusfirst.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfirst.ui.theme.FocusColors
import kotlin.math.roundToInt

// ============================================================================
// SettingsScreen — "The Focused Void" redesign
// ============================================================================

@Composable
fun SettingsScreen() {

    // ── Local V1 state (replace with DataStore in V2) ─────────────────────────
    var autoStart       by remember { mutableStateOf(false) }
    var sessionLength   by remember { mutableFloatStateOf(25f) }
    var hapticFeedback  by remember { mutableStateOf(true) }
    var volume          by remember { mutableFloatStateOf(0.7f) }
    var themeMode       by remember { mutableStateOf("System") }
    var monochrome      by remember { mutableStateOf(false) }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(FocusColors.Background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // ── 1. Glass top bar ──────────────────────────────────────────────────
        SettingsTopBar()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // ── 2. Pro Features card (solid dark, most prominent) ─────────────
            ProFeaturesCard()

            Spacer(Modifier.height(20.dp))

            // ── 3. Timer section ──────────────────────────────────────────────
            SectionLabel("TIMER")
            Spacer(Modifier.height(8.dp))
            GlassCard {
                SettingsToggleRow(
                    label   = "Auto-Start Next Session",
                    checked = autoStart,
                    onChanged = { autoStart = it },
                )
                Spacer(Modifier.height(16.dp))
                SettingsSliderRow(
                    label     = "Session Length",
                    value     = sessionLength,
                    valueLabel = "${sessionLength.roundToInt()}m",
                    range     = 5f..60f,
                    steps     = 10,
                    onChanged  = { sessionLength = it },
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── 4. Sound & Haptics section ────────────────────────────────────
            SectionLabel("SOUND & HAPTICS")
            Spacer(Modifier.height(8.dp))
            GlassCard {
                SettingsToggleRow(
                    label     = "Haptic Feedback",
                    checked   = hapticFeedback,
                    onChanged = { hapticFeedback = it },
                )
                Spacer(Modifier.height(16.dp))
                SettingsSliderRow(
                    label      = "Volume",
                    value      = volume,
                    valueLabel = "${(volume * 100).roundToInt()}%",
                    range      = 0f..1f,
                    steps      = 9,
                    onChanged  = { volume = it },
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── 5. Appearance section ─────────────────────────────────────────
            SectionLabel("APPEARANCE")
            Spacer(Modifier.height(8.dp))
            GlassCard {
                Text(
                    text     = "Theme",
                    fontSize = 14.sp,
                    color    = FocusColors.OnSurface,
                )
                Spacer(Modifier.height(10.dp))
                ThemePillRow(
                    selectedTheme = themeMode,
                    onSelected    = { themeMode = it },
                )
                Spacer(Modifier.height(16.dp))
                SettingsToggleRow(
                    label     = "Monochrome Theme",
                    checked   = monochrome,
                    onChanged = { monochrome = it },
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── 6. About bento (2 square-ish cards) ───────────────────────────
            SectionLabel("ABOUT")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AboutBentoCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Outlined.Help,
                    title    = "Support",
                    subtitle = "Help Center & Guides",
                )
                AboutBentoCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Outlined.Share,
                    title    = "Referral",
                    subtitle = "Invite your friends",
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── 7. Version footer ─────────────────────────────────────────────
            Text(
                text      = "FOCUSFIRST V1.0.0",
                fontSize  = 12.sp,
                letterSpacing = 1.sp,
                color     = Color.White.copy(alpha = 0.2f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ============================================================================
// Sub-composables
// ============================================================================

// ── Glass top bar ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsTopBar() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = "Settings",
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White,
        )
    }
}

// ── Pro features card ─────────────────────────────────────────────────────────

@Composable
private fun ProFeaturesCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = FocusColors.SurfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
    ) {
        Text(
            text       = "PRO FEATURES",
            fontSize   = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color      = FocusColors.TomatoRed.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text       = "Unlock Pro",
            fontSize   = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text     = "Access custom focus sounds, detailed analytics\nand more across all devices.",
            fontSize = 13.sp,
            color    = FocusColors.OnSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(50.dp))
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

// ── Glass card container ──────────────────────────────────────────────────────

private val GlassShape  = RoundedCornerShape(16.dp)
private val GlassBg     = Color.White.copy(alpha = 0.07f)
private val GlassBorder = Color.White.copy(alpha = 0.1f)

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassBg, GlassShape)
            .border(1.dp, GlassBorder, GlassShape)
            .padding(16.dp),
    ) {
        content()
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.Medium,
        letterSpacing = 1.5.sp,
        color         = Color.White.copy(alpha = 0.4f),
    )
}

// ── Toggle row ────────────────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(
    label:     String,
    checked:   Boolean,
    onChanged: (Boolean) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text     = label,
            fontSize = 15.sp,
            color    = FocusColors.OnSurface,
        )
        Switch(
            checked         = checked,
            onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor       = Color.Black,
                checkedTrackColor       = Color.White,
                uncheckedThumbColor     = FocusColors.OnSurfaceVariant,
                uncheckedTrackColor     = FocusColors.SurfaceContainerHighest,
                uncheckedBorderColor    = FocusColors.OutlineVariant,
            ),
        )
    }
}

// ── Slider row ────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSliderRow(
    label:      String,
    value:      Float,
    valueLabel: String,
    range:      ClosedFloatingPointRange<Float>,
    steps:      Int,
    onChanged:  (Float) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text     = label,
            fontSize = 15.sp,
            color    = FocusColors.OnSurface,
        )
        Text(
            text     = valueLabel,
            fontSize = 14.sp,
            color    = FocusColors.OnSurfaceVariant,
        )
    }
    Slider(
        value         = value,
        onValueChange = onChanged,
        valueRange    = range,
        steps         = steps,
        modifier      = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor           = Color.White,
            activeTrackColor     = Color.White,
            inactiveTrackColor   = Color.White.copy(alpha = 0.2f),
        ),
    )
}

// ── Theme pill row ────────────────────────────────────────────────────────────

@Composable
private fun ThemePillRow(
    selectedTheme: String,
    onSelected:    (String) -> Unit,
) {
    val options = listOf("System", "Dark", "Light")
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(50.dp)),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selectedTheme
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(50.dp),
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = option,
                    fontSize   = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) Color.Black else Color.White.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// ── About bento card ──────────────────────────────────────────────────────────

@Composable
private fun AboutBentoCard(
    modifier:  Modifier,
    icon:      ImageVector,
    title:     String,
    subtitle:  String,
) {
    Column(
        modifier = modifier
            .background(GlassBg, GlassShape)
            .border(1.dp, GlassBorder, GlassShape)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = Color.White.copy(alpha = 0.8f),
                modifier           = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text       = title,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text     = subtitle,
            fontSize = 11.sp,
            color    = FocusColors.OnSurfaceVariant,
        )
    }
}
