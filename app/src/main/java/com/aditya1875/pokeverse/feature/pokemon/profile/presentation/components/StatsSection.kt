package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import java.text.NumberFormat

/** Total XP · games · streak in one slim strip (was three tall cards). */
@Composable
fun StatsSection(profile: UserProfile) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.height(IntrinsicSize.Min).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StripStat(Icons.Default.Star, Color(0xFFFFD54F), profile.totalXp, stringResource(R.string.profile_stat_total_xp), Modifier.weight(1f))
            VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            StripStat(Icons.Default.SportsEsports, MaterialTheme.colorScheme.primary, profile.gamesPlayed, stringResource(R.string.profile_stat_games), Modifier.weight(1f))
            VerticalDivider(Modifier.fillMaxHeight().padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            StripStat(Icons.Default.LocalFireDepartment, Color(0xFFFF8A3D), profile.dailyStreak, stringResource(R.string.profile_stat_streak), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StripStat(icon: ImageVector, tint: Color, value: Int, label: String, modifier: Modifier) {
    val shown by countUp(value)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                NumberFormat.getIntegerInstance().format(shown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Counts up from 0 on first show, then animates to any new value. */
@Composable
private fun countUp(target: Int) = run {
    var start by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) { start = target }
    animateIntAsState(start, tween(900, easing = FastOutSlowInEasing), label = "count")
}

private data class GameBest(val icon: ImageVector, val title: Int, val score: Int, val color: Color)

/** Personal bests as a compact two-column grid, played games first. */
@Composable
fun GameStatsSection(profile: UserProfile) {
    val games = listOf(
        GameBest(Icons.AutoMirrored.Filled.DirectionsRun, R.string.game_name_chase_title, profile.bestChaseScore, Color(0xFFFFD54F)),
        GameBest(Icons.Default.Bolt, R.string.game_name_survivor_title, profile.bestSurvivorScore, Color(0xFFFFC107)),
        GameBest(Icons.Default.RadioButtonUnchecked, R.string.game_name_wildcatch_title, profile.bestWildCatchScore, Color(0xFFEF5350)),
        GameBest(Icons.Default.FlashOn, R.string.game_name_pokeduel_title, profile.bestDuelScore, Color(0xFFFF9800)),
        GameBest(Icons.Default.SportsEsports, R.string.typerush_game_title, profile.bestTypeRushScore, Color(0xFF7986CB)),
        GameBest(Icons.Default.Quiz, R.string.game_name_pokequiz_title, profile.bestQuizScore, Color(0xFF42A5F5)),
        GameBest(Icons.Default.GridView, R.string.game_name_pokematch_title, profile.bestMatchScore, Color(0xFF66BB6A)),
        GameBest(Icons.Default.Visibility, R.string.game_name_pokeguess_title, profile.bestGuessScore, Color(0xFFBA68C8))
    ).sortedByDescending { it.score > 0 }
    val topScore = games.maxOf { it.score }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "🏆 " + stringResource(R.string.profile_personal_bests),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        games.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { BestTile(it, isTop = topScore > 0 && it.score == topScore, modifier = Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BestTile(game: GameBest, isTop: Boolean, modifier: Modifier) {
    val played = game.score > 0
    val shown by countUp(game.score)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = game.color.copy(alpha = if (played) 0.1f else 0.04f),
        border = BorderStroke(1.dp, game.color.copy(alpha = if (isTop) 0.7f else if (played) 0.25f else 0.1f)),
        modifier = modifier
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(game.color.copy(alpha = if (played) 0.22f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(game.icon, contentDescription = null, tint = game.color.copy(alpha = if (played) 1f else 0.5f), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(game.title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (played) 1f else 0.6f)
                )
                Text(
                    if (played) (if (isTop) "👑 " else "") + NumberFormat.getIntegerInstance().format(shown)
                    else stringResource(R.string.profile_not_played),
                    style = if (played) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
                    fontWeight = if (played) FontWeight.Black else FontWeight.Normal,
                    color = if (played) game.color else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
