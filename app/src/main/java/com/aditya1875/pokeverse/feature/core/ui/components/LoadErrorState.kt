package com.aditya1875.pokeverse.feature.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.utils.ConnectivityObserver
import com.aditya1875.pokeverse.utils.LoadErrorKind
import org.koin.compose.koinInject

/**
 * The one way a screen says "this didn't load". It names the actual problem (offline, slow, server,
 * unknown), offers Retry, and, when the problem is being offline, retries by itself the moment the
 * connection comes back, so the player never has to tap anything to recover.
 *
 * [kind] null means the screen only knows that something failed: the phone's connection decides
 * whether that reads as offline or as a generic problem.
 */
@Composable
fun LoadErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    kind: LoadErrorKind? = null,
    onBack: (() -> Unit)? = null,
    backLabel: Int = R.string.action_go_back,
) {
    val resolved = resolveKind(kind)
    AutoRetryWhenBackOnline(resolved, onRetry)

    val (icon, title, subtitle) = when (resolved) {
        LoadErrorKind.OFFLINE -> Triple(Icons.Default.WifiOff, R.string.load_error_offline_title, R.string.load_error_offline_body)
        LoadErrorKind.TIMEOUT -> Triple(Icons.Default.HourglassEmpty, R.string.load_error_timeout_title, R.string.load_error_timeout_body)
        LoadErrorKind.SERVER -> Triple(Icons.Default.CloudOff, R.string.load_error_server_title, R.string.load_error_server_body)
        LoadErrorKind.UNKNOWN -> Triple(Icons.Default.ErrorOutline, R.string.error_unexpected_title, R.string.error_unexpected_subtitle)
    }

    // A slow breathing pulse says "waiting", not "crashed".
    val pulse by rememberInfiniteTransition(label = "loadError").animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse"
    )

    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(44.dp).alpha(pulse)
                )
            }
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_retry), fontWeight = FontWeight.Bold)
            }
            if (onBack != null) {
                TextButton(onClick = onBack) { Text(stringResource(backLabel)) }
            }
        }
    }
}

/** The small version for one section of a screen that otherwise loaded fine (evolutions, moves...). */
@Composable
fun LoadErrorInline(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    kind: LoadErrorKind? = null,
) {
    val resolved = resolveKind(kind)
    AutoRetryWhenBackOnline(resolved, onRetry)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                iconFor(resolved),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                stringResource(R.string.load_error_section),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry), fontWeight = FontWeight.Bold) }
        }
    }
}

private fun iconFor(kind: LoadErrorKind): ImageVector = when (kind) {
    LoadErrorKind.OFFLINE -> Icons.Default.WifiOff
    LoadErrorKind.TIMEOUT -> Icons.Default.HourglassEmpty
    LoadErrorKind.SERVER -> Icons.Default.CloudOff
    LoadErrorKind.UNKNOWN -> Icons.Default.ErrorOutline
}

/** A screen that only knows "it failed" reads as offline when the phone says it is. */
@Composable
private fun resolveKind(kind: LoadErrorKind?): LoadErrorKind {
    if (kind != null) return kind
    val connectivity: ConnectivityObserver = koinInject()
    val online by connectivity.isOnline.collectAsStateWithLifecycle(initialValue = true)
    return if (online) LoadErrorKind.UNKNOWN else LoadErrorKind.OFFLINE
}

/** When the failure was being offline, retries once as soon as the connection returns. */
@Composable
private fun AutoRetryWhenBackOnline(kind: LoadErrorKind, onRetry: () -> Unit) {
    if (kind != LoadErrorKind.OFFLINE) return
    val connectivity: ConnectivityObserver = koinInject()
    val online by connectivity.isOnline.collectAsStateWithLifecycle(initialValue = false)
    var sawOffline by remember { mutableStateOf(!online) }
    LaunchedEffect(online) {
        if (!online) sawOffline = true
        else if (sawOffline) {
            sawOffline = false
            onRetry()
        }
    }
}
