package com.aditya1875.pokeverse.feature.game.wildcatch.domain.model

enum class ThrowAccuracy(
    val label: String,
    val baseCatchChance: Float,
    val scoreBonus: Int
) {
    EXCELLENT("Excellent!", 0.93f, 100),
    GREAT("Great!", 0.62f, 60),
    GOOD("Good!", 0.30f, 30),
    // A missed ring is a missed throw: a sliver of luck, not a free pass.
    MISS("Missed...", 0.03f, 0)
}

/** Ring-timing windows (as a fraction of the shrinking ring, 0f = smallest). */
data class RingThresholds(
    val excellent: Float,
    val great: Float,
    val good: Float
)

/**
 * Wild Catch starts forgiving and tightens up as the run goes on, same idea
 * as the ring-shrink speed ramp, but applied to both the timing windows and
 * the underlying odds so a run-long "always throw PERFECT" habit stops being
 * a guaranteed catch. Both curves fully ramp by the 20th Pokémon; the speed ramp
 * (see WildCatchViewModel.speedFor) keeps tightening a little longer.
 */
object WildCatchDifficulty {
    private const val RAMP_LENGTH = 20
    private const val CATCH_CHANCE_FLOOR_MULTIPLIER = 0.6f

    private fun progress(pokemonCount: Int): Float =
        (pokemonCount.toFloat() / RAMP_LENGTH).coerceIn(0f, 1f)

    private fun lerp(start: Float, end: Float, t: Float) = start + (end - start) * t

    fun ringThresholds(pokemonCount: Int): RingThresholds {
        val t = progress(pokemonCount)
        return RingThresholds(
            excellent = lerp(0.15f, 0.07f, t),
            great = lerp(0.32f, 0.18f, t),
            good = lerp(0.58f, 0.38f, t)
        )
    }

    /** Multiplier applied to a tier's base catch chance, tapers toward a floor, never to zero. */
    fun catchChanceMultiplier(pokemonCount: Int): Float =
        lerp(1f, CATCH_CHANCE_FLOOR_MULTIPLIER, progress(pokemonCount))
}
