package com.aditya1875.pokeverse.feature.berry.data.source.remote.model

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.NamedApiResource
import com.google.gson.annotations.SerializedName

data class BerryListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<NamedApiResource>
)

data class BerryDetail(
    val id: Int,
    val name: String,
    @SerializedName("growth_time") val growthTime: Int,
    @SerializedName("natural_gift_power") val naturalGiftPower: Int,
    val size: Int,
    val firmness: NamedApiResource,
    val flavors: List<BerryFlavor>,
    val item: NamedApiResource,
    @SerializedName("natural_gift_type") val naturalGiftType: NamedApiResource?
)

data class BerryFlavor(
    val potency: Int,
    val flavor: NamedApiResource
)

data class BerryUiModel(
    val id: Int,
    val name: String,
    val displayName: String,
    val spriteUrl: String,
    val naturalGiftType: String,
    val naturalGiftPower: Int,
    val firmness: String,
    val size: Int,
    val growthTime: Int,
    val dominantFlavor: String,
    val flavorPotencies: Map<String, Int>
)

private const val SPRITE_BASE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/"

fun BerryDetail.toUiModel(): BerryUiModel {
    val displayName = name.split("-")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } + " Berry"

    val flavorMap = flavors.associate { it.flavor.name to it.potency }
    val dominant = flavors.maxByOrNull { it.potency }
        ?.takeIf { it.potency > 0 }?.flavor?.name ?: "neutral"

    return BerryUiModel(
        id = id,
        name = name,
        displayName = displayName,
        spriteUrl = "$SPRITE_BASE${item.name}.png",
        naturalGiftType = naturalGiftType?.name ?: "normal",
        naturalGiftPower = naturalGiftPower,
        firmness = firmness.name,
        size = size,
        growthTime = growthTime,
        dominantFlavor = dominant,
        flavorPotencies = flavorMap
    )
}
