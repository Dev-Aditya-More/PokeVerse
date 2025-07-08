package com.example.pokeverse.data.remote.model

import com.google.gson.annotations.SerializedName

data class PokemonResponse(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val sprites: Sprites,
    val types: List<TypeSlot>,
    val stats: List<StatSlot>
)

data class Sprites(
    val front_default: String?,
    var other: OtherSprites? = null,

)
data class TypeSlot(val type: Type)
data class Type(val name: String)
data class StatSlot(val base_stat: Int, val stat: Stat)
data class Stat(val name: String)

data class OtherSprites(
    @SerializedName("official-artwork")
    val officialArtwork: OfficialArtwork? = null
)

data class OfficialArtwork(
    @SerializedName("front_default")
    val frontDefault: String?
)