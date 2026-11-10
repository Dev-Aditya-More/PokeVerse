package com.aditya1875.pokeverse.feature.game.wildcatch.domain.model

import kotlin.math.abs
import kotlin.random.Random

/**
 * How often each category of wild Pokémon turns up. The category is rolled FIRST, then a Pokémon of that
 * category is looked for, so "Common" really is the usual sight and a Legendary really is a rare event,
 * whatever the Pokédex happens to contain.
 *
 * Categories, by name in the app: Common, Uncommon, Rare, Ultra Rare ([CatchTier.EPIC]) and Legendary.
 */
object CatchSpawns {

    /** Share of encounters per category, in percent. Adds up to 100. */
    val WEIGHTS: Map<CatchTier, Int> = linkedMapOf(
        CatchTier.COMMON to 56,
        CatchTier.UNCOMMON to 28,
        CatchTier.RARE to 11,
        CatchTier.EPIC to 4,
        CatchTier.LEGENDARY to 1,
    )

    /** The first few encounters stay friendly: only Common and Uncommon. */
    const val FRIENDLY_ENCOUNTERS = 3

    /**
     * After a Legendary shows up, this many encounters pass before another can come up by chance (a Rare Lure
     * can still call one). A new run starts partway through this, so a Legendary is a rare event in a run, not
     * something to expect every few minutes.
     */
    const val LEGENDARY_COOLDOWN = 25
    /** How much of the cooldown a new run starts with already used. */
    const val LEGENDARY_START_HEAD = 10

    /** A Pokémon seen in this many of the latest encounters is not offered again, so you never meet the same one back to back. */
    const val RECENT_MEMORY = 12

    /**
     * Pokémon whose base stats put them in Ultra Rare (BST 580+) without being legendary. There are very few
     * in the first 809, too few to find reliably by random sampling, so they are looked up directly. Each one
     * is still checked against its real stats when it is loaded.
     */
    val ULTRA_RARE_SEEDS: List<Int> = listOf(149, 248, 289, 373, 376, 445, 635, 706, 784)

    /** The category of the next encounter. [encounterNumber] is how many have appeared so far this run. */
    fun rollTier(encounterNumber: Int, random: Random, legendaryAllowed: Boolean = true): CatchTier {
        val allowed = WEIGHTS.filterKeys { tier ->
            (encounterNumber >= FRIENDLY_ENCOUNTERS || tier == CatchTier.COMMON || tier == CatchTier.UNCOMMON) &&
                (legendaryAllowed || tier != CatchTier.LEGENDARY)
        }
        var roll = random.nextInt(allowed.values.sum())
        for ((tier, weight) in allowed) {
            if (roll < weight) return tier
            roll -= weight
        }
        return CatchTier.COMMON
    }

    /** How far apart two categories are; used to take the nearest one when the exact one can't be found. */
    fun distance(a: CatchTier, b: CatchTier): Int = abs(a.ordinal - b.ordinal)
}

/** The last few Pokémon ids shown, newest last, so the same one doesn't come round again straight away. */
class RecentEncounters(private val capacity: Int = CatchSpawns.RECENT_MEMORY) {
    private val ids = ArrayDeque<Int>()

    fun remember(id: Int) {
        ids.remove(id)
        ids.addLast(id)
        while (ids.size > capacity) ids.removeFirst()
    }

    operator fun contains(id: Int): Boolean = id in ids

    fun snapshot(): Set<Int> = ids.toSet()

    fun clear() = ids.clear()
}
