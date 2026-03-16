package com.aditya1875.pokeverse.presentation.screens.analysis.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.presentation.screens.analysis.AnalysisColors.AMBER
import com.aditya1875.pokeverse.presentation.screens.analysis.AnalysisColors.BG
import com.aditya1875.pokeverse.presentation.screens.analysis.AnalysisColors.GREEN
import com.aditya1875.pokeverse.presentation.screens.analysis.TypeDiversityCard

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
        // ── Hero score card ───────────────────────────────────────────────────
        item { ScoreHeroCard(score = analysis.coverageScore) }

        // ── 3 quick stat chips ────────────────────────────────────────────────
        item { QuickStatsRow(analysis = analysis, teamSize = teamWithTypes.size) }

        // ── Strengths (only if any) ───────────────────────────────────────────
        if (analysis.strengths.isNotEmpty()) {
            item { InsightCard(
                title = "Strengths",
                icon = "✅",
                accentColor = GREEN,
                items = analysis.strengths
            ) }
        }

        // ── Recommendations ───────────────────────────────────────────────────
        item { InsightCard(
            title = "Suggestions",
            icon = "💡",
            accentColor = AMBER,
            items = analysis.recommendations
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