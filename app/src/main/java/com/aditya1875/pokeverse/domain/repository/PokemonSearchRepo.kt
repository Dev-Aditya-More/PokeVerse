package com.aditya1875.pokeverse.domain.repository

import android.util.Log
import com.aditya1875.pokeverse.data.remote.PokeApi
import com.aditya1875.pokeverse.data.remote.model.PokemonResult
import com.aditya1875.pokeverse.utils.PokemonSearchHelper
import com.aditya1875.pokeverse.utils.SearchResult

class PokemonSearchRepository(private val api: PokeApi) {

    private var allPokemonCache: List<PokemonResult>? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes

    suspend fun getAllPokemonNames(): List<PokemonResult> {
        val currentTime = System.currentTimeMillis()

        if (allPokemonCache != null && (currentTime - cacheTimestamp) < CACHE_DURATION) {
            return allPokemonCache!!
        }

        return try {
            val response = api.getPokemonList(limit = 100000, offset = 0)
            allPokemonCache = response.results
            cacheTimestamp = currentTime
            response.results
        } catch (e: Exception) {
            Log.e("SearchRepo", "Failed to fetch all Pokemon", e)
            allPokemonCache ?: emptyList()
        }
    }

    suspend fun searchPokemon(query: String, limit: Int = 10): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val allPokemon = getAllPokemonNames()
        return PokemonSearchHelper.searchPokemon(allPokemon, query, limit)
    }
}