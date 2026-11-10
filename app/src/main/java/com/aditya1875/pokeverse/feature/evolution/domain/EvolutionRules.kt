package com.aditya1875.pokeverse.feature.evolution.domain

import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.Chain
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolvesTo
import com.aditya1875.pokeverse.feature.progression.domain.ShopItems

/**
 * Evolution: turn a Pokémon on your roster into its next form. The old form is used up (it leaves the
 * roster; the Pokédex entry stays), and the new form takes its place. Evolutions that need a stone or a
 * held item cost that item, bought with Poké Coins; the rest cost Evo Shards. Shards come from the two
 * games that use your Pokémon: catching them in Wild Catch and winning with them in Team Battle.
 *
 * Pure Kotlin so every rule is unit-tested. Shards never touch XP, level or rank.
 */
object EvolutionEconomy {

    /** Cost to reach a form that is [depth] steps from the first stage (1 = first evolution). */
    fun cost(depth: Int): Int = when {
        depth <= 1 -> 25
        depth == 2 -> 60
        else -> 90
    }

    /**
     * Shards for catching a Pokémon. Meeting one you already have pays the most (5), since that is the
     * duplicate you can turn into progress; a brand-new entry pays 3 (its reward is the Pokédex entry
     * itself). A clean throw and a tougher Pokémon add a little on top.
     */
    fun forCatch(accuracy: ThrowAccuracy, newEntry: Boolean, tier: CatchTier): Int {
        val throwBonus = when (accuracy) {
            ThrowAccuracy.EXCELLENT -> 3
            ThrowAccuracy.GREAT -> 1
            else -> 0
        }
        val tierBonus = when (tier) {
            CatchTier.COMMON, CatchTier.UNCOMMON -> 0
            CatchTier.RARE -> 1
            CatchTier.EPIC -> 2
            CatchTier.LEGENDARY -> 4
        }
        return (if (newEntry) NEW_ENTRY_SHARDS else REPEAT_SHARDS) + throwBonus + tierBonus
    }

    /** Shards for finishing a Team Battle; a loss still pays a little. */
    fun forBattle(won: Boolean): Int = if (won) 8 else 2

    /** Shards for finishing an online Team Battle against a real trainer; worth more than a bot, win or lose. */
    fun forOnlineBattle(won: Boolean): Int = if (won) 14 else 4

    const val REPEAT_SHARDS = 5
    const val NEW_ENTRY_SHARDS = 3

    /** The most shards a trainer can earn in one day, so evolving stays something you work towards. */
    const val DAILY_CAP = 80

    /** How much of [amount] still fits under today's cap after [earnedToday] has already been earned. */
    fun withinCap(earnedToday: Int, amount: Int, cap: Int = DAILY_CAP): Int =
        (cap - earnedToday.coerceAtLeast(0)).coerceIn(0, amount.coerceAtLeast(0))
}

/** One thing a caught Pokémon can evolve into. */
data class EvolutionOption(
    val fromId: Int,
    val toId: Int,
    val toName: String,
    val depth: Int,
    val cost: Int,
)

/**
 * One evolution a Pokémon can take. [itemId] is the shop item it needs (a stone or a held item), or null
 * when it evolves by Evo Shards alone (level, friendship, trade without an item...).
 */
data class EvolutionStep(val name: String, val depth: Int, val itemId: String?)

object EvolutionPaths {

    /**
     * The forms [speciesName] evolves into directly, with what each one needs. Handles branching lines
     * (one Pokémon with several evolutions) and returns nothing for a final form.
     */
    fun steps(root: Chain, speciesName: String): List<EvolutionStep> {
        fun stepsOf(children: List<EvolvesTo>, depth: Int) =
            children.map { EvolutionStep(it.species.name, depth, requiredItem(it)) }

        fun find(node: EvolvesTo, depth: Int): List<EvolutionStep>? {
            if (node.species.name == speciesName) return stepsOf(node.evolves_to, depth + 1)
            for (child in node.evolves_to) find(child, depth + 1)?.let { return it }
            return null
        }
        if (root.species.name == speciesName) return stepsOf(root.evolves_to, 1)
        for (child in root.evolves_to) find(child, 1)?.let { return it }
        return emptyList()
    }

    /** The forms [speciesName] evolves into directly, as (species name, depth of that form). */
    fun next(root: Chain, speciesName: String): List<Pair<String, Int>> =
        steps(root, speciesName).map { it.name to it.depth }

    /**
     * The shop item an evolution into [node] needs: the stone used on it, else the item it must hold.
     * Only items the shop actually sells count, so an evolution we can't supply an item for falls back
     * to shards instead of becoming impossible. Null means shards.
     */
    private fun requiredItem(node: EvolvesTo): String? {
        for (detail in node.evolution_details) {
            ShopItems.forEvolution(detail.item?.name)?.let { return it.id }
            ShopItems.forEvolution(detail.held_item?.name)?.let { return it.id }
        }
        return null
    }
}

object EvolutionRules {
    enum class Check { OK, NOT_CAUGHT, ALREADY_HAVE, NOT_ENOUGH }

    /**
     * [fromOwned] is whether the Pokémon is on the trainer's roster right now; [toOwned] whether the form
     * it would become is already there. An evolution that needs an item pays no shards (cost 0).
     */
    fun check(balance: Int, cost: Int, fromCaught: Boolean, toCaught: Boolean): Check = when {
        !fromCaught -> Check.NOT_CAUGHT
        toCaught -> Check.ALREADY_HAVE
        balance < cost -> Check.NOT_ENOUGH
        else -> Check.OK
    }
}
