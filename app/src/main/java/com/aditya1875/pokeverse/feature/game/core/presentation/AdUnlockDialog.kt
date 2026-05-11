package com.aditya1875.pokeverse.feature.game.core.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState

@Composable
fun AdUnlockDialog(
    adState: RewardedAdState,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hard Mode Locked") },
        text = {
            Text(
                when (adState) {
                    is RewardedAdState.Ready -> "Watch a short rewarded ad to play one round of Hard mode. Premium subscribers get permanent access."
                    is RewardedAdState.Loading -> "Loading ad… please wait a moment."
                    else -> "No ad available right now. Try again later, or subscribe to Premium for permanent access."
                }
            )
        },
        confirmButton = {
            when (adState) {
                is RewardedAdState.Ready -> Button(onClick = onWatchAd) { Text("Watch Ad") }
                is RewardedAdState.Loading -> Button(onClick = {}, enabled = false) { Text("Loading…") }
                else -> {}
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
