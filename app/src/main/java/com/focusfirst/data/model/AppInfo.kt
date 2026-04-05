package com.focusfirst.data.model

import android.graphics.drawable.Drawable

/**
 * Lightweight representation of an installed app, used in the
 * Focus Guard "Blocked Apps" list.
 *
 * [icon] is kept as a nullable [Drawable] rather than a Bitmap to avoid
 * large heap allocations during the initial list-load; it is loaded lazily
 * per-item via Coil / AsyncImage in the UI.
 */
data class AppInfo(
    val packageName: String,
    val appName:     String,
    val icon:        Drawable?,
)
