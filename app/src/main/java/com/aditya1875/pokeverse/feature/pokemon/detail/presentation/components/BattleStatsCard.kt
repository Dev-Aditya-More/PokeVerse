package com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats.GO_MAX_IV
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats.GO_MAX_LEVEL
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats.GoStatsRepository
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats.MAX_EV_PER_STAT
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats.MAX_EV_TOTAL
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats.MAX_IV
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats.Nature
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats.StatCalculator
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.screens.GlossyCard
import kotlin.math.roundToInt
import org.koin.compose.koinInject

// ─────────────────────────────────────────────────────────────────────────────
// Shared shell: collapsed by default, expands into the calculator body
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CalculatorShell(
    title: String,
    subtitle: String,
    accentColor: Color,
    onLearnMoreClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    GlossyCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.battle_calc_collapse else R.string.battle_calc_expand
                    ),
                    tint = accentColor,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column(Modifier.padding(top = 14.dp)) {
                    if (onLearnMoreClick != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLearnMoreClick() }
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.battle_calc_learn_more),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                    content()
                }
            }
        }
    }
}

@Composable
private fun IntStepper(
    value: Int,
    range: IntRange,
    step: Int = 1,
    accentColor: Color,
    onValueChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onValueChange((value - step).coerceIn(range)) },
            enabled = value > range.first,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.width(34.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        IconButton(
            onClick = { onValueChange((value + step).coerceIn(range)) },
            enabled = value < range.last,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mainline Stat Calculator — IV / EV / Nature, pure math on PokeAPI base stats
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StatCalculatorCard(pokemon: PokemonResponse, accentColor: Color) {
    var level by remember { mutableIntStateOf(50) }
    var nature by remember { mutableStateOf(Nature.HARDY) }
    var natureMenuOpen by remember { mutableStateOf(false) }
    val ivs = remember { mutableStateOf(pokemon.stats.associate { it.stat.name to MAX_IV }) }
    val evs = remember { mutableStateOf(pokemon.stats.associate { it.stat.name to 0 }) }

    val evTotal = evs.value.values.sum()
    var showGuide by remember { mutableStateOf(false) }

    CalculatorShell(
        title = stringResource(R.string.stat_calc_title),
        subtitle = stringResource(R.string.stat_calc_subtitle),
        accentColor = accentColor,
        onLearnMoreClick = { showGuide = true }
    ) {
        // Level
        Text(
            "${stringResource(R.string.stat_calc_level)}: $level",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            value = level.toFloat(),
            onValueChange = { level = it.roundToInt() },
            valueRange = 1f..100f,
            steps = 98,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
        )

        Spacer(Modifier.height(10.dp))

        // Nature
        Box {
            Surface(
                onClick = { natureMenuOpen = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stringResource(R.string.stat_calc_nature),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            nature.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (nature.boosts == null) stringResource(R.string.stat_calc_nature_neutral)
                        else stringResource(
                            R.string.stat_calc_nature_boost_lower,
                            statLabel(nature.boosts!!),
                            statLabel(nature.lowers!!)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(expanded = natureMenuOpen, onDismissRequest = { natureMenuOpen = false }) {
                Nature.entries.forEach { n ->
                    DropdownMenuItem(
                        text = {
                            val boosts = n.boosts
                            val lowers = n.lowers
                            Text(
                                if (boosts == null || lowers == null) n.displayName
                                else "${n.displayName} (+${statLabel(boosts)} / -${statLabel(lowers)})"
                            )
                        },
                        onClick = { nature = n; natureMenuOpen = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Per-stat rows
        pokemon.stats.forEach { stat ->
            val statName = stat.stat.name
            val iv = ivs.value[statName] ?: MAX_IV
            val ev = evs.value[statName] ?: 0
            val finalValue = if (statName == "hp") {
                StatCalculator.calculateHp(stat.base_stat, iv, ev, level)
            } else {
                StatCalculator.calculateStat(stat.base_stat, iv, ev, level, nature.multiplierFor(statName))
            }
            val natureTag = when {
                nature.boosts == statName -> " ▲"
                nature.lowers == statName -> " ▼"
                else -> ""
            }
            val natureTagColor = when {
                nature.boosts == statName -> Color(0xFF4CAF50)
                nature.lowers == statName -> Color(0xFFE53935)
                else -> MaterialTheme.colorScheme.onSurface
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            statLabel(statName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (natureTag.isNotEmpty()) {
                            Text(natureTag, style = MaterialTheme.typography.bodySmall, color = natureTagColor, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        finalValue.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.stat_calc_iv), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IntStepper(
                        value = iv,
                        range = 0..MAX_IV,
                        accentColor = accentColor,
                        onValueChange = { ivs.value = ivs.value + (statName to it) }
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.stat_calc_ev), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IntStepper(
                        value = ev,
                        range = 0..MAX_EV_PER_STAT,
                        step = 4,
                        accentColor = accentColor,
                        onValueChange = { newEv ->
                            val delta = newEv - ev
                            val newTotal = evTotal + delta
                            if (delta <= 0 || newTotal <= MAX_EV_TOTAL) {
                                evs.value = evs.value + (statName to newEv)
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val evOverLimit = evTotal > MAX_EV_TOTAL
        Text(
            stringResource(R.string.stat_calc_ev_total, evTotal, MAX_EV_TOTAL),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (evOverLimit) MaterialTheme.colorScheme.error else accentColor
        )

        Spacer(Modifier.height(10.dp))

        Surface(
            onClick = {
                level = 50
                nature = Nature.HARDY
                ivs.value = pokemon.stats.associate { it.stat.name to MAX_IV }
                evs.value = pokemon.stats.associate { it.stat.name to 0 }
            },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                stringResource(R.string.stat_calc_reset),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showGuide) {
        StatGuideSheet(onDismiss = { showGuide = false })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pokémon GO CP Calculator — uses bundled GO base stats + CP multiplier table
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GoBattlePowerCard(
    pokemonId: Int,
    accentColor: Color,
    repository: GoStatsRepository = koinInject()
) {
    val base = remember(pokemonId) { repository.getBaseStats(pokemonId) }
    var showGuide by remember { mutableStateOf(false) }

    CalculatorShell(
        title = stringResource(R.string.go_cp_title),
        subtitle = stringResource(R.string.go_cp_subtitle),
        accentColor = accentColor,
        onLearnMoreClick = { showGuide = true }
    ) {
        if (base == null) {
            Text(
                stringResource(R.string.go_cp_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@CalculatorShell
        }

        val levels = remember { repository.availableLevels() }
        var levelIndex by remember {
            mutableIntStateOf(levels.indexOf(40f).takeIf { it >= 0 } ?: (levels.size - 1))
        }
        var ivAtk by remember { mutableIntStateOf(GO_MAX_IV) }
        var ivDef by remember { mutableIntStateOf(GO_MAX_IV) }
        var ivSta by remember { mutableIntStateOf(GO_MAX_IV) }

        val level = levels.getOrElse(levelIndex) { GO_MAX_LEVEL }
        val cp = repository.calculateCp(base, level, ivAtk, ivDef, ivSta)
        val hp = repository.calculateHp(base, level, ivSta)
        val ivPercent = ((ivAtk + ivDef + ivSta) * 100) / 45

        // Big CP readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(stringResource(R.string.go_cp_result_cp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    cp.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.go_cp_result_hp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$hp", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(4.dp))
        Surface(shape = RoundedCornerShape(50), color = accentColor.copy(alpha = 0.14f)) {
            Text(
                stringResource(R.string.go_cp_iv_percent, ivPercent),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }

        Spacer(Modifier.height(14.dp))

        // Level
        Text(
            "${stringResource(R.string.go_cp_level)}: ${formatGoLevel(level)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            value = levelIndex.toFloat(),
            onValueChange = { levelIndex = it.roundToInt() },
            valueRange = 0f..(levels.size - 1).toFloat(),
            steps = (levels.size - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
        )

        GoIvSlider(stringResource(R.string.go_cp_iv_attack), ivAtk, accentColor) { ivAtk = it }
        GoIvSlider(stringResource(R.string.go_cp_iv_defense), ivDef, accentColor) { ivDef = it }
        GoIvSlider(stringResource(R.string.go_cp_iv_stamina), ivSta, accentColor) { ivSta = it }

        Spacer(Modifier.height(4.dp))

        Surface(
            onClick = { ivAtk = GO_MAX_IV; ivDef = GO_MAX_IV; ivSta = GO_MAX_IV },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                stringResource(R.string.go_cp_perfect_ivs),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        val maxCp = repository.calculateCp(base, GO_MAX_LEVEL, GO_MAX_IV, GO_MAX_IV, GO_MAX_IV)
        Text(
            stringResource(R.string.go_cp_max_note, formatGoLevel(GO_MAX_LEVEL)) + " → $maxCp CP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.go_cp_raid_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showGuide) {
        GoCpGuideSheet(onDismiss = { showGuide = false })
    }
}

@Composable
private fun GoIvSlider(label: String, value: Int, accentColor: Color, onValueChange: (Int) -> Unit) {
    Spacer(Modifier.height(8.dp))
    Text("$label: $value", style = MaterialTheme.typography.labelMedium)
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        valueRange = 0f..GO_MAX_IV.toFloat(),
        steps = GO_MAX_IV - 1,
        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
    )
}

private fun formatGoLevel(level: Float): String =
    if (level == level.toInt().toFloat()) level.toInt().toString() else level.toString()

// ─────────────────────────────────────────────────────────────────────────────
// Guide sheets — plain-language explainers opened via "Learn more" on each card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GuideSection(heading: String, body: String, accentColor: Color) {
    Column(Modifier.padding(bottom = 18.dp)) {
        Text(
            heading,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatGuideSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                stringResource(R.string.stat_guide_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(16.dp))
            val accent = MaterialTheme.colorScheme.primary
            GuideSection(stringResource(R.string.stat_guide_iv_heading), stringResource(R.string.stat_guide_iv_body), accent)
            GuideSection(stringResource(R.string.stat_guide_ev_heading), stringResource(R.string.stat_guide_ev_body), accent)
            GuideSection(stringResource(R.string.stat_guide_nature_heading), stringResource(R.string.stat_guide_nature_body), accent)
            GuideSection(stringResource(R.string.stat_guide_together_heading), stringResource(R.string.stat_guide_together_body), accent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoCpGuideSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                stringResource(R.string.go_guide_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(16.dp))
            val accent = MaterialTheme.colorScheme.primary
            GuideSection(stringResource(R.string.go_guide_cp_heading), stringResource(R.string.go_guide_cp_body), accent)
            GuideSection(stringResource(R.string.go_guide_iv_heading), stringResource(R.string.go_guide_iv_body), accent)
            GuideSection(stringResource(R.string.go_guide_appraisal_heading), stringResource(R.string.go_guide_appraisal_body), accent)
            GuideSection(stringResource(R.string.go_guide_raid_heading), stringResource(R.string.go_guide_raid_body), accent)
        }
    }
}
