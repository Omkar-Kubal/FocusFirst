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
                text       = "Welcome to Toki",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
        },
        text = {
            Column {
                Text(
                    text     = "Toki helps you start focus sessions quickly and keep your history on this device.",
                    fontSize = 14.sp,
                    color    = Color.White.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = "By continuing, you agree to the Terms of Service and Privacy Policy.",
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
                Text("Continue", fontWeight = FontWeight.Bold)
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
