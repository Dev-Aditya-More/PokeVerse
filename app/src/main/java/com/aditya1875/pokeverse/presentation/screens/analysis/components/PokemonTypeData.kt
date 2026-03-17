package com.aditya1875.pokeverse.presentation.screens.analysis.components

import androidx.compose.ui.graphics.Color

/**
 * Pokemon Type System and Team Analysis Utilities
 */

object PokemonTypeData {

    // All Pokemon types
    val allTypes = listOf(
        "normal", "fire", "water", "electric", "grass", "ice",
        "fighting", "poison", "ground", "flying", "psychic", "bug",
        "rock", "ghost", "dragon", "dark", "steel", "fairy"
    )

    /**
     * Type effectiveness chart
     * Key: Attacking type
     * Value: Map of defending type to effectiveness multiplier
     */
    val typeEffectiveness = mapOf(
        "normal" to mapOf(
            "rock" to 0.5, "ghost" to 0.0, "steel" to 0.5
        ),
        "fire" to mapOf(
            "fire" to 0.5, "water" to 0.5, "grass" to 2.0, "ice" to 2.0,
            "bug" to 2.0, "rock" to 0.5, "dragon" to 0.5, "steel" to 2.0
        ),
        "water" to mapOf(
            "fire" to 2.0, "water" to 0.5, "grass" to 0.5, "ground" to 2.0,
            "rock" to 2.0, "dragon" to 0.5
        ),
        "electric" to mapOf(
            "water" to 2.0, "electric" to 0.5, "grass" to 0.5, "ground" to 0.0,
            "flying" to 2.0, "dragon" to 0.5
        ),
        "grass" to mapOf(
            "fire" to 0.5, "water" to 2.0, "grass" to 0.5, "poison" to 0.5,
            "ground" to 2.0, "flying" to 0.5, "bug" to 0.5, "rock" to 2.0,
            "dragon" to 0.5, "steel" to 0.5
        ),
        "ice" to mapOf(
            "fire" to 0.5, "water" to 0.5, "grass" to 2.0, "ice" to 0.5,
            "ground" to 2.0, "flying" to 2.0, "dragon" to 2.0, "steel" to 0.5
        ),
        "fighting" to mapOf(
            "normal" to 2.0, "ice" to 2.0, "poison" to 0.5, "flying" to 0.5,
            "psychic" to 0.5, "bug" to 0.5, "rock" to 2.0, "ghost" to 0.0,
            "dark" to 2.0, "steel" to 2.0, "fairy" to 0.5
        ),
        "poison" to mapOf(
            "grass" to 2.0, "poison" to 0.5, "ground" to 0.5, "rock" to 0.5,
            "ghost" to 0.5, "steel" to 0.0, "fairy" to 2.0
        ),
        "ground" to mapOf(
            "fire" to 2.0, "electric" to 2.0, "grass" to 0.5, "poison" to 2.0,
            "flying" to 0.0, "bug" to 0.5, "rock" to 2.0, "steel" to 2.0
        ),
        "flying" to mapOf(
            "electric" to 0.5, "grass" to 2.0, "fighting" to 2.0, "bug" to 2.0,
            "rock" to 0.5, "steel" to 0.5
        ),
        "psychic" to mapOf(
            "fighting" to 2.0, "poison" to 2.0, "psychic" to 0.5, "dark" to 0.0,
            "steel" to 0.5
        ),
        "bug" to mapOf(
            "fire" to 0.5, "grass" to 2.0, "fighting" to 0.5, "poison" to 0.5,
            "flying" to 0.5, "psychic" to 2.0, "ghost" to 0.5, "dark" to 2.0,
            "steel" to 0.5, "fairy" to 0.5
        ),
        "rock" to mapOf(
            "fire" to 2.0, "ice" to 2.0, "fighting" to 0.5, "ground" to 0.5,
            "flying" to 2.0, "bug" to 2.0, "steel" to 0.5
        ),
        "ghost" to mapOf(
            "normal" to 0.0, "psychic" to 2.0, "ghost" to 2.0, "dark" to 0.5
        ),
        "dragon" to mapOf(
            "dragon" to 2.0, "steel" to 0.5, "fairy" to 0.0
        ),
        "dark" to mapOf(
            "fighting" to 0.5, "psychic" to 2.0, "ghost" to 2.0, "dark" to 0.5,
            "fairy" to 0.5
        ),
        "steel" to mapOf(
            "fire" to 0.5, "water" to 0.5, "electric" to 0.5, "ice" to 2.0,
            "rock" to 2.0, "steel" to 0.5, "fairy" to 2.0
        ),
        "fairy" to mapOf(
            "fire" to 0.5, "fighting" to 2.0, "poison" to 0.5, "dragon" to 2.0,
            "dark" to 2.0, "steel" to 0.5
        )
    )

