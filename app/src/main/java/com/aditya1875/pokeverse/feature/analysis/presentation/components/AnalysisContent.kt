package com.aditya1875.pokeverse.feature.analysis.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.AMBER
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.BG
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.BLUE
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.GREEN
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.RED
import com.aditya1875.pokeverse.feature.analysis.presentation.viewmodels.CoachUiState
import com.aditya1875.pokeverse.utils.Features
import com.aditya1875.pokeverse.utils.localizedTypeName

/**
 * The analysis as a scrolling show: a drifting backdrop tinted by the team's score, then each section
 * springing in turn. The numbers all come from [TeamAnalyzer]; this is only how they are presented.
 */
@Composable
fun AnalysisContent(
    analysis: TeamAnalysis,
    teamWithTypes: List<TeamMemberWithTypes>,
    coachState: CoachUiState,
    onAskCoach: () -> Unit,
    assetsEnabled: Boolean,
    caughtIds: Set<Int>
) {
    val score = analysis.coverageScore
    val tint = when {
        score >= 85 -> Color(0xFFFFD54F)
        score >= 70 -> GREEN
        score >= 55 -> BLUE
        score >= 40 -> AMBER
        else -> RED
    }
    // Which sections have already played their entrance, so scrolling back never replays it.
    val seen = remember { mutableStateMapOf<Int, Boolean>() }

    Box(Modifier.fillMaxSize().background(BG)) {
        AmbientBackdrop(tint)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Reveal(0, seen) { RankHero(analysis, teamWithTypes.size) } }

            item { Reveal(1, seen) { TeamRosterCard(team = teamWithTypes, assetsEnabled = assetsEnabled, caughtIds = caughtIds) } }

            item { Reveal(2, seen) { QuickStatsRow(analysis = analysis, teamSize = teamWithTypes.size) } }

            if (Features.COACH_ENABLED) {
                item { Reveal(3, seen) { CoachReadCard(state = coachState, onAsk = onAskCoach) } }
            }

            if (analysis.strengths.isNotEmpty()) {
                item {
                    Reveal(4, seen) {
                        InsightCard(
                            title = stringResource(R.string.analysis_strengths),
                            icon = "✅",
                            accentColor = GREEN,
                            items = analysis.strengths.map { it.text() }
                        )
                    }
                }
            }

            item {
                Reveal(5, seen) {
                    InsightCard(
                        title = stringResource(R.string.analysis_suggestions),
                        icon = "💡",
                        accentColor = AMBER,
                        items = analysis.recommendations.map { it.text() }
                    )
                }
            }

            item {
                Reveal(6, seen) {
                    DefenseSection(
                        weaknesses = analysis.defensiveWeaknesses,
                        resistances = analysis.resistances,
                        teamSize = teamWithTypes.size
                    )
                }
            }

            item { Reveal(7, seen) { CoverageSection(coverage = analysis.offensiveCoverage, teamSize = teamWithTypes.size) } }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

/** Renders a generated [AnalysisNote] in the user's language, with localized type names. */
@Composable
private fun AnalysisNote.text(): String {
    @Composable
    fun names(types: List<String>) = types.map { localizedTypeName(it) }.joinToString(", ")
    return when (this) {
        AnalysisNote.AddPokemon -> stringResource(R.string.analysis_note_add_pokemon)
        is AnalysisNote.WeakTo -> stringResource(R.string.analysis_note_weak_to, count, localizedTypeName(type))
        is AnalysisNote.NoCoverage -> stringResource(R.string.analysis_note_no_coverage, names(types))
        is AnalysisNote.TooMany -> stringResource(R.string.analysis_note_too_many, localizedTypeName(type))
        is AnalysisNote.ConsiderAdding -> stringResource(
            R.string.analysis_note_consider_adding,
            if (types.size >= 2) stringResource(R.string.analysis_note_or, localizedTypeName(types[0]), localizedTypeName(types[1]))
            else localizedTypeName(types.first())
        )
        AnalysisNote.GreatBalance -> stringResource(R.string.analysis_note_great_balance)
        is AnalysisNote.StrongCoverage -> stringResource(R.string.analysis_note_strong_coverage, names(types))
        is AnalysisNote.SolidResistance -> stringResource(R.string.analysis_note_solid_resistance, names(types))
        AnalysisNote.GoodVariety -> stringResource(R.string.analysis_note_good_variety)
    }
}
