package com.example.pokeverse.specialscreens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class ParticleType {
    FIRE, WATER, GRASS, NONE
}

fun getParticleTypeFor(types: List<String>): ParticleType {
    return when {
        types.contains("fire") -> ParticleType.FIRE
        types.contains("water") -> ParticleType.WATER
        types.contains("grass") -> ParticleType.GRASS
        // Add more types...
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

