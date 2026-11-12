package com.aditya1875.pokeverse.utils

import android.content.Context
import com.google.gson.JsonParser

/**
 * Every Pokémon's name and types (#1-1025), bundled so screens can search and filter without a request per
 * Pokémon. Generated once from PokeAPI into assets/pokemon_index.json: {"6":{"n":"charizard","t":["fire","flying"]}}.
 */
object PokemonIndex {

    data class Entry(val name: String, val types: List<String>)

    @Volatile
    private var cache: Map<Int, Entry>? = null

    /** The whole index, read once. Empty (never throws) if the asset is missing or damaged. */
    fun get(context: Context): Map<Int, Entry> =
        cache ?: synchronized(this) {
            cache ?: load(context).also { cache = it }
        }

    private fun load(context: Context): Map<Int, Entry> =
        runCatching {
            context.assets.open("pokemon_index.json").bufferedReader().use { parse(it.readText()) }
        }.getOrDefault(emptyMap())

    /** Pure so it can be tested: bad entries are skipped rather than failing the whole table. */
    internal fun parse(json: String): Map<Int, Entry> {
        val root = JsonParser.parseString(json).asJsonObject
        val out = HashMap<Int, Entry>(root.size())
        for ((key, value) in root.entrySet()) {
            val id = key.toIntOrNull() ?: continue
            val obj = runCatching { value.asJsonObject }.getOrNull() ?: continue
            val name = obj.get("n")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
            val types = obj.getAsJsonArray("t")?.mapNotNull { runCatching { it.asString }.getOrNull() }.orEmpty()
            out[id] = Entry(name, types)
        }
        return out
    }
}
