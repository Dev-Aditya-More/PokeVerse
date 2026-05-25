package com.aditya1875.pokeverse.feature.leaderboard.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.utils.SoundManager
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private val Gold = Color(0xFFFFD700)
private val GoldDim = Color(0xFFFFD700).copy(alpha = 0.15f)

@Composable
fun XPOverlay(
    result: XPResult?,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        // XP toast
        AnimatedVisibility(
            visible = result != null && result.xpGained > 0 && !result.leveledUp,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit  = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
        ) {
            result?.let { XPToast(it, onDismiss) }
        }

        if (result?.leveledUp == true) {
            LevelUpCelebration(result = result, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun XPToast(result: XPResult, onDismiss: () -> Unit) {

    LaunchedEffect(result) {
        delay(2200)
        onDismiss()
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = result.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LevelUpCelebration(result: XPResult, onDismiss: () -> Unit) {

    val soundManager: SoundManager = koinInject()

    LaunchedEffect(result) {
        delay(5000)
        onDismiss()
    }

    soundManager.play(SoundManager.Sound.LEVEL_UP)

    // Card bounces in
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardVisible = true }

    // XP progress bar animates after card appears
    val xpProgress by animateFloatAsState(
        targetValue = if (result.newNextLevelXp > 0)
            result.newCurrentXp.toFloat() / result.newNextLevelXp else 0f,
        animationSpec = tween(1400, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "xp_progress"
    )

    // Badge glow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glow_alpha"
    )
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "badge_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = cardVisible,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(200))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    // Header label
                    Text(
                        text = stringResource(R.string.xp_level_up),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Gold,
                        letterSpacing = 2.sp
                    )

                    // Level badge
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .scale(badgeScale)
                            .background(GoldDim, CircleShape)
                            .border(
                                width = 3.dp,
                                color = Gold.copy(alpha = glowAlpha),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "LVL",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Gold.copy(alpha = 0.8f),
                                letterSpacing = 3.sp
                            )
                            Text(
                                text = result.newLevel.toString(),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = Gold
                            )
                        }
                    }

                    // XP progress
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progress to next level",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "${result.newCurrentXp} / ${result.newNextLevelXp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        LinearProgressIndicator(
                            progress = { xpProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50)),
                            color = Gold,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // XP gained chip
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = GoldDim
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.xp_gained, result.xpGained),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Gold
                            )
                        }
                    }

                    // Continue button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold)
                    ) {
                        Text(
                            text = stringResource(R.string.action_continue),
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
