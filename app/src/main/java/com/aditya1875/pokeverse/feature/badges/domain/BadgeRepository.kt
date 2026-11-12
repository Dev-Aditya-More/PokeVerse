package com.aditya1875.pokeverse.feature.badges.domain

import android.content.Context
import com.aditya1875.pokeverse.R
import org.json.JSONArray

data class GymBadge(
    val name: String,
    val region: String,
    val leader: String,
    val location: String,
    val type: String,
    val description: String,
    /** Official badge artwork from the PokeAPI sprites repo; empty if the badge has none. */
    val spriteUrl: String = ""
)

/**
 * Gym badges are a small, fixed dataset (PokeAPI has no badges endpoint),
 * so they ship as bundled JSON, loaded once and cached for the process.
 */
class BadgeRepository(private val context: Context) {

    private var cached: List<GymBadge>? = null

    fun getBadges(): List<GymBadge> {
        cached?.let { return it }
        return try {
            val json = context.resources.openRawResource(R.raw.gym_badges)
                .bufferedReader().readText()
            val array = JSONArray(json)
            val seen = HashMap<String, Int>()
            val list = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val region = obj.getString("region")
                val indexInRegion = seen.merge(region, 1, Int::plus)!! - 1
                val spriteId = SPRITE_IDS[region]?.getOrNull(indexInRegion)
                GymBadge(
                    name = obj.getString("name"),
                    region = obj.getString("region"),
                    leader = obj.getString("leader"),
                    location = obj.getString("location"),
                    type = obj.getString("type"),
                    description = obj.getString("description"),
                    spriteUrl = spriteId?.let { "$SPRITE_BASE$it.png" }.orEmpty()
                )
            }
            cached = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Regions in canonical game order (JSON is already ordered) */
    fun getRegions(): List<String> = getBadges().map { it.region }.distinct()

    private companion object {
        const val SPRITE_BASE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/badges/"

        // PokeAPI numbers badges by region in release order; Unova and Galar skip the B2W2-only /
        // extra badges that aren't in gym_badges.json. Matched against the artwork by eye.
        val SPRITE_IDS: Map<String, List<Int>> = mapOf(
            "Kanto" to (1..8).toList(),
            "Johto" to (9..16).toList(),
            "Hoenn" to (17..24).toList(),
            "Sinnoh" to (25..32).toList(),
            "Unova" to listOf(33, 34, 36, 37, 38, 39, 40, 41),
            "Kalos" to (43..50).toList(),
            "Galar" to listOf(51, 52, 53, 54, 56, 57, 59, 60),
            "Paldea" to (70..77).toList(),
        )
    }
}
