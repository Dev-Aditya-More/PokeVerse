package com.aditya1875.pokeverse.feature.progression.presentation

import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.CalendarToday
import com.aditya1875.pokeverse.feature.core.ui.components.CoinIcon
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aditya1875.pokeverse.utils.SoundManager
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.progression.domain.WeeklyPrizes

/**
 * One-time introduction to Dex Coins and the weekly prizes, shown to everyone once.
 * Guests get the same tour plus a nudge to sign in, since coins are tied to an account.
 */
@Composable
fun CoinsIntroDialog(isGuest: Boolean, onSeeRewards: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        shape = RoundedCornerShape(24.dp),
        icon = { CoinIcon(44.dp) },
        title = {
            Text(
                stringResource(R.string.coins_intro_title),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IntroLine(Icons.Default.CalendarToday, stringResource(R.string.coins_intro_earn))
                IntroLine(
                    Icons.Default.EmojiEvents,
                    stringResource(
                        R.string.coins_intro_weekly,
                        WeeklyPrizes.PLAYED, WeeklyPrizes.TOP_10, WeeklyPrizes.THIRD, WeeklyPrizes.FIRST
                    )
                )
                IntroLine(Icons.Default.Star, stringResource(R.string.coins_intro_levels))
                IntroLine(Icons.Default.ShoppingBag, stringResource(R.string.coins_intro_spend))
                if (isGuest) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            stringResource(R.string.coins_intro_guest),
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSeeRewards, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.coins_intro_cta), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.action_not_now)) } }
    )
}

private val Gold = Color(0xFFFFC24B)
private val Silver = Color(0xFFC9D1DB)
private val Bronze = Color(0xFFCD7F32)

/**
 * Shown once, the first time the app opens after a Monday payout: where you finished and what it paid.
 * Presented like a level-up: dark scrim, a card that springs in, a glowing medal, a coin counter that
 * ticks up while coins fly off the medal, a sound with haptics, and confetti for the top 10.
 */
@Composable
fun WeeklyRewardDialog(rank: Int, coins: Int, onSeeRewards: () -> Unit, onDismiss: () -> Unit) {
    val tier = WeeklyPrizes.tierOf(rank)
    val sound: SoundManager = koinInject()
    val medalColor = when (rank) {
        1 -> Gold
        2 -> Silver
        3 -> Bronze
        else -> if (tier == WeeklyPrizes.Tier.TOP_10) Gold else MaterialTheme.colorScheme.primary
    }
    val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> if (tier == WeeklyPrizes.Tier.TOP_10) "⭐" else "🪙" }

    // Once, not per recomposition: the sound carries its own haptic.
    LaunchedEffect(Unit) {
        sound.play(if (tier == WeeklyPrizes.Tier.PLAYED) SoundManager.Sound.CORRECT_ANSWER else SoundManager.Sound.LEVEL_UP)
    }

    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardVisible = true }

    // The count-up starts just after the card lands.
    val shownCoins by animateFloatAsState(
        targetValue = if (cardVisible) coins.toFloat() else 0f,
        animationSpec = tween(1500, delayMillis = 450, easing = FastOutSlowInEasing),
        label = "weeklyCoins"
    )
    val burst by animateFloatAsState(
        targetValue = if (cardVisible) 1f else 0f,
        animationSpec = tween(1700, delayMillis = 350, easing = LinearOutSlowInEasing),
        label = "coinBurst"
    )
    val glow = rememberInfiniteTransition(label = "weeklyGlow")
    val glowAlpha by glow.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "glowAlpha"
    )
    val badgeScale by glow.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "badgeScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            if (tier != WeeklyPrizes.Tier.PLAYED) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    // The podium keeps celebrating; the rest get one burst.
                    iterations = if (tier == WeeklyPrizes.Tier.TOP_10) 1 else LottieConstants.IterateForever
                )
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AnimatedVisibility(
                visible = cardVisible,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) + fadeIn(tween(200))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.88f),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            stringResource(
                                when (tier) {
                                    WeeklyPrizes.Tier.FIRST -> R.string.weekly_reward_title_first
                                    WeeklyPrizes.Tier.PODIUM -> R.string.weekly_reward_title_podium
                                    WeeklyPrizes.Tier.TOP_10 -> R.string.weekly_reward_title_top10
                                    WeeklyPrizes.Tier.PLAYED -> R.string.weekly_reward_title_played
                                }
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = medalColor,
                            textAlign = TextAlign.Center
                        )

                        // The medal, with coins flying off it.
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(112.dp)
                                    .scale(badgeScale)
                                    .background(medalColor.copy(alpha = 0.16f), CircleShape)
                                    .border(3.dp, medalColor.copy(alpha = glowAlpha), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(medal, fontSize = 52.sp)
                            }
                            CoinBurst(progress = burst)
                        }

                        Text(
                            text = stringResource(R.string.weekly_reward_amount, shownCoins.toInt()),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Gold
                        )

                        Text(
                            if (tier == WeeklyPrizes.Tier.PLAYED) stringResource(R.string.weekly_reward_body_played, coins)
                            else stringResource(R.string.weekly_reward_body, rank, coins),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold)
                        ) {
                            Text(
                                stringResource(R.string.weekly_reward_nice),
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        TextButton(onClick = onSeeRewards) { Text(stringResource(R.string.coins_intro_cta)) }
                    }
                }
            }
        }
    }
}

/** A handful of coins that pop off the medal, drift up and fade, driven by one 0..1 [progress]. */
@Composable
private fun CoinBurst(progress: Float) {
    if (progress <= 0f || progress >= 1f) return
    val spread = listOf(-52f, -30f, -8f, 14f, 36f, 56f, -42f, 46f)
    spread.forEachIndexed { i, x ->
        // Stagger: each coin starts a little after the previous one.
        val local = ((progress - i * 0.06f) / 0.7f).coerceIn(0f, 1f)
        if (local <= 0f || local >= 1f) return@forEachIndexed
        CoinIcon(
            size = 18.dp,
            modifier = Modifier.graphicsLayer {
                translationX = x.dp.toPx() * local
                translationY = -(34.dp.toPx() + 64.dp.toPx() * local)
                alpha = 1f - local * local
                val s = 0.7f + 0.5f * (1f - local)
                scaleX = s
                scaleY = s
            }
        )
    }
}

@Composable
private fun IntroLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = Color(0xFFFFC24B), modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
