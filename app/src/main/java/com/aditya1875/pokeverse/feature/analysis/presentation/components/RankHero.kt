package com.aditya1875.pokeverse.feature.analysis.presentation.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors
import com.aditya1875.pokeverse.utils.SoundManager
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Fun labels for what a team is good (or risky) at, shown as chips under the rank. */
enum class SquadTrait(val emoji: String, @StringRes val label: Int) {
    IRON_WALL("🛡️", R.string.an_trait_wall),
    COVERAGE_KING("⚔️", R.string.an_trait_king),
    RAINBOW("🌈", R.string.an_trait_rainbow),
    GLASS_CANNON("💥", R.string.an_trait_glass),
    TYPE_TWINS("👯", R.string.an_trait_twins),
}

fun squadTraits(analysis: TeamAnalysis, teamSize: Int): List<SquadTrait> {
    val covered = analysis.offensiveCoverage.values.count { it > 0 }
    return buildList {
        if (analysis.resistances.size >= 8) add(SquadTrait.IRON_WALL)
        if (covered >= 16) add(SquadTrait.COVERAGE_KING)
        if (analysis.typeDiversity.uniqueTypes >= 8) add(SquadTrait.RAINBOW)
        if (covered >= 14 && analysis.defensiveWeaknesses.size >= 8) add(SquadTrait.GLASS_CANNON)
        if (teamSize >= 4 && analysis.typeDiversity.hasDuplicates && analysis.typeDiversity.uniqueTypes <= teamSize - 1) {
            add(SquadTrait.TYPE_TWINS)
        }
    }.take(3)
}

private val Gold = Color(0xFFFFD54F)

/**
 * The headline of the screen: a glowing gauge that counts up to the score, a rank title, a one-line
 * verdict and the squad's traits. Strong teams get confetti and a fanfare.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RankHero(analysis: TeamAnalysis, teamSize: Int) {
    val score = analysis.coverageScore
    val color = when {
        score >= 85 -> Gold
        score >= 70 -> AnalysisColors.GREEN
        score >= 55 -> AnalysisColors.BLUE
        score >= 40 -> AnalysisColors.AMBER
        else -> AnalysisColors.RED
    }
    val (emoji, rank) = when {
        score >= 85 -> "👑" to R.string.an_rank_champion
        score >= 70 -> "🏆" to R.string.an_rank_elite
        score >= 55 -> "💪" to R.string.an_rank_gym
        score >= 40 -> "⚡" to R.string.an_rank_rookie
        else -> "🌱" to R.string.an_rank_pep
    }
    val verdict = when {
        score >= 80 -> R.string.analysis_verdict_excellent
        score >= 60 -> R.string.analysis_verdict_good
        score >= 40 -> R.string.analysis_verdict_fair
        else -> R.string.analysis_verdict_poor
    }

    val shown = rememberCountUp(score, 1400)
    var goal by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(score) { goal = score / 100f }
    val sweep by animateFloatAsState(goal, tween(1400, easing = FastOutSlowInEasing), label = "sweep")

    val transition = rememberInfiniteTransition(label = "heroPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val bounce by transition.animateFloat(
        initialValue = 1f, targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounce",
    )

    // Fanfare + confetti once the gauge lands, for teams worth celebrating.
    val sound: SoundManager = koinInject()
    var celebrate by remember { mutableStateOf(false) }
    LaunchedEffect(score) {
        delay(1300)
        if (score >= 70) {
            celebrate = true
            sound.play(SoundManager.Sound.LEVEL_UP, 0.7f)
        }
    }

    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AnalysisColors.CARD)
            .border(BorderStroke(1.dp, color.copy(alpha = 0.35f)), shape)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.radialGradient(listOf(color.copy(alpha = 0.16f), Color.Transparent), radius = 900f))
        )
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.analysis_team_rating),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))

            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 16.dp.toPx()
                    val inset = stroke
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                    val topLeft = Offset(inset, inset)
                    val start = -220f
                    val total = 260f
                    drawArc(Color.White.copy(alpha = 0.07f), start, total, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    // Soft halo, then the arc itself.
                    drawArc(color.copy(alpha = 0.22f * pulse), start, total * sweep, false, topLeft, arcSize, style = Stroke(stroke * 2f, cap = StrokeCap.Round))
                    drawArc(color, start, total * sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    // A bright dot riding the end of the arc.
                    if (sweep > 0.01f) {
                        val a = (start + total * sweep) * (PI.toFloat() / 180f)
                        val r = arcSize.width / 2f
                        val c = Offset(size.width / 2f + cos(a) * r, size.height / 2f + sin(a) * r)
                        drawCircle(Color.White.copy(alpha = 0.9f), stroke * 0.42f, c)
                        drawCircle(color.copy(alpha = 0.5f * pulse), stroke * 0.9f, c)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        emoji,
                        fontSize = 30.sp,
                        modifier = Modifier.graphicsLayer { scaleX = bounce; scaleY = bounce },
                    )
                    Text(
                        "$shown",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                    Text("/100", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(rank).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(verdict),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            val traits = remember(analysis, teamSize) { squadTraits(analysis, teamSize) }
            if (traits.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    traits.forEachIndexed { i, trait -> TraitChip(trait, delayMs = 900L + i * 160L, color = color) }
                }
            }
        }
        Confetti(fire = celebrate, modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun TraitChip(trait: SquadTrait, delayMs: Long, color: Color) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
    }
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = pop.value.coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.55f)), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(trait.emoji, fontSize = 14.sp)
        Spacer(Modifier.size(6.dp))
        Text(
            stringResource(trait.label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
