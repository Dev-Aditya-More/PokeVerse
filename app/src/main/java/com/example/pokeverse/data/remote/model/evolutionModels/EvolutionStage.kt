package com.example.pokeverse.data.remote.model.evolutionModels

data class EvolutionStage(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val hasPrev: Boolean,
    val hasNext: Boolean,
    val prevId: Int? = null,
    val nextId: Int? = null
)
