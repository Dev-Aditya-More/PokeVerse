package com.aditya1875.pokeverse.feature.game.poketype.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.core.presentation.GameResultLayout
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultHeroIcon
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatChips
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatRow
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushState
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.compose.koinInject

@Composable
fun TypeRushResultScreen(
    state: TypeRushState.Finished,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    soundManager: SoundManager = koinInject()
) {
    LaunchedEffect(Unit) { soundManager.play(SoundManager.Sound.GAME_WIN) }

    val pct = state.correctRounds.toFloat() / state.totalRounds
    val stars = when { pct >= 0.9f -> 3; pct >= 0.7f -> 2; pct >= 0.5f -> 1; else -> 0 }
    val heroColor = when (stars) {
        3 -> Color(0xFFFFD700)
        2 -> Color(0xFF4CAF50)
        1 -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }

    GameResultLayout(
        title = when (stars) {
            3 -> stringResource(R.string.result_title_type_master)
            2 -> stringResource(R.string.result_title_well_typed)
            1 -> stringResource(R.string.result_title_keep_rushing)
            else -> stringResource(R.string.result_title_type_learner)
        },
        subtitle = stringResource(R.string.typerush_subtitle),
        score = state.score.toString(),
        scoreLabel = stringResource(R.string.result_score_label_points),
        heroColor = heroColor,
        stars = stars,
        onPlayAgain = onPlayAgain,
        onBack = onBack,
        heroContent = {
            ResultHeroIcon(
                icon = when (stars) {
                    3    -> Icons.Default.EmojiEvents
                    2    -> Icons.Default.Bolt
                    else -> Icons.Default.Speed
                },
                heroColor = heroColor
            )
        },
        statsContent = {
            ResultStatChips(
                stringResource(R.string.typerush_stat_correct)  to "${state.correctRounds}/${state.totalRounds}",
                stringResource(R.string.typerush_stat_accuracy) to "${(pct * 100).toInt()}%",
                stringResource(R.string.typerush_stat_mode)     to state.difficulty.label
            )
            Spacer(Modifier.height(16.dp))
            ResultStatRow(
                label = stringResource(R.string.typerush_stat_rounds),
                value = "${state.correctRounds} / ${state.totalRounds}",
                icon = Icons.Default.CheckCircle,
                valueColor = heroColor
            )
            ResultStatRow(
                label = stringResource(R.string.label_difficulty),
                value = state.difficulty.label,
                icon = Icons.Default.Speed,
                isLast = true
            )
        }
    )
}