package com.aditya1875.pokeverse.feature.analysis.presentation.components

import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.AMBER
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.BG
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.GREEN

@Composable
fun AnalysisContent(
    analysis: TeamAnalysis,
    teamWithTypes: List<TeamMemberWithTypes>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BG),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScoreHeroCard(score = analysis.coverageScore) }

        item { QuickStatsRow(analysis = analysis, teamSize = teamWithTypes.size) }

        if (analysis.strengths.isNotEmpty()) {
            item { InsightCard(
                title = stringResource(R.string.analysis_strengths),
                icon = "✅",
                accentColor = GREEN,
                items = analysis.strengths.map { it.text() }
            ) }
        }

        // ── Recommendations ───────────────────────────────────────────────────
        item { InsightCard(
            title = stringResource(R.string.analysis_suggestions),
            icon = "💡",
            accentColor = AMBER,
            items = analysis.recommendations.map { it.text() }
        ) }

        item { DefenseSection(
            weaknesses = analysis.defensiveWeaknesses,
            resistances = analysis.resistances,
            teamSize = teamWithTypes.size
        ) }

        item { CoverageSection(coverage = analysis.offensiveCoverage, teamSize = teamWithTypes.size) }

        item { Spacer(Modifier.height(32.dp)) }
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
