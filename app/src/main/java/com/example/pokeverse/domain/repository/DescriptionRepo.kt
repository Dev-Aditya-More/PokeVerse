package com.example.pokeverse.domain.repository

import android.content.Context
import com.example.pokeverse.data.remote.model.PokemonDescription
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DescriptionRepo(private val context: Context) {

    private val descriptionMap: Map<Int, String> by lazy {
        loadDescriptionsFromAssets()
    }

    private fun loadDescriptionsFromAssets(): Map<Int, String> {
        val json = context.assets.open("simplified_pokedex_descriptions.json")
            .bufferedReader().use { it.readText() }

        val gson = Gson()
        val type = object : TypeToken<List<PokemonDescription>>(){}.type
        val list: List<PokemonDescription> = gson.fromJson(json, type)

        return list.associateBy({ it.id }, { it.description })
    }

    fun getDescriptionById(id: Int): String {
        return descriptionMap[id] ?: "Description not available."
    }
}
