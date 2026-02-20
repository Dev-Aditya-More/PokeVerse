package com.aditya1875.pokeverse.data.remote.model

import com.google.gson.annotations.SerializedName

data class PokemonResponse(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val sprites: Sprites,
    val types: List<TypeSlot>,
    val stats: List<StatSlot>,
    val moves: List<Move>,
    val cries: PokemonCries?
)

data class PokemonCries(
    val latest: String?,
    val legacy: String?
)

data class Move(
    val move: NamedApiResource,
    val version_group_details: List<VersionGroupDetail>
)

data class VersionGroupDetail(
    val level_learned_at: Int,
    val move_learn_method: NamedApiResource,
    val order: Int?,
    val version_group: NamedApiResource
)

data class NamedApiResource(
    val name: String,
    val url: String
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
    val officialArtwork: OfficialArtwork? = null,
    @SerializedName("home")
    val home: Home? = null,
    @SerializedName("showdown")
    val showdown: Showdown? = null,
    @SerializedName("dream_world")
    val dreamWorld: DreamWorld? = null
)

data class OfficialArtwork(
    @SerializedName("front_default")
    val frontDefault: String?,
    @SerializedName("front_shiny")
    val frontShiny: String?
)

data class Home(
    @SerializedName("front_default")
    val frontDefault: String?,
    @SerializedName("front_shiny")
    val frontShiny: String?
)

data class Showdown(
    @SerializedName("front_default")
    val frontDefault: String?,
    @SerializedName("front_shiny")
    val frontShiny: String?
)

data class DreamWorld(
    @SerializedName("front_default")
    val frontDefault: String?,
    @SerializedName("front_shiny")
    val frontShiny: String?
)