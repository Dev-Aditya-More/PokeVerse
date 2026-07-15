package com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats

import android.content.Context
import com.aditya1875.pokeverse.R
import org.json.JSONArray
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

data class GoBaseStats(val id: Int, val atk: Int, val def: Int, val sta: Int)

const val GO_MAX_IV = 15
const val GO_MAX_LEVEL = 45f

/**
 * Pokémon GO uses its own base Attack/Defense/Stamina per species — distinct
 * from mainline base stats — plus a per-level CP Multiplier (CPM). Neither is
 * derivable from PokeAPI, so both are bundled from community-verified game
 * data (sourced via pogoapi.net, matching in-game values).
 */
class GoStatsRepository(private val context: Context) {

    private var baseStatsCache: Map<Int, GoBaseStats>? = null
    private var cpmCache: List<Pair<Float, Float>>? = null // (level, multiplier), sorted by level

    fun getBaseStats(pokemonId: Int): GoBaseStats? = loadBaseStats()[pokemonId]

    /** All levels this dataset supports, e.g. 1f, 1.5f, 2f, … up to [GO_MAX_LEVEL] */
    fun availableLevels(): List<Float> = loadCpm().map { it.first }

    fun calculateCp(base: GoBaseStats, level: Float, ivAtk: Int, ivDef: Int, ivSta: Int): Int {
        val cpm = cpmFor(level)
        val atk = (base.atk + ivAtk).toDouble()
        val def = (base.def + ivDef).toDouble()
        val sta = (base.sta + ivSta).toDouble()
        val cp = floor(atk * sqrt(def) * sqrt(sta) * cpm.toDouble().pow(2) / 10.0)
        return max(cp.toInt(), 10)
    }

    fun calculateHp(base: GoBaseStats, level: Float, ivSta: Int): Int {
        val cpm = cpmFor(level)
        return max(floor((base.sta + ivSta) * cpm).toInt(), 10)
    }

    private fun cpmFor(level: Float): Float {
        val table = loadCpm()
        return table.minByOrNull { kotlin.math.abs(it.first - level) }?.second ?: table.last().second
    }

    private fun loadBaseStats(): Map<Int, GoBaseStats> {
        baseStatsCache?.let { return it }
        return try {
            val json = context.resources.openRawResource(R.raw.go_base_stats)
                .bufferedReader().readText()
            val array = JSONArray(json)
            val map = (0 until array.length()).associate { i ->
                val obj = array.getJSONObject(i)
                val id = obj.getInt("id")
                id to GoBaseStats(
                    id = id,
                    atk = obj.getInt("atk"),
                    def = obj.getInt("def"),
                    sta = obj.getInt("sta")
                )
            }
            baseStatsCache = map
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun loadCpm(): List<Pair<Float, Float>> {
        cpmCache?.let { return it }
        return try {
            val json = context.resources.openRawResource(R.raw.go_cp_multipliers)
                .bufferedReader().readText()
            val array = JSONArray(json)
            val list = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                obj.getDouble("lvl").toFloat() to obj.getDouble("cpm").toFloat()
            }.sortedBy { it.first }
            cpmCache = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
