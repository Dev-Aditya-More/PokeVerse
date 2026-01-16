package com.aditya1875.pokeverse.screens.analysis.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.screens.analysis.TypeDiversityCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalysisContent(
    analysis: TeamAnalysis,
    teamWithTypes: List<TeamMemberWithTypes>,
    accentColor: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OverallScoreCard(
                score = analysis.coverageScore,
                accentColor = accentColor
            )
        }

        if (analysis.strengths.isNotEmpty()) {
            item {
                StrengthsCard(strengths = analysis.strengths)
            }
        }

        item {
            RecommendationsCard(recommendations = analysis.recommendations)
        }

        item {
            TypeCoverageCard(
                coverage = analysis.offensiveCoverage,
                accentColor = accentColor
            )
        }

        if (analysis.defensiveWeaknesses.isNotEmpty()) {
            item {
                WeaknessesCard(
                    weaknesses = analysis.defensiveWeaknesses,
                    teamWithTypes = teamWithTypes
                )
            }
        }

        if (analysis.resistances.isNotEmpty()) {
            item {
                ResistancesCard(
                    resistances = analysis.resistances,
                    teamWithTypes = teamWithTypes
                )
            }
        }

        item {
            TypeDiversityCard(diversity = analysis.typeDiversity)
        }
    }
}