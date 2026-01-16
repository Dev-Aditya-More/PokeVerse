package com.aditya1875.pokeverse.utils

import com.aditya1875.pokeverse.data.remote.model.PokemonResult

object PokemonSearchHelper {
    fun normalizeForSearch(name: String): String {
        return name.lowercase().replace("-", " ").trim()
    }

    fun getBaseName(name: String): String {
        return name.lowercase().split("-").firstOrNull() ?: name
    }

    fun getFormLabel(name: String): String? {
        val parts = name.lowercase().split("-")
        if (parts.size < 2) return null

        val formPart = parts.drop(1).joinToString("-")

        return when {
            formPart == "10" -> "10% Form"
            formPart == "50" -> "50% Form"
            formPart == "complete" -> "Complete Form"
            formPart == "male" -> "Male"
            formPart == "female" -> "Female"
            formPart.contains("mega") -> "Mega Evolution"
            formPart.contains("alola") -> "Alolan Form"
            formPart.contains("galar") -> "Galarian Form"
            formPart.contains("hisui") -> "Hisuian Form"
            else -> formPart.split("-").joinToString(" ") {
                it.replaceFirstChar { char -> char.uppercase() }
            }
        }
    }

    fun calculateRelevanceScore(pokemonName: String, query: String): Int {
        val cleanQuery = query.lowercase().trim()
        val cleanName = pokemonName.lowercase()
        val baseName = getBaseName(pokemonName)

        var score = 0

        if (cleanName == cleanQuery) score += 1000
        if (baseName == cleanQuery) score += 900
        if (cleanName.startsWith(cleanQuery)) score += 500
        if (baseName.startsWith(cleanQuery)) score += 450
        if (cleanName.contains(cleanQuery)) score += 300
        if (baseName.contains(cleanQuery)) score += 250

        score -= pokemonName.length

        return score
    }

    fun searchPokemon(
        pokemonList: List<PokemonResult>,
        query: String,
        limit: Int = 8
    ): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val cleanQuery = query.lowercase().trim()

        return pokemonList
            .asSequence()
            .map { pokemon ->
                SearchResult(
                    pokemon = pokemon,
                    score = calculateRelevanceScore(pokemon.name, cleanQuery),
                    baseName = getBaseName(pokemon.name),
                    formLabel = getFormLabel(pokemon.name)
                )
            }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
    }
}

data class SearchResult(
    val pokemon: PokemonResult,
    val score: Int,
    val baseName: String,
    val formLabel: String?
)