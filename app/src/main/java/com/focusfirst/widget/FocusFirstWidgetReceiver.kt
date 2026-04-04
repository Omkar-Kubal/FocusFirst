package com.focusfirst.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.focusfirst.MainActivity

// ============================================================================
// Keys used to pass timer state into the Glance DataStore
// ============================================================================

object WidgetKeys {
    val REMAINING  = intPreferencesKey("widget_remaining")
    val PHASE      = stringPreferencesKey("widget_phase")
    val RUNNING    = booleanPreferencesKey("widget_running")
    val TODAY      = intPreferencesKey("widget_today")
}

// ============================================================================
// FocusFirstWidget
// ============================================================================

class FocusFirstWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                TokiWidgetContent()
            }
        }
    }
}

// ============================================================================
// Widget UI
// ============================================================================

@Composable
private fun TokiWidgetContent() {
    val prefs            = currentState<Preferences>()
    val remainingSeconds = prefs[WidgetKeys.REMAINING] ?: 0
    val phase            = prefs[WidgetKeys.PHASE]     ?: "READY"
    val isRunning        = prefs[WidgetKeys.RUNNING]   ?: false
    val todayCount       = prefs[WidgetKeys.TODAY]     ?: 0

    val minutes    = remainingSeconds / 60
    val seconds    = remainingSeconds % 60
    val timeString = "%02d:%02d".format(minutes, seconds)

    val openApp = actionStartActivity<MainActivity>()

    Box(
        modifier         = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .cornerRadius(16.dp)
            .clickable(openApp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier              = GlanceModifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            // App name
            Text(
                text  = "Toki",
                style = TextStyle(
                    color      = ColorProvider(Color(0xFF888888)),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            Spacer(GlanceModifier.height(2.dp))

            // Phase label
            Text(
                text  = phase,
                style = TextStyle(
                    color      = ColorProvider(Color(0xFF888888)),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            Spacer(GlanceModifier.height(6.dp))

            // Time
            Text(
                text  = if (remainingSeconds > 0 || isRunning) timeString else "00:00",
                style = TextStyle(
                    color      = ColorProvider(Color.White),
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(GlanceModifier.height(10.dp))

            // Start button — only when idle
            if (!isRunning && remainingSeconds == 0) {
                Box(
                    modifier         = GlanceModifier
                        .background(Color.White)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                        .clickable(openApp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "Start",
                        style = TextStyle(
                            color      = ColorProvider(Color.Black),
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Spacer(GlanceModifier.height(8.dp))
            }

            // Today's session count
            if (todayCount > 0) {
                Text(
                    text  = "$todayCount 🍅",
                    style = TextStyle(
                        color    = ColorProvider(Color(0xFF888888)),
                        fontSize = 10.sp,
                    ),
                )
            }
        }
    }
}

// ============================================================================
// FocusFirstWidgetReceiver
// ============================================================================

class FocusFirstWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusFirstWidget()
}
