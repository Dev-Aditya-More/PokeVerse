package com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.itemModels

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.NamedApiResource
import com.google.gson.annotations.SerializedName

data class ItemListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<NamedApiResource>
)

data class ItemDetail(
    val id: Int,
    val name: String,
    val cost: Int,
    val category: NamedApiResource,
    val attributes: List<NamedApiResource>,
    @SerializedName("effect_entries") val effectEntries: List<ItemEffect>,
    @SerializedName("flavor_text_entries") val flavorTextEntries: List<ItemFlavorText>,
    val sprites: ItemSprites,
    @SerializedName("held_by_pokemon") val heldByPokemon: List<ItemHolderPokemon>,
    @SerializedName("fling_power") val flingPower: Int?,
    @SerializedName("fling_effect") val flingEffect: NamedApiResource?,
)

data class ItemSprites(
    val default: String?
)

data class ItemEffect(
    val effect: String,
    @SerializedName("short_effect") val shortEffect: String,
    val language: NamedApiResource
)

data class ItemFlavorText(
    @SerializedName("text") val text: String,
    val language: NamedApiResource,
    @SerializedName("version_group") val versionGroup: NamedApiResource
)

data class ItemHolderPokemon(
    val pokemon: NamedApiResource,
    @SerializedName("version_details") val versionDetails: List<Any>
)

// ── Clean UI model (what the screen actually uses) ────────────────────────────
data class ItemUiModel(
    val id: Int,
    val name: String,                   // "master-ball" → display as "Master Ball"
    val displayName: String,            // formatted
    val spriteUrl: String,
    val category: String,               // "standard-balls"
    val categoryDisplay: String,        // "Standard Balls"
    val effect: String,                 // English short_effect
    val effectFull: String,             // English full effect
    val flavorText: String,             // most recent English flavor text
    val attributes: List<String>,       // ["countable", "consumable", ...]
    val cost: Int,
    val heldByPokemon: List<String>,    // pokemon names
    val flingPower: Int?,
) {
    val categoryColor: Long
        get() = when {
            category.contains("ball") -> 0xFF1565C0  // deep blue
            category.contains("heal") -> 0xFF2E7D32  // green
            category.contains("berry") -> 0xFFE65100  // orange
            category.contains("held") -> 0xFF6A1B9A  // purple
            category.contains("medicine") -> 0xFF00695C  // teal
            category.contains("battle") -> 0xFFB71C1C  // red
            category.contains("evolution") -> 0xFF4527A0  // indigo
            category.contains("fossil") -> 0xFF4E342E  // brown
            category.contains("key") -> 0xFF37474F  // dark blue-grey
            category.contains("tm") ||
                    category.contains("hm") -> 0xFFFF6F00  // amber
            else -> 0xFF37474F
        }
}

fun ItemDetail.toUiModel(): ItemUiModel {
    val displayName = name.split("-")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    val categoryDisplay = category.name.split("-")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    val englishEffect = effectEntries.firstOrNull { it.language.name == "en" }
    val englishFlavor = flavorTextEntries
        .lastOrNull { it.language.name == "en" }?.text
        ?.replace("\n", " ")
        ?: ""

    return ItemUiModel(
        id = id,
        name = name,
        displayName = displayName,
        spriteUrl = sprites.default ?: "",
        category = category.name,
        categoryDisplay = categoryDisplay,
        effect = englishEffect?.shortEffect ?: "",
        effectFull = englishEffect?.effect?.replace("\n", " ") ?: "",
        flavorText = englishFlavor,
        attributes = attributes.map { it.name },
        cost = cost,
        heldByPokemon = heldByPokemon.map { it.pokemon.name },
        flingPower = flingPower,
    )
}