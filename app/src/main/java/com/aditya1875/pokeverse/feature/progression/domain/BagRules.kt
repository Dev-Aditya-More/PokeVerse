package com.aditya1875.pokeverse.feature.progression.domain

/**
 * How much the player's bag can carry. Every item has a base limit ([ShopItem.maxHeld]); each bag upgrade
 * multiplies it, so a full bag always has a way out. The Master Ball stays at one, whatever the bag: it is a
 * once-in-a-while catch by design. Pure Kotlin, unit-tested.
 */
object BagRules {

    const val MAX_LEVEL = 3

    /** What each upgrade costs, in order: level 0 to 1, 1 to 2, 2 to 3. */
    val UPGRADE_PRICES: List<Int> = listOf(1_000, 2_500, 6_000)

    /** How many times the base limit a bag of [level] carries: x1 at level 0, up to x4 at the top. */
    fun multiplier(level: Int): Int = 1 + level.coerceIn(0, MAX_LEVEL)

    /** The most of [item] a bag of [level] holds. */
    fun capacity(item: ShopItem, level: Int): Int =
        if (item.id == ShopItems.ID_MASTER_BALL) item.maxHeld else item.maxHeld * multiplier(level)

    /** The price of the next upgrade from [level], or null once the bag is at the top. */
    fun upgradePrice(level: Int): Int? = UPGRADE_PRICES.getOrNull(level.coerceAtLeast(0))
}