    /**
     * Calculate defensive effectiveness for a type
     * Returns map of attacking type to damage multiplier
     */
    fun getDefensiveWeaknesses(defendingType: String): Map<String, Double> {
        val weaknesses = mutableMapOf<String, Double>()

        typeEffectiveness.forEach { (attackingType, matchups) ->
            val multiplier = matchups[defendingType] ?: 1.0
            if (multiplier != 1.0) {
                weaknesses[attackingType] = multiplier
            }
        }

        return weaknesses
    }

    /**
     * Calculate combined defensive effectiveness for dual types
     */
    fun getDualTypeDefense(type1: String, type2: String?): Map<String, Double> {
        if (type2 == null) return getDefensiveWeaknesses(type1)

        val combined = mutableMapOf<String, Double>()

        allTypes.forEach { attackingType ->
            val mult1 = typeEffectiveness[attackingType]?.get(type1) ?: 1.0
            val mult2 = typeEffectiveness[attackingType]?.get(type2) ?: 1.0
            val total = mult1 * mult2

            if (total != 1.0) {
                combined[attackingType] = total
            }
        }

        return combined
    }

    /**
     * Get type color for UI
     */
    fun getTypeColor(type: String): Color {
        return when (type.lowercase()) {
            "normal" -> Color(0xFFA8A878)
            "fire" -> Color(0xFFF08030)
            "water" -> Color(0xFF6890F0)
            "electric" -> Color(0xFFF8D030)
            "grass" -> Color(0xFF78C850)
            "ice" -> Color(0xFF98D8D8)
            "fighting" -> Color(0xFFC03028)
            "poison" -> Color(0xFFA040A0)
            "ground" -> Color(0xFFE0C068)
            "flying" -> Color(0xFFA890F0)
            "psychic" -> Color(0xFFF85888)
            "bug" -> Color(0xFFA8B820)
            "rock" -> Color(0xFFB8A038)
            "ghost" -> Color(0xFF705898)
            "dragon" -> Color(0xFF7038F8)
            "dark" -> Color(0xFF705848)
            "steel" -> Color(0xFFB8B8D0)
            "fairy" -> Color(0xFFEE99AC)
            else -> Color(0xFF68A090)
        }
    }
}

/**
 * Team Analysis Data Classes
 */
data class TeamAnalysis(
    val coverageScore: Int, // 0-100
    val offensiveCoverage: Map<String, Int>, // Type -> number of team members that can hit it effectively
    val defensiveWeaknesses: Map<String, List<String>>, // Weakness type -> Pokemon names vulnerable
    val resistances: Map<String, List<String>>, // Resistance type -> Pokemon names that resist
    val recommendations: List<String>,
    val strengths: List<String>,
    val typeDiversity: TypeDiversity
)

data class TypeDiversity(
    val uniqueTypes: Int,
    val typeDistribution: Map<String, Int>,
    val hasDuplicates: Boolean,
    val missingCriticalTypes: List<String>
)

/**
 * Team Analyzer - Main analysis engine
 */
object TeamAnalyzer {

