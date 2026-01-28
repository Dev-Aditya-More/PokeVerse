package com.aditya1875.pokeverse.presentation.specialscreens.data

import androidx.compose.ui.graphics.Color

data class EnergyParticle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    val size: Float,
    val color: Color,
    var rotation: Float,
    val rotationSpeed: Float,
    val createdAt: Long,
    val lifetime: Long,
    val isSquare: Boolean
)