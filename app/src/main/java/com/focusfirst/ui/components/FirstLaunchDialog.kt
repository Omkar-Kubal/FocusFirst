package com.focusfirst.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FirstLaunchDialog(
    onAccept:    () -> Unit,
    onLearnMore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* non-dismissible */ },
        containerColor   = Color(0xFF111111),
        title = {
            Text(
                text       = "Welcome to Toki \uD83D\uDC4B",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
        },
        text = {
            Column {
                Text(
                    text     = "By using Toki, you agree to our Terms of Service and Privacy Policy.",
                    fontSize = 14.sp,
                    color    = Color.White.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = "Toki collects no personal data. Everything stays on your device.",
                    fontSize = 12.sp,
                    color    = Color.White.copy(alpha = 0.5f),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor   = Color.Black,
                ),
            ) {
                Text("I Agree", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onLearnMore) {
                Text(
                    text  = "Privacy Policy",
                    color = Color(0xFF1A9E5F),
                )
            }
        },
    )
}
