package com.focusfirst.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// TODO: implement full HomeScreen (timer face, preset selector, controls)
@Composable
fun HomeScreen(
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Home — Timer")
    }
}
