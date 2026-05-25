package com.aditya1875.pokeverse.feature.game.pokequiz.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.core.presentation.GameResultLayout
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultHeroIcon
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatChips
import com.aditya1875.pokeverse.feature.game.core.presentation.ResultStatRow
import com.aditya1875.pokeverse.feature.game.pokequiz.domain.model.QuizDifficulty
import com.aditya1875.pokeverse.utils.SoundManager
import org.koin.compose.koinInject

@Composable
fun QuizResultScreen(
    score: Int,
    correctAnswers: Int,
    totalQuestions: Int,
    difficulty: QuizDifficulty,
    stars: Int,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit,
    soundManager: SoundManager = koinInject()
) {
    LaunchedEffect(Unit) { soundManager.play(SoundManager.Sound.GAME_WIN) }

    val pct = (correctAnswers.toFloat() / totalQuestions * 100).toInt()
    val wrong = totalQuestions - correctAnswers
    val heroColor = when {
        stars == 3 -> Color(0xFFFFD700)
        stars == 2 -> Color(0xFF4CAF50)
        stars == 1 -> Color(0xFF2196F3)
        else       -> Color(0xFF9E9E9E)
    }
    val titleText = when (stars) {
        3 -> stringResource(R.string.result_title_perfect)
        2 -> stringResource(R.string.result_title_great_job)
        1 -> stringResource(R.string.result_title_good_try)
        else -> stringResource(R.string.result_title_keep_practicing)
    }

    GameResultLayout(
        title = titleText,
        subtitle = difficulty.name.lowercase()
            .replaceFirstChar { it.uppercase() } + " difficulty",
        score = score.toString(),
        scoreLabel = stringResource(R.string.result_score_label_points),
        heroColor = heroColor,
        stars = stars,
        onPlayAgain = onPlayAgain,
        onBack = onBackToMenu,
        heroContent = {
            ResultHeroIcon(
                icon = when (stars) {
                    3    -> Icons.Default.EmojiEvents
                    2    -> Icons.Default.WorkspacePremium
                    1    -> Icons.Default.Grade
                    else -> Icons.Default.MenuBook
                },
                heroColor = heroColor
            )
        },
        statsContent = {
            ResultStatChips(
                stringResource(R.string.quiz_stat_correct) to "$correctAnswers",
                stringResource(R.string.quiz_stat_wrong) to "$wrong",
                stringResource(R.string.quiz_stat_accuracy) to "$pct%"
            )
            Spacer(Modifier.height(16.dp))
            ResultStatRow(
                label = stringResource(R.string.quiz_stat_questions_answered),
                value = "$correctAnswers / $totalQuestions",
                icon = Icons.Default.Quiz
            )
            ResultStatRow(
                label = stringResource(R.string.quiz_stat_score_per_question),
                value = if (totalQuestions > 0) "${score / totalQuestions}" else "0",
                icon = Icons.Default.BarChart
            )
            ResultStatRow(
                label = stringResource(R.string.label_difficulty),
                value = difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                icon = Icons.Default.Speed,
                isLast = true
            )
        }
    )
}