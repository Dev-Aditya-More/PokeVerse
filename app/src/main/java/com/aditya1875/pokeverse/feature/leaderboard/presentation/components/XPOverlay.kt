package com.aditya1875.pokeverse.feature.leaderboard.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.utils.SoundManager
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

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
                tint = Color(0xFFFFD700),
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

    val soundManager : SoundManager = koinInject()

    LaunchedEffect(result) {
        delay(3000)
        onDismiss()
    }

    soundManager.play(SoundManager.Sound.LEVEL_UP)

    val infiniteTransition = rememberInfiniteTransition(label = "levelup")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue  = 1.05f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("⭐", fontSize = 64.sp,
                modifier = Modifier.scale(pulse))

            Text(
                "LEVEL UP!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700),
                modifier = Modifier.scale(pulse)
            )

            Text(
                "You reached Level ${result.newLevel}",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Text(
                "+${result.xpGained} XP",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
            ) {
                Text("Continue", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}