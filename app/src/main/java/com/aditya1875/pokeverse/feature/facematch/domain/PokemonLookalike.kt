package com.aditya1875.pokeverse.feature.facematch.domain

/** Plain 0f..1f RGB triple — kept independent of any Android/Compose Color type so this
 * package has no UI-layer dependency and stays trivially unit-testable. */
data class RgbColor(val r: Float, val g: Float, val b: Float)

enum class FaceShape { ROUND, OVAL, LONG }

data class PokemonLookalike(
    val id: Int,
    val displayName: String,
    val representativeColor: RgbColor,
    val shape: FaceShape,
    val typicalSmiling: Float = 0.5f,
    val typicalEyeOpen: Float = 0.8f,
    val blurb: String
) {
    val spriteUrl: String =
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
}
