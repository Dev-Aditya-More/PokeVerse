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
    val description: String
)

/**
 * Gym badges are a small, fixed dataset (PokeAPI has no badges endpoint),
 * so they ship as bundled JSON — loaded once and cached for the process.
 */
class BadgeRepository(private val context: Context) {

    private var cached: List<GymBadge>? = null

    fun getBadges(): List<GymBadge> {
        cached?.let { return it }
        return try {
            val json = context.resources.openRawResource(R.raw.gym_badges)
                .bufferedReader().readText()
            val array = JSONArray(json)
            val list = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                GymBadge(
                    name = obj.getString("name"),
                    region = obj.getString("region"),
                    leader = obj.getString("leader"),
                    location = obj.getString("location"),
                    type = obj.getString("type"),
                    description = obj.getString("description")
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
}
