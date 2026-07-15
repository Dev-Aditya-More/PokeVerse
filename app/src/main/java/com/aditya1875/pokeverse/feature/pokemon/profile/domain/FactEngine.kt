package com.aditya1875.pokeverse.feature.pokemon.profile.domain

import android.content.Context
import com.aditya1875.pokeverse.R
import org.json.JSONArray
import java.util.Calendar
import java.util.Random

data class PokemonFact(val fact: String, val category: String)

object FactEngine {

    private var cachedFacts: List<PokemonFact>? = null

    fun todaysFact(context: Context): PokemonFact {
        val facts = loadFacts(context)
        if (facts.isEmpty()) return fallback

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        // Shuffle indices deterministically for this year.
        // Different seed every year → different order every year.
        // Same day within a year → same fact for everyone on that day.
        val indices = (facts.indices).toMutableList()
        val rng = Random(year.toLong() * 73_856_093L xor 0x9e3779b9L)
        for (i in indices.lastIndex downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = indices[i]; indices[i] = indices[j]; indices[j] = tmp
        }

        // Wrap with modulo — no repeat for first facts.size days of the year
        val factIndex = indices[(dayOfYear - 1) % facts.size]
        return facts[factIndex]
    }

    private fun loadFacts(context: Context): List<PokemonFact> {
        cachedFacts?.let { return it }
        return try {
            val json = context.resources.openRawResource(R.raw.pokemon_facts).bufferedReader().readText()
            val array = JSONArray(json)
            val list = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                PokemonFact(fact = obj.getString("fact"), category = obj.getString("category"))
            }
            cachedFacts = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private val fallback = PokemonFact(
        fact = "Pikachu's name comes from 'pikapika' (sparkling) and 'chuu' (mouse sound in Japanese).",
        category = "Trivia"
    )
}
