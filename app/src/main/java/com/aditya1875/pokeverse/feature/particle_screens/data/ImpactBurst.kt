package com.aditya1875.pokeverse.feature.particle_screens.data

data class ImpactBurst(
    var x: Float,
    var y: Float,
    val lines: List<ImpactLine>,
    val createdAt: Long,
    val lifetime: Long
)