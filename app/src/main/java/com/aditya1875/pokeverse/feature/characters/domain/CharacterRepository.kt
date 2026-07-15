package com.aditya1875.pokeverse.feature.characters.domain

import android.content.Context
import com.aditya1875.pokeverse.R
import org.json.JSONArray

data class PokeCharacter(
    val name: String,
    val role: String,        // Professor / Champion / Gym Leader / Villain / Trainer / Rival
    val region: String,
    val signature: String,   // signature Pokémon or claim to fame
    val emoji: String,       // fallback avatar when the image is missing or fails to load
    val imageUrl: String,    // official trainer sprite (Pokémon Showdown CDN)
    val description: String
)

/**
 * Notable trainers/characters are a fixed dataset (no PokeAPI endpoint exists),
 * bundled as JSON — loaded once and cached for the process.
 */
class CharacterRepository(private val context: Context) {

    private var cached: List<PokeCharacter>? = null

    fun getCharacters(): List<PokeCharacter> {
        cached?.let { return it }
        return try {
            val json = context.resources.openRawResource(R.raw.characters)
                .bufferedReader().readText()
            val array = JSONArray(json)
            val list = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                PokeCharacter(
                    name = obj.getString("name"),
                    role = obj.getString("role"),
                    region = obj.getString("region"),
                    signature = obj.getString("signature"),
                    emoji = obj.getString("emoji"),
                    imageUrl = obj.optString("image", ""),
                    description = obj.getString("description")
                )
            }
            cached = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getRoles(): List<String> = getCharacters().map { it.role }.distinct()
}
