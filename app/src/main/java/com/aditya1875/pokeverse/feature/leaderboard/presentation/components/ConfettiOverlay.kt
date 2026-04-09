package com.aditya1875.pokeverse.feature.leaderboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aditya1875.pokeverse.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun ConfettiOverlay(rank: Int) {

    // Only for top 3 (you already decided this)
    if (rank !in 1..3) return

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.confetti)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LottieAnimation(
            composition = composition,
            progress = progress,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun RankCelebrationDialog(
    rank: Int,
    displayName: String?,
    onDismiss: () -> Unit
) {
    val (title, subtitle, emoji, color) = when (rank) {
        1 -> Quad(
            "Champion!",
            "$displayName, You are #1 this week — absolute dominance 👑",
            "🏆",
            Color(0xFFFFD700)
        )
        2 -> Quad(
            "So Close!",
            "You are #2 this week - keep up the spark, $displayName",
            "🥈",
            Color(0xFFC0C0C0)
        )
        3 -> Quad(
            "Strong Finish!",
            "You are #3 this week, $displayName ",
            "🥉",
            Color(0xFFCD7F32)
        )
        else -> Quad(
            "Keep Climbing!",
            "$displayName, you're ranked #$rank this week",
            "🔥",
            MaterialTheme.colorScheme.primary
        )
    }

    Dialog(onDismissRequest = onDismiss) {

        ConfettiOverlay(rank)

        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        emoji,
                        fontSize = 36.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = color,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                // Rank badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "#$rank",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        "Continue",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)