package com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels

data class EvolutionDetail(
    val gender: Any? = null,
    val known_move: Any? = null,
    val known_move_type: Any? = null,
    val trigger: Trigger,
    /** The item used on the Pokémon (stones), when the trigger is "use-item". */
    val item: ItemRef? = null,
    /** The item the Pokémon must hold (Metal Coat, King's Rock...) when traded or levelled. */
    val held_item: ItemRef? = null,
)

/** A named PokeAPI resource such as an item; only the name matters here. */
data class ItemRef(
    val name: String,
    val url: String? = null,
)
