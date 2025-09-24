package com.example.pokeverse.specialscreens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

enum class ParticleType {
    FIRE, WATER, GRASS, ROCK, ELECTRIC, FLYING, ICE, GROUND, POISON, BUG, GHOST, NONE
}
enum class SpecialEffect {
    BLUE_FLAMES, DARK_AURA, NONE
}

fun getSpecialEffectFor(pokemonName: String): SpecialEffect {
    return when {
        pokemonName.equals("Charizard-mega-x", ignoreCase = true) -> SpecialEffect.BLUE_FLAMES
        pokemonName.equals("gengar", ignoreCase = true) -> SpecialEffect.DARK_AURA
        else -> SpecialEffect.NONE
    }
}

fun getParticleTypeFor(types: List<String>): ParticleType {
    if (types.isEmpty()) return ParticleType.NONE

    return when (types[0].lowercase()) {
        "fire" -> ParticleType.FIRE
        "water" -> ParticleType.WATER
        "grass" -> ParticleType.GRASS
        "rock" -> ParticleType.ROCK
        "electric" -> ParticleType.ELECTRIC
        "flying" -> ParticleType.FLYING
        "ice" -> ParticleType.ICE
        "ground" -> ParticleType.GROUND
        "poison" -> ParticleType.POISON
        "bug" -> ParticleType.BUG
        "ghost" -> ParticleType.GHOST
        else -> ParticleType.NONE
    }
}


@Composable
fun ParticleBackground(
    types: ParticleType,
    pokemonName: String,
) {
    val special = getSpecialEffectFor(pokemonName)
    val type = getParticleTypeFor(listOf(types.toString()))

    Box(modifier = Modifier.fillMaxSize()) {
        when (special) {
            SpecialEffect.BLUE_FLAMES -> BlueFlameParticles()
            SpecialEffect.DARK_AURA -> DarkAuraParticles()
            SpecialEffect.NONE -> when (type) {
                ParticleType.FIRE -> EmberParticles()
                ParticleType.WATER -> BubbleParticles()
                ParticleType.GRASS -> LeafParticles()
                ParticleType.ROCK -> RockParticleBackground()
                ParticleType.ELECTRIC -> ElectricParticles()
                ParticleType.FLYING -> FlyingParticles()
                ParticleType.ICE -> IceParticles()
                ParticleType.GROUND -> GroundParticles()
                ParticleType.POISON -> PoisonParticles()
                ParticleType.BUG -> BugParticles()
                ParticleType.GHOST -> GhostParticles()
                ParticleType.NONE -> {}
            }
        }
    }
}


@Composable
fun DarkAuraParticles() {
    TODO("Not yet implemented")
}

@Composable
fun BlueFlameParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 70
) {
    val charizardXFlames = listOf(
        Color(0xFF0D47A1), // Dark Indigo
        Color(0xFF1976D2), // Strong Blue
        Color(0xFF00ACC1), // Teal/Cyan Fire
        Color(0xFF82B1FF), // Soft Glow Blue
        Color(0xFF1565C0)  // Deep Royal Blue
    )

    EmberParticles(
        modifier = modifier,
        particleCount = particleCount,
        colors = charizardXFlames
    )
}



//@Preview
//@Composable
//fun EmberTestPreview() {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black)
//    ) {
//        AnimatedEmberBackground(
//
//        )
//    }
//}

