package com.aditya1875.pokeverse.feature.game.wildcatch.domain.model

import com.aditya1875.pokeverse.feature.progression.domain.ShopItems

/**
 * The ball a trainer throws. A plain Poké Ball is free and unlimited; the others are bought with coins
 * and used up. Poké Ball < Great Ball < Ultra Ball < Master Ball, always: a better ball is better at every
 * throw quality and against every category. [strength] is how many "tries" the ball is worth (see
 * [CatchOdds]), and a better ball also softens the penalty for catching a tough category, which is what
 * makes legendaries and ultra rares reachable without lowering the skill needed to time a throw.
 */
enum class BallType(
    /** Inventory id in the shop, or null for the free Poké Ball. */
    val itemId: String?,
    val strength: Float,
    /** Share (0..1) of a tier's catch penalty this ball cancels. */
    val tierRelief: Float,
    /** The best any throw can do with this ball. Each ball's ceiling is above the last, so the order holds even on an easy catch. */
    val ceiling: Float,
    /** A Master Ball never fails, whatever the throw. */
    val guaranteed: Boolean = false,
) {
    POKE(null, 1.0f, 0f, 0.95f),
    GREAT(ShopItems.ID_GREAT_BALL, 1.8f, 0.25f, 0.975f),
    ULTRA(ShopItems.ID_ULTRA_BALL, 2.8f, 0.5f, 0.99f),
    MASTER(ShopItems.ID_MASTER_BALL, 1.0f, 1f, 1f, guaranteed = true);
}

/** The whole catch roll, in one pure place so it can be tested. */
object CatchOdds {

    /** A thrown ball is never a sure thing unless it is a Master Ball. */
    const val MAX_CHANCE = 0.99f

    fun chance(accuracy: ThrowAccuracy, pokemonCount: Int, tier: CatchTier, ball: BallType): Float {
        if (ball.guaranteed) return 1f
        val tierFactor = tier.catchFactor + (1f - tier.catchFactor) * ball.tierRelief
        // What one plain try is worth, from the throw, how deep into the run it is and how tough the category is.
        val single = (accuracy.baseCatchChance *
            WildCatchDifficulty.catchChanceMultiplier(pokemonCount) *
            tierFactor).coerceIn(0f, 1f)
        // A stronger ball is worth more tries: 1 - (1 - p)^strength. It rises with every ball and never
        // saturates early the way a flat multiplier does, so Great < Ultra holds even on an easy catch.
        val chance = 1f - Math.pow((1f - single).toDouble(), ball.strength.toDouble()).toFloat()
        return chance.coerceIn(0f, ball.ceiling)
    }
}
