package com.aditya1875.pokeverse.feature.progression.domain

import androidx.annotation.StringRes
import com.aditya1875.pokeverse.R

enum class ItemKind { STONE, EVO_ITEM, BALL, BOOST, PACK }

/**
 * A consumable the player buys with Poké Coins and spends in a game. Unlike a [Cosmetic] it is used up.
 *
 * [id] doubles as the inventory key, so for evolution items it is the PokeAPI item name
 * ("water-stone", "metal-coat"): the evolution chain says which item a Pokémon needs, and that string
 * finds it here. Ids are stored in DataStore/Firestore, so never rename one that has shipped.
 *
 * @param quantity how many one purchase adds (a Great Ball pack holds several).
 * @param maxHeld the most a player can hold at once, so nobody hoards a year of balls.
 * @param shards for [ItemKind.PACK] only: Evo Shards granted instead of an inventory item.
 */
data class ShopItem(
    val id: String,
    val kind: ItemKind,
    @StringRes val name: Int,
    @StringRes val desc: Int,
    val price: Int,
    val quantity: Int = 1,
    val maxHeld: Int = 5,
    val shards: Int = 0,
) {
    val isInventory: Boolean get() = kind != ItemKind.PACK
}

/** What the coin shop sells besides cosmetics. Prices live here so the economy is tuned in one place. */
object ShopItems {

    const val STONE_PRICE = 120
    const val EVO_ITEM_PRICE = 200

    const val GREAT_BALL_PACK = 5
    const val GREAT_BALL_PRICE = 100
    const val ULTRA_BALL_PACK = 3
    const val ULTRA_BALL_PRICE = 180
    /** A sure catch of anything, so it is a long-term goal. */
    const val MASTER_BALL_PRICE = 5_000

    const val LURE_PRICE = 80
    /** How many wild encounters one Rare Lure covers. */
    const val LURE_ENCOUNTERS = 5

    const val SHARD_POUCH_SHARDS = 40
    const val SHARD_POUCH_PRICE = 150

    const val ID_GREAT_BALL = "ball_great"
    const val ID_ULTRA_BALL = "ball_ultra"
    const val ID_MASTER_BALL = "ball_master"
    const val ID_LURE = "lure_rare"
    const val ID_SHARD_POUCH = "pack_shards"

    private fun stone(id: String, @StringRes name: Int) =
        ShopItem(id, ItemKind.STONE, name, R.string.item_desc_stone, STONE_PRICE, maxHeld = 5)

    private fun evoItem(id: String, @StringRes name: Int) =
        ShopItem(id, ItemKind.EVO_ITEM, name, R.string.item_desc_evo_item,
            EVO_ITEM_PRICE, maxHeld = 3)

    val stones: List<ShopItem> = listOf(
        stone("fire-stone", R.string.item_fire_stone),
        stone("water-stone", R.string.item_water_stone),
        stone("thunder-stone", R.string.item_thunder_stone),
        stone("leaf-stone", R.string.item_leaf_stone),
        stone("ice-stone", R.string.item_ice_stone),
        stone("moon-stone", R.string.item_moon_stone),
        stone("sun-stone", R.string.item_sun_stone),
        stone("shiny-stone", R.string.item_shiny_stone),
        stone("dusk-stone", R.string.item_dusk_stone),
        stone("dawn-stone", R.string.item_dawn_stone),
    )

    /** Held items that evolve a Pokémon when it is traded or levelled while holding them. */
    val evoItems: List<ShopItem> = listOf(
        evoItem("metal-coat", R.string.item_metal_coat),
        evoItem("kings-rock", R.string.item_kings_rock),
        evoItem("dragon-scale", R.string.item_dragon_scale),
        evoItem("up-grade", R.string.item_up_grade),
        evoItem("dubious-disc", R.string.item_dubious_disc),
        evoItem("protector", R.string.item_protector),
        evoItem("electirizer", R.string.item_electirizer),
        evoItem("magmarizer", R.string.item_magmarizer),
        evoItem("reaper-cloth", R.string.item_reaper_cloth),
        evoItem("razor-claw", R.string.item_razor_claw),
        evoItem("razor-fang", R.string.item_razor_fang),
        evoItem("deep-sea-tooth", R.string.item_deep_sea_tooth),
        evoItem("deep-sea-scale", R.string.item_deep_sea_scale),
        evoItem("prism-scale", R.string.item_prism_scale),
        evoItem("sachet", R.string.item_sachet),
        evoItem("whipped-dream", R.string.item_whipped_dream),
    )

    val balls: List<ShopItem> = listOf(
        ShopItem(ID_GREAT_BALL, ItemKind.BALL, R.string.item_great_ball, R.string.item_desc_great_ball,
            GREAT_BALL_PRICE, quantity = GREAT_BALL_PACK, maxHeld = 25),
        ShopItem(ID_ULTRA_BALL, ItemKind.BALL, R.string.item_ultra_ball, R.string.item_desc_ultra_ball,
            ULTRA_BALL_PRICE, quantity = ULTRA_BALL_PACK, maxHeld = 12),
        ShopItem(ID_MASTER_BALL, ItemKind.BALL, R.string.item_master_ball, R.string.item_desc_master_ball,
            MASTER_BALL_PRICE, maxHeld = 1),
    )

    val boosts: List<ShopItem> = listOf(
        ShopItem(ID_LURE, ItemKind.BOOST, R.string.item_rare_lure, R.string.item_desc_rare_lure,
            LURE_PRICE, maxHeld = 3),
        ShopItem(ID_SHARD_POUCH, ItemKind.PACK, R.string.item_shard_pouch, R.string.item_desc_shard_pouch,
            SHARD_POUCH_PRICE, shards = SHARD_POUCH_SHARDS),
    )

    val all: List<ShopItem> = balls + boosts + stones + evoItems

    private val byId = all.associateBy { it.id }

    fun find(id: String): ShopItem? = byId[id]

    /** The shop item that satisfies an evolution needing the PokeAPI item [apiName], or null when we don't sell it. */
    fun forEvolution(apiName: String?): ShopItem? =
        apiName?.let { byId[it] }?.takeIf { it.kind == ItemKind.STONE || it.kind == ItemKind.EVO_ITEM }
}
