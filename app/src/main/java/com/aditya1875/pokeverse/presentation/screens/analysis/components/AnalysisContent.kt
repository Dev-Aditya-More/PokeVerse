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
import com.aditya1875.pokeverse.presentation.screens.analysis.TypeDiversityCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalysisContent(
    analysis: TeamAnalysis,
    teamWithTypes: List<TeamMemberWithTypes>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0F0F),
                        Color(0xFF1A1A1A),
                        Color(0xFF0F0F0F)
                    )
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Score with gradient card
        item {
            OverallScoreCard(score = analysis.coverageScore)
        }

        // Quick Stats Row
        item {
            QuickStatsRow(analysis = analysis, teamWithTypes = teamWithTypes)
        }

        // Strengths
        if (analysis.strengths.isNotEmpty()) {
            item {
                StrengthsCard(strengths = analysis.strengths)
            }
        }

        // Recommendations
        item {
            RecommendationsCard(recommendations = analysis.recommendations)
        }

        // Type Coverage with better visualization
        item {
            TypeCoverageCard(coverage = analysis.offensiveCoverage)
        }

        item {
            DefensiveAnalysisCard(
                weaknesses = analysis.defensiveWeaknesses,
                resistances = analysis.resistances,
                teamWithTypes = teamWithTypes
            )
        }

        // Type Diversity
        item {
            TypeDiversityCard(diversity = analysis.typeDiversity)
        }

        // Bottom spacing
        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}