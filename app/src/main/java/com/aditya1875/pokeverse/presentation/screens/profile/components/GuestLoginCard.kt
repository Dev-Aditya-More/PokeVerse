package com.aditya1875.pokeverse.presentation.screens.profile.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.presentation.auth.AuthState
import com.aditya1875.pokeverse.presentation.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun GuestLoginCard(viewModel: ProfileViewModel = koinViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Map raw error codes to friendly user-facing messages.
    // Raw messages like "Failed to launch selector UI..." are developer hints, not UX.
    val friendlyError: String? = when {
        authState !is AuthState.Error -> null
        else -> {
            val raw = (authState as AuthState.Error).message.lowercase()
            when {
                "cancelled" in raw -> null  // user dismissed — no message
                "no_credentials" in raw -> "No Google account found on this device. Add one in Settings → Accounts."
                "network" in raw ||
                        "internet" in raw -> "No internet connection. Please check your network."

                "sign_in_failed" in raw ||
                        "credential_error" in raw -> "Sign-in failed. Please try again."

                else -> "Something went wrong. Try again in a moment."
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Create a Trainer Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text("Sign in to appear on leaderboards and sync your progress across devices.")
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val activity = context.findActivity()
                        ?: return@Button  // Should never be null in normal usage
                    scope.launch {
                        isLoading = true
                        viewModel.signInWithGoogle(activity)
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountCircle, null,
                            tint = Color.White, modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Sign In with Google",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Only show error when it's actionable — never show "cancelled"
            if (friendlyError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = friendlyError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}