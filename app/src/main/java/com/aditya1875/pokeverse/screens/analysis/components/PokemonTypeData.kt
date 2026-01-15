package com.aditya1875.pokeverse.screens.analysis.components

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
    fun getTypeColor(type: String): androidx.compose.ui.graphics.Color {
        return when (type.lowercase()) {
            "normal" -> androidx.compose.ui.graphics.Color(0xFFA8A878)
            "fire" -> androidx.compose.ui.graphics.Color(0xFFF08030)
            "water" -> androidx.compose.ui.graphics.Color(0xFF6890F0)
            "electric" -> androidx.compose.ui.graphics.Color(0xFFF8D030)
            "grass" -> androidx.compose.ui.graphics.Color(0xFF78C850)
            "ice" -> androidx.compose.ui.graphics.Color(0xFF98D8D8)
            "fighting" -> androidx.compose.ui.graphics.Color(0xFFC03028)
            "poison" -> androidx.compose.ui.graphics.Color(0xFFA040A0)
            "ground" -> androidx.compose.ui.graphics.Color(0xFFE0C068)
            "flying" -> androidx.compose.ui.graphics.Color(0xFFA890F0)
            "psychic" -> androidx.compose.ui.graphics.Color(0xFFF85888)
            "bug" -> androidx.compose.ui.graphics.Color(0xFFA8B820)
            "rock" -> androidx.compose.ui.graphics.Color(0xFFB8A038)
            "ghost" -> androidx.compose.ui.graphics.Color(0xFF705898)
            "dragon" -> androidx.compose.ui.graphics.Color(0xFF7038F8)
            "dark" -> androidx.compose.ui.graphics.Color(0xFF705848)
            "steel" -> androidx.compose.ui.graphics.Color(0xFFB8B8D0)
            "fairy" -> androidx.compose.ui.graphics.Color(0xFFEE99AC)
            else -> androidx.compose.ui.graphics.Color(0xFF68A090)
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
        val offensiveCoverage = calculateOffensiveCoverage(team)
        val defensiveAnalysis = calculateDefensiveProfile(team)
        val typeDiversity = analyzeTypeDiversity(team)
        val coverageScore = calculateOverallScore(offensiveCoverage, defensiveAnalysis, typeDiversity)
        val recommendations = generateRecommendations(team, offensiveCoverage, defensiveAnalysis, typeDiversity)
        val strengths = identifyStrengths(team, offensiveCoverage, defensiveAnalysis)
        
        return TeamAnalysis(
            coverageScore = coverageScore,
            offensiveCoverage = offensiveCoverage,
            defensiveWeaknesses = defensiveAnalysis.first,
            resistances = defensiveAnalysis.second,
            recommendations = recommendations,
            strengths = strengths,
            typeDiversity = typeDiversity
        )
    }
    
    private fun calculateOffensiveCoverage(team: List<TeamMemberWithTypes>): Map<String, Int> {
        val coverage = mutableMapOf<String, Int>()
        
        // For each defending type, count how many team members can hit it super effectively
        PokemonTypeData.allTypes.forEach { defendingType ->
            var hitCount = 0
            
            team.forEach { pokemon ->
                // Check if this Pokemon's types can hit the defending type super effectively
                pokemon.types.forEach { attackingType ->
                    val effectiveness = PokemonTypeData.typeEffectiveness[attackingType]?.get(defendingType) ?: 1.0
                    if (effectiveness >= 2.0) {
                        hitCount++
                        return@forEach // Count each Pokemon only once
                    }
                }
            }
            
            coverage[defendingType] = hitCount
        }
        
        return coverage
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
                    multiplier >= 2.0 -> {
                        weaknesses.getOrPut(attackType) { mutableListOf() }.add(pokemon.name)
                    }
                    multiplier <= 0.5 -> {
                        resistances.getOrPut(attackType) { mutableListOf() }.add(pokemon.name)
                    }
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
        defense: Pair<Map<String, List<String>>, Map<String, List<String>>>,
        diversity: TypeDiversity
    ): Int {
        // Coverage score (40 points): Average coverage across all types
        val avgCoverage = coverage.values.average()
        val coveragePoints = ((avgCoverage / 6.0) * 40).coerceIn(0.0, 40.0).toInt()
        
        // Diversity score (30 points)
        val diversityPoints = ((diversity.uniqueTypes / 18.0) * 30).coerceIn(0.0, 30.0).toInt()
        
        // Defense score (30 points): Fewer weaknesses, more resistances
        val weaknessCount = defense.first.values.sumOf { it.size }
        val resistanceCount = defense.second.values.sumOf { it.size }
        val defensePoints = (30 - (weaknessCount * 2) + resistanceCount).coerceIn(0, 30)
        
        return (coveragePoints + diversityPoints + defensePoints).coerceIn(0, 100)
    }
    
    private fun generateRecommendations(
        team: List<TeamMemberWithTypes>,
        coverage: Map<String, Int>,
        defense: Pair<Map<String, List<String>>, Map<String, List<String>>>,
        diversity: TypeDiversity
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Check for poor coverage
        val poorCoverage = coverage.filter { it.value == 0 }
        if (poorCoverage.isNotEmpty()) {
            val types = poorCoverage.keys.take(3).joinToString(", ")
            recommendations.add("⚠️ No coverage against: $types. Consider adding Pokemon with these attack types.")
        }
        
        // Check for common weaknesses
        val criticalWeaknesses = defense.first.filter { it.value.size >= 3 }
        if (criticalWeaknesses.isNotEmpty()) {
            val worst = criticalWeaknesses.maxBy { it.value.size }
            recommendations.add("🛡️ ${worst.value.size} Pokemon are weak to ${worst.key}. Consider adding a ${worst.key}-resistant Pokemon.")
        }
        
        // Check type diversity
        if (diversity.uniqueTypes < 6 && team.size >= 4) {
            recommendations.add("🎨 Low type diversity (${diversity.uniqueTypes} types). Add different types for better balance.")
        }
        
        // Check for duplicate types
        val duplicates = diversity.typeDistribution.filter { it.value > 2 }
        if (duplicates.isNotEmpty()) {
            val type = duplicates.keys.first()
            recommendations.add("⚡ Too many ${type}-type Pokemon (${duplicates[type]}). Consider more variety.")
        }
        
        // Suggest missing critical types
        if (diversity.missingCriticalTypes.isNotEmpty() && team.size < 6) {
            val missing = diversity.missingCriticalTypes.take(2).joinToString(" or ")
            recommendations.add("💡 Consider adding a $missing-type Pokemon for better coverage.")
        }
        
        return recommendations.ifEmpty { listOf("✅ Your team has good balance!") }
    }
    
    private fun identifyStrengths(
        team: List<TeamMemberWithTypes>,
        coverage: Map<String, Int>,
        defense: Pair<Map<String, List<String>>, Map<String, List<String>>>
    ): List<String> {
        val strengths = mutableListOf<String>()
        
        // Strong coverage
        val excellentCoverage = coverage.filter { it.value >= 3 }
        if (excellentCoverage.isNotEmpty()) {
            val types = excellentCoverage.keys.take(3).joinToString(", ")
            strengths.add("💪 Excellent coverage against: $types")
        }
        
        // Strong resistances
        val strongResistances = defense.second.filter { it.value.size >= 3 }
        if (strongResistances.isNotEmpty()) {
            val type = strongResistances.keys.first()
            strengths.add("🛡️ Strong ${type}-type resistance across team")
        }
        
        // Balanced team
        if (team.size >= 5 && coverage.values.average() >= 2.0) {
            strengths.add("⚖️ Well-balanced offensive coverage")
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