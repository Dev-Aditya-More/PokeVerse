package com.aditya1875.pokeverse.feature.game.chase.presentation.components

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState
import com.aditya1875.pokeverse.feature.game.core.presentation.requestRewardedAd
import org.koin.compose.koinInject

val ChaseAccent = Color(0xFFFFD54F)
private val CardColor = Color(0xFF1B1F2B)
private val RocketRed = Color(0xFFE53935)

/** Full-screen dim that swallows taps so nothing behind it reacts. */
@Composable
private fun Scrim(alpha: Float = 0.65f, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun OverlayCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        shape = RoundedCornerShape(24.dp),
        color = CardColor,
        border = BorderStroke(1.dp, ChaseAccent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ChaseAccent)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun ChaseGuideOverlay(onDismiss: () -> Unit) {
    Scrim(alpha = 0.75f) {
        OverlayCard {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = ChaseAccent, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.chase_guide_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(Modifier.height(18.dp))
            GuideRow(Icons.Default.SwapHoriz, ChaseAccent, stringResource(R.string.chase_guide_lanes))
            Spacer(Modifier.height(14.dp))
            GuideRow(Icons.Default.GpsFixed, RocketRed, stringResource(R.string.chase_guide_nets))
            Spacer(Modifier.height(14.dp))
            GuideRow(Icons.Default.Bolt, Color(0xFF4FC3F7), stringResource(R.string.chase_guide_berries))
            Spacer(Modifier.height(14.dp))
            GuideRow(Icons.Default.Favorite, RocketRed, stringResource(R.string.chase_guide_lives))
            Spacer(Modifier.height(22.dp))
            PrimaryButton(stringResource(R.string.chase_guide_go), onDismiss)
        }
    }
}

@Composable
private fun GuideRow(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )
    }
}

/** Shown over the idle track — the whole screen is the start button. */
@Composable
fun ChaseReadyOverlay(bestScore: Int, onStart: () -> Unit) {
    val blink = rememberInfiniteTransition(label = "tap_blink")
    val textAlpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onStart),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.chase_tap_to_run),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.alpha(textAlpha)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.chase_controls_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            if (bestScore > 0) {
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.chase_best, bestScore),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ChaseAccent
                )
            }
        }
    }
}

@Composable
fun ChasePausedOverlay(onResume: () -> Unit) {
    Scrim {
        OverlayCard {
            Text(
                stringResource(R.string.chase_paused),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(stringResource(R.string.chase_resume), onResume)
        }
    }
}

/**
 * Team Rocket got Pikachu. Offers the run's single revive (free for premium,
 * rewarded ad otherwise) or ends the run.
 */
@Composable
fun ChaseCaughtOverlay(
    canRevive: Boolean,
    isPremium: Boolean,
    isOnline: Boolean,
    onRevive: () -> Unit,
    onGiveUp: () -> Unit
) {
    val adManager: IRewardedAdManager = koinInject()
    val adState by adManager.adState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(adState, isOnline, isPremium, canRevive) {
        if (canRevive && !isPremium && isOnline && adState is RewardedAdState.Idle) {
            adManager.loadAd(context)
        }
    }

    Scrim {
        OverlayCard {
            Text("🎈", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.chase_caught_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.chase_caught_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            if (canRevive) {
                OutlinedButton(
                    onClick = {
                        if (isPremium) onRevive()
                        else requestRewardedAd(context, activity, adManager, adState) { onRevive() }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ChaseAccent),
                    border = BorderStroke(1.5.dp, ChaseAccent.copy(alpha = 0.6f))
                ) {
                    Text(
                        stringResource(if (isPremium) R.string.chase_revive else R.string.chase_revive_ad),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            TextButton(onClick = onGiveUp) {
                Text(stringResource(R.string.chase_give_up), color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}
