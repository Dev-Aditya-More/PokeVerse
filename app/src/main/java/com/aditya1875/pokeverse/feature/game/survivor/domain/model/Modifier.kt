package com.aditya1875.pokeverse.feature.game.survivor.domain.model

/**
 * A pluggable rule that adjusts a round's per-type effectiveness map before the
 * player answers. Each modifier is a pure transform — [SurvivorRuleEngine] folds
 * the active list over the base type-chart result.
 *
 * v1 ships only [Weather]. Ability/held-item/status/critical-hit modifiers are
 * deliberately not stubbed here — each is a self-contained follow-up once Weather
 * is validated, not dead scaffolding now.
 */
sealed interface Modifier {

    /** Human-readable label for the round's modifier callout (e.g. "Rain"). */
    val label: String

    fun apply(effectiveness: Map<String, Float>): Map<String, Float>

    data class Weather(val kind: WeatherKind) : Modifier {
        override val label: String get() = kind.label

        override fun apply(effectiveness: Map<String, Float>): Map<String, Float> =
            effectiveness.mapValues { (type, multiplier) ->
                multiplier * (kind.typeMultipliers[type] ?: 1f)
            }
    }
}

enum class WeatherKind(val label: String, val typeMultipliers: Map<String, Float>) {
    RAIN("Rain", mapOf("water" to 1.5f, "fire" to 0.5f)),
    SUN("Harsh Sunlight", mapOf("fire" to 1.5f, "water" to 0.5f))
}
