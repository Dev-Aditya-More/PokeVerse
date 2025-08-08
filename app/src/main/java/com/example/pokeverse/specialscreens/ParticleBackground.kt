package com.example.pokeverse.specialscreens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class ParticleType {
    FIRE, WATER, GRASS, ROCK, ELECTRIC, FLYING, ICE, GROUND, POISON, BUG, GHOST, NONE
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
        // add more types here as needed
        else -> ParticleType.NONE
    }
}


@Composable
fun ParticleBackground(
    type: ParticleType
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (type) {
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
            // Add more as needed
        }
    }
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