    fun analyzeTeam(team: List<TeamMemberWithTypes>): TeamAnalysis {
        if (team.isEmpty()) return emptyAnalysis()

        val offensiveCoverage = calculateOffensiveCoverage(team)
        val (weaknesses, resistances) = calculateDefensiveProfile(team)
        val typeDiversity = analyzeTypeDiversity(team)
        val coverageScore = calculateOverallScore(
            offensiveCoverage,
            weaknesses,
            resistances,
            typeDiversity,
            team.size
        )
        val recommendations =
            generateRecommendations(team, offensiveCoverage, weaknesses, typeDiversity)
        val strengths = identifyStrengths(team, offensiveCoverage, resistances)

        return TeamAnalysis(
            coverageScore = coverageScore,
            offensiveCoverage = offensiveCoverage,
            defensiveWeaknesses = weaknesses,
            resistances = resistances,
            recommendations = recommendations,
            strengths = strengths,
            typeDiversity = typeDiversity
        )
    }

    private fun emptyAnalysis() = TeamAnalysis(
        coverageScore = 0,
        offensiveCoverage = emptyMap(),
        defensiveWeaknesses = emptyMap(),
        resistances = emptyMap(),
        recommendations = listOf("Add Pokémon to your team to get analysis!"),
        strengths = emptyList(),
        typeDiversity = TypeDiversity(0, emptyMap(), false, emptyList())
    )

    private fun calculateOffensiveCoverage(team: List<TeamMemberWithTypes>): Map<String, Int> {
        return PokemonTypeData.allTypes.associateWith { defendingType ->
            team.count { pokemon ->
                pokemon.types.any { attackingType ->
                    (PokemonTypeData.typeEffectiveness[attackingType]?.get(defendingType)
                        ?: 1.0) >= 2.0
                }
            }
        }
    }

    private fun calculateDefensiveProfile(
        team: List<TeamMemberWithTypes>
    ): Pair<Map<String, List<String>>, Map<String, List<String>>> {
        val weaknesses = mutableMapOf<String, MutableList<String>>()
        val resistances = mutableMapOf<String, MutableList<String>>()

        team.forEach { pokemon ->
            val defense = PokemonTypeData.getDualTypeDefense(
                pokemon.types.getOrNull(0) ?: "normal",
                pokemon.types.getOrNull(1)
            )
            defense.forEach { (attackType, multiplier) ->
                when {
                    multiplier >= 2.0 -> weaknesses.getOrPut(attackType) { mutableListOf() }
                        .add(pokemon.name)

                    multiplier <= 0.5 -> resistances.getOrPut(attackType) { mutableListOf() }
                        .add(pokemon.name)
                }
            }
        }
        return Pair(weaknesses, resistances)
    }

    private fun analyzeTypeDiversity(team: List<TeamMemberWithTypes>): TypeDiversity {
        val allTeamTypes = team.flatMap { it.types }
        val uniqueTypes = allTeamTypes.toSet().size
        val typeDistribution = allTeamTypes.groupingBy { it }.eachCount()
        val criticalTypes = listOf("water", "fire", "grass", "electric", "fighting", "psychic")
        val missingCritical = criticalTypes.filter { it !in allTeamTypes }

        return TypeDiversity(
            uniqueTypes = uniqueTypes,
            typeDistribution = typeDistribution,
            hasDuplicates = allTeamTypes.size > uniqueTypes,
            missingCriticalTypes = missingCritical
        )
    }

