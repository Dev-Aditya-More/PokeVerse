package com.aditya1875.pokeverse.feature.game.pokematch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.core.presentation.GameResultLayout
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultHeroIcon
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatChips
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatRow
import com.aditya1875.pokeverse.feature.game.pokematch.domain.model.GameState
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.compose.koinInject

@Composable
fun VictoryScreen(
    victory: GameState.Victory,
    onPlayAgain: () -> Unit,
    onChangeDifficulty: () -> Unit,
    onHome: () -> Unit,
    soundManager: SoundManager = koinInject()
) {
    LaunchedEffect(Unit) { soundManager.play(SoundManager.Sound.GAME_WIN) }

    val heroColor = when (victory.stars) {
        3    -> Color(0xFFFFD700)
        2    -> Color(0xFF4CAF50)
        1    -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }

    GameResultLayout(
        title = if (victory.isNewBest) stringResource(R.string.result_title_new_best) else stringResource(R.string.result_title_you_win),
        subtitle = stringResource(R.string.match_cleared, victory.difficulty.displayName),
        score = victory.score.toString(),
        scoreLabel = stringResource(R.string.result_score_label_score),
        heroColor = heroColor,
        stars = victory.stars,
        isNewBest = victory.isNewBest,
        onPlayAgain = onPlayAgain,
        onBack = onHome,
        heroContent = {
            ResultHeroIcon(
                icon = when (victory.stars) {
                    3    -> Icons.Default.EmojiEvents
                    2    -> Icons.Default.WorkspacePremium
                    else -> Icons.Default.Style
                },
                heroColor = heroColor
            )
        },
        statsContent = {
            ResultStatChips(
                stringResource(R.string.match_stat_score) to victory.score.toString(),
                stringResource(R.string.match_stat_moves) to victory.moves.toString(),
                stringResource(R.string.match_stat_time) to "${victory.timeTaken}s"
            )
            Spacer(Modifier.height(16.dp))
            ResultStatRow(
                label = stringResource(R.string.label_difficulty),
                value = victory.difficulty.displayName,
                icon = Icons.Default.Speed
            )
            ResultStatRow(
                label = stringResource(R.string.match_stat_moves_used),
                value = victory.moves.toString(),
                icon = Icons.Default.TouchApp
            )
            ResultStatRow(
                label = stringResource(R.string.match_stat_time_taken),
                value = "${victory.timeTaken}s",
                icon = Icons.Default.Timer,
                isLast = true
            )
        }
    )
}

@Composable
fun TimeUpScreen(
    timeUp: GameState.TimeUp,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit
) {
    val soundManager : SoundManager = koinInject()
    soundManager.play(SoundManager.Sound.TIMER_UP)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = Color(0xFFFF1744),
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = stringResource(R.string.result_title_times_up),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF1744)
            )

            Text(
                text = stringResource(R.string.result_pairs_found, timeUp.matchesFound, timeUp.totalPairs),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_try_again))
            }

            TextButton(onClick = onBack) {
                Text(stringResource(R.string.result_back_to_menu))
            }
        }
    }
}