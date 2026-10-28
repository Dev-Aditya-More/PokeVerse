package com.aditya1875.pokeverse.feature.analysis.presentation.components

import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors
import com.aditya1875.pokeverse.feature.pokemon.dex.PartyBadge
import com.aditya1875.pokeverse.feature.pokemon.dex.PokeBallIcon
import com.aditya1875.pokeverse.feature.pokemon.dex.isTrainersParty
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.TrainerChip
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import kotlinx.coroutines.delay

/**
 * The team as a lineup: members pop in, float, and can be tapped. A tapped member steps into the
 * spotlight and the panel underneath shows exactly what hits it, what it resists and what it is
 * immune to, so the team-wide numbers can be checked against each member.
 * Sprites stay silhouettes until Original Assets is on; names and types still carry the information.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamRosterCard(team: List<TeamMemberWithTypes>, assetsEnabled: Boolean, caughtIds: Set<Int>) {
    var selected by remember { mutableStateOf<Int?>(null) }
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AnalysisColors.CARD)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.analysis_team_title, team.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (isTrainersParty(team.map { it.id }, caughtIds)) {
                PartyBadge(container = Color.White.copy(alpha = 0.1f), content = Color.White)
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            team.forEachIndexed { index, member ->
                LineupMember(
                    index = index,
                    member = member,
                    selected = selected == index,
                    dimmed = selected != null && selected != index,
                    assetsEnabled = assetsEnabled,
                    caught = member.id in caughtIds,
                    modifier = Modifier.fillMaxWidth(1f / 3f),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selected = if (selected == index) null else index
                    },
                )
            }
        }

        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 6 }) togetherWith fadeOut(tween(120))
            },
            label = "spotlight",
        ) { index ->
            val member = index?.let { team.getOrNull(it) }
            if (member == null) {
                Text(
                    "👆  " + stringResource(R.string.an_spotlight_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
            } else {
                Spotlight(member)
            }
        }
    }
}

@Composable
private fun LineupMember(
    index: Int,
    member: TeamMemberWithTypes,
    selected: Boolean,
    dimmed: Boolean,
    assetsEnabled: Boolean,
    caught: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = pokemonTypeColor(member.types.firstOrNull() ?: "normal")

    // Pop in, staggered.
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(300L + index * 110L)
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
    }
    // Idle float; every member on its own rhythm so the row never moves in lockstep.
    val float by rememberInfiniteTransition(label = "float$index").animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1700 + index * 190, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY",
    )
    val lift by animateFloatAsState(
        targetValue = if (selected) 1.16f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "lift",
    )
    val glow by animateFloatAsState(if (selected) 0.55f else 0f, tween(250), label = "glow")
    val fade by animateFloatAsState(if (dimmed) 0.45f else 1f, tween(250), label = "dim")

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = pop.value.coerceIn(0f, 1f) * fade
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
            if (glow > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(accent.copy(alpha = glow), Color.Transparent)), CircleShape)
                )
            }
            AsyncImage(
                model = member.imageUrl,
                contentDescription = member.name,
                colorFilter = if (assetsEnabled) null else ColorFilter.tint(Color.Black, BlendMode.SrcAtop),
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer {
                        translationY = float.dp.toPx()
                        scaleX = lift
                        scaleY = lift
                    },
            )
            if (caught) {
                PokeBallIcon(Modifier.align(Alignment.BottomEnd).size(16.dp))
            }
        }
        Text(
            member.name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            color = if (selected) accent else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        member.types.forEach { type -> TrainerChip(localizedTypeName(type), pokemonTypeColor(type)) }
    }
}

/** What hits the tapped Pokémon, what it shrugs off, and what cannot touch it at all. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Spotlight(member: TeamMemberWithTypes) {
    val defense = remember(member) {
        PokemonTypeData.getDualTypeDefense(member.types.firstOrNull() ?: "normal", member.types.getOrNull(1))
    }
    val weak = defense.filterValues { it > 1.0 }.entries.sortedByDescending { it.value }
    val resists = defense.filterValues { it > 0.0 && it < 1.0 }.entries.sortedBy { it.value }
    val immune = defense.filterValues { it == 0.0 }.keys.toList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            member.name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )
        if (weak.isNotEmpty()) {
            MatchupRow(stringResource(R.string.an_spot_weak), AnalysisColors.RED) {
                weak.forEachIndexed { i, (type, mult) -> MatchupChip(type, multLabel(mult), i) }
            }
        }
        if (resists.isNotEmpty()) {
            MatchupRow(stringResource(R.string.an_spot_resists), AnalysisColors.GREEN) {
                resists.forEachIndexed { i, (type, mult) -> MatchupChip(type, multLabel(mult), i) }
            }
        }
        if (immune.isNotEmpty()) {
            MatchupRow(stringResource(R.string.an_spot_immune), AnalysisColors.BLUE) {
                immune.forEachIndexed { i, type -> MatchupChip(type, "0", i) }
            }
        }
    }
}

private fun multLabel(mult: Double): String = when (mult) {
    4.0 -> "×4"
    2.0 -> "×2"
    0.5 -> "½"
    0.25 -> "¼"
    else -> "×$mult"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchupRow(label: String, color: Color, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.9f),
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            chips()
        }
    }
}

@Composable
private fun MatchupChip(type: String, mult: String, index: Int) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 45L)
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
    }
    val color = pokemonTypeColor(type)
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = pop.value.coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.2f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.8f)), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(localizedTypeName(type), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text("  $mult", color = color, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}
