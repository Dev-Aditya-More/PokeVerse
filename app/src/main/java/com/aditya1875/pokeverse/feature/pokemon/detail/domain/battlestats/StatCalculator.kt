package com.aditya1875.pokeverse.feature.pokemon.detail.domain.battlestats

const val MAX_IV = 31
const val MAX_EV_PER_STAT = 252
const val MAX_EV_TOTAL = 510

/**
 * Standard mainline-game stat formulas (Gen III onward). Pure math over the
 * base stats PokeAPI already provides — no external dataset needed.
 */
object StatCalculator {

    fun calculateHp(base: Int, iv: Int, ev: Int, level: Int): Int {
        // Shedinja is the one species whose HP is always fixed at 1
        if (base <= 1) return 1
        return ((2 * base + iv + ev / 4) * level) / 100 + level + 10
    }

    fun calculateStat(base: Int, iv: Int, ev: Int, level: Int, natureMultiplier: Float): Int {
        val raw = ((2 * base + iv + ev / 4) * level) / 100 + 5
        return (raw * natureMultiplier).toInt()
    }
}
