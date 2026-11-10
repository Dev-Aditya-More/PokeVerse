package com.aditya1875.pokeverse.feature.core.ui.components

import com.aditya1875.pokeverse.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Kept for the many screens that call it: it is now the shared [LoadErrorState] in its offline form,
 * so every "no internet" in the app looks the same and recovers by itself when the connection returns.
 */
@Composable
fun NoInternetScreen(
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    LoadErrorState(
        kind = com.aditya1875.pokeverse.utils.LoadErrorKind.OFFLINE,
        onRetry = onRetry ?: {},
        modifier = modifier
    )
}
