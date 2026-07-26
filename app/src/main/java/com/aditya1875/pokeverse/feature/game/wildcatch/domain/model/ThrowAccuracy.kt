package com.aditya1875.pokeverse.feature.game.wildcatch.domain.model

enum class ThrowAccuracy(
    val label: String,
    val baseCatchChance: Float,
    val scoreBonus: Int
) {
    PERFECT("Perfect!", 0.97f, 100),
    GREAT("Great!", 0.72f, 60),
    NICE("Nice!", 0.42f, 30),
    MISS("Missed...", 0.08f, 0)
}

/** Ring-timing windows (as a fraction of the shrinking ring, 0f = smallest). */
data class RingThresholds(
    val perfect: Float,
    val great: Float,
    val nice: Float
)

/**
 * Wild Catch starts forgiving and tightens up as the run goes on — same idea
 * as the ring-shrink speed ramp, but applied to both the timing windows and
 * the underlying odds so a run-long "always throw PERFECT" habit stops being
 * a guaranteed catch. Both curves fully ramp by the 30th Pokémon, matching
 * where the speed ramp (see WildCatchViewModel.speedFor) also plateaus.
 */
object WildCatchDifficulty {
    private const val RAMP_LENGTH = 30
    private const val CATCH_CHANCE_FLOOR_MULTIPLIER = 0.6f

    private fun progress(pokemonCount: Int): Float =
        (pokemonCount.toFloat() / RAMP_LENGTH).coerceIn(0f, 1f)

    private fun lerp(start: Float, end: Float, t: Float) = start + (end - start) * t

    fun ringThresholds(pokemonCount: Int): RingThresholds {
        val t = progress(pokemonCount)
        return RingThresholds(
            perfect = lerp(0.20f, 0.10f, t),
            great = lerp(0.40f, 0.25f, t),
            nice = lerp(0.70f, 0.50f, t)
        )
    }

    /** Multiplier applied to a tier's base catch chance — tapers toward a floor, never to zero. */
    fun catchChanceMultiplier(pokemonCount: Int): Float =
        lerp(1f, CATCH_CHANCE_FLOOR_MULTIPLIER, progress(pokemonCount))
}
