package com.aditya1875.pokeverse.presentation.specialscreens.data

data class ImpactBurst(
    var x: Float,
    var y: Float,
    val lines: List<ImpactLine>,
    val createdAt: Long,
    val lifetime: Long
)