    private fun calculateOverallScore(
        coverage: Map<String, Int>,
        weaknesses: Map<String, List<String>>,
        resistances: Map<String, List<String>>,
        diversity: TypeDiversity,
        teamSize: Int
    ): Int {
        if (teamSize == 0) return 0

        // ── COVERAGE (40 pts) ─────────────────────────────────────────────────
        // What fraction of all 18 types does the team cover super-effectively?
        val typesCovered = coverage.values.count { it > 0 }
        val coverageRatio = typesCovered / 18.0
        val avgDepth = coverage.values.average() / teamSize.toDouble()   // normalised by team size
        val coverageScore = ((coverageRatio * 25) + (avgDepth * 15)).coerceIn(0.0, 40.0).toInt()

        // ── DIVERSITY (30 pts) ────────────────────────────────────────────────
        // Max possible unique types for a team of N: N * 2 (dual types), capped at 18
        val maxPossibleTypes = minOf(teamSize * 2, 18)
        val diversityRatio = diversity.uniqueTypes.toDouble() / maxPossibleTypes
        val missingPenalty = diversity.missingCriticalTypes.size * 2.0
        val diversityScore = ((diversityRatio * 30) - missingPenalty).coerceIn(0.0, 30.0).toInt()

        // ── DEFENSE (30 pts) ──────────────────────────────────────────────────
        // Use ratios so small teams aren't punished for having fewer total Pokémon
        val weaknessRatio =
            weaknesses.size.toDouble() / 18.0     // fraction of types you're weak to
        val resistanceRatio = resistances.size.toDouble() / 18.0    // fraction you resist
        // Concentrated weaknesses (3+ members weak to same type) are severe
        val criticalWeaks =
            weaknesses.count { it.value.size >= (teamSize / 2.0).coerceAtLeast(2.0) }
        val defenseScore = (30 * (1 - weaknessRatio) + 10 * resistanceRatio - criticalWeaks * 5)
            .coerceIn(0.0, 30.0).toInt()

        return (coverageScore + diversityScore + defenseScore).coerceIn(0, 100)
    }

    private fun generateRecommendations(
        team: List<TeamMemberWithTypes>,
        coverage: Map<String, Int>,
        weaknesses: Map<String, List<String>>,
        diversity: TypeDiversity
    ): List<String> {
        val recs = mutableListOf<String>()

        // Worst weakness
        val worstWeak = weaknesses.maxByOrNull { it.value.size }
        if (worstWeak != null && worstWeak.value.size >= 2) {
            recs.add("${worstWeak.value.size} of your Pokémon are weak to ${worstWeak.key.replaceFirstChar { it.uppercase() }}. Add a ${worstWeak.key}-resistant Pokémon.")
        }

        // Uncovered types (no super-effective move)
        val uncovered = coverage.filter { it.value == 0 }.keys.take(3)
        if (uncovered.isNotEmpty()) {
            recs.add("No super-effective moves against: ${uncovered.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }}.")
        }

        // Duplicate types
        val heavyDuplicate = diversity.typeDistribution.filter { it.value >= 3 }.keys.firstOrNull()
        if (heavyDuplicate != null) {
            recs.add("Too many ${heavyDuplicate.replaceFirstChar { it.uppercase() }}-types. Replace one for better variety.")
        }

        // Missing critical types (only when team has room)
        if (diversity.missingCriticalTypes.isNotEmpty() && team.size < 6) {
            val missing = diversity.missingCriticalTypes.take(2)
                .joinToString(" or ") { it.replaceFirstChar { c -> c.uppercase() } }
            recs.add("Consider adding a $missing-type for broader coverage.")
        }

        return recs.ifEmpty { listOf("Great balance! Your team is well-rounded.") }
    }

    private fun identifyStrengths(
        team: List<TeamMemberWithTypes>,
        coverage: Map<String, Int>,
        resistances: Map<String, List<String>>
    ): List<String> {
        val strengths = mutableListOf<String>()

        val excellentTypes = coverage.filter { it.value >= maxOf(2, team.size / 2) }.keys.take(4)
        if (excellentTypes.isNotEmpty()) {
            strengths.add("Strong coverage against ${excellentTypes.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }}")
        }

        val teamResist =
            resistances.filter { it.value.size >= maxOf(2, team.size / 2) }.keys.take(3)
        if (teamResist.isNotEmpty()) {
            strengths.add("Solid resistance to ${teamResist.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }}")
        }

        val typeCount = team.flatMap { it.types }.toSet().size
        if (team.size in 3..typeCount) {
            strengths.add("Good type variety. no major overlaps")
        }

        return strengths
    }
}

/**
 * Data class for Pokemon with types (for analysis)
 */
data class TeamMemberWithTypes(
    val name: String,
    val types: List<String>,
    val imageUrl: String
)