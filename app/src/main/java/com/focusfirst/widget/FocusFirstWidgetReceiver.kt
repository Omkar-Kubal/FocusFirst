package com.focusfirst.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.text.Text

// ============================================================================
// FocusFirstWidget — V1 stub
//
// Minimal Glance widget to satisfy the manifest receiver declaration and
// prevent a crash at install time.  Full widget UI comes in Phase 3.
// ============================================================================

class FocusFirstWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Text("FocusFirst")
        }
    }
}

// ============================================================================
// FocusFirstWidgetReceiver
// ============================================================================

class FocusFirstWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusFirstWidget()
}
