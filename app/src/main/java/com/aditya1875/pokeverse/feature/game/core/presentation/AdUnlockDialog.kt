package com.aditya1875.pokeverse.feature.game.core.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState

@Composable
fun AdUnlockDialog(
    adState: RewardedAdState,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ad_dialog_title)) },
        text = {
            Text(
                when (adState) {
                    is RewardedAdState.Ready -> stringResource(R.string.ad_dialog_ready)
                    is RewardedAdState.Loading -> stringResource(R.string.ad_dialog_loading)
                    else -> stringResource(R.string.ad_dialog_error)
                }
            )
        },
        confirmButton = {
            when (adState) {
                is RewardedAdState.Ready -> Button(onClick = onWatchAd) { Text(stringResource(R.string.action_watch_ad)) }
                is RewardedAdState.Loading -> Button(onClick = {}, enabled = false) { Text(stringResource(R.string.ad_dialog_loading_btn)) }
                else -> OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.action_reload_ad)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
