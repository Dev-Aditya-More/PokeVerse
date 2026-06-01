package com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model

enum class PokeGame(
    val id: String,
    val displayName: String,
    val generation: Int,
    val versionNames: List<String>,
    val versionGroupName: String
) {
    RED_BLUE("red-blue", "Red / Blue", 1, listOf("red", "blue"), "red-blue"),
    YELLOW("yellow", "Yellow", 1, listOf("yellow"), "yellow"),
    GOLD_SILVER("gold-silver", "Gold / Silver", 2, listOf("gold", "silver"), "gold-silver"),
    CRYSTAL("crystal", "Crystal", 2, listOf("crystal"), "crystal"),
    RUBY_SAPPHIRE("ruby-sapphire", "Ruby / Sapphire", 3, listOf("ruby", "sapphire"), "ruby-sapphire"),
    FIRERED_LEAFGREEN("firered-leafgreen", "FireRed / LeafGreen", 3, listOf("firered", "leafgreen"), "firered-leafgreen"),
    EMERALD("emerald", "Emerald", 3, listOf("emerald"), "emerald"),
    DIAMOND_PEARL("diamond-pearl", "Diamond / Pearl", 4, listOf("diamond", "pearl"), "diamond-pearl"),
    PLATINUM("platinum", "Platinum", 4, listOf("platinum"), "platinum"),
    HEARTGOLD_SOULSILVER("heartgold-soulsilver", "HG / SoulSilver", 4, listOf("heartgold", "soulsilver"), "heartgold-soulsilver"),
    BLACK_WHITE("black-white", "Black / White", 5, listOf("black", "white"), "black-white"),
    BLACK2_WHITE2("black-2-white-2", "Black 2 / White 2", 5, listOf("black-2", "white-2"), "black-2-white-2"),
    X_Y("x-y", "X / Y", 6, listOf("x", "y"), "x-y"),
    ORAS("omegaruby-alphasapphire", "OR / Alpha Sapphire", 6, listOf("omega-ruby", "alpha-sapphire"), "omegaruby-alphasapphire"),
    SUN_MOON("sun-moon", "Sun / Moon", 7, listOf("sun", "moon"), "sun-moon"),
    USUM("ultra-sun-ultra-moon", "Ultra Sun / Ultra Moon", 7, listOf("ultra-sun", "ultra-moon"), "ultra-sun-ultra-moon"),
    SWSH("sword-shield", "Sword / Shield", 8, listOf("sword", "shield"), "sword-shield"),
    BDSP("brilliant-diamond-shining-pearl", "BD / Shining Pearl", 8, listOf("brilliant-diamond", "shining-pearl"), "brilliant-diamond-and-shining-pearl"),
    PLA("legends-arceus", "Legends: Arceus", 8, listOf("legends-arceus"), "legends-arceus"),
    SV("scarlet-violet", "Scarlet / Violet", 9, listOf("scarlet", "violet"), "scarlet-violet");

    companion object {
        fun fromId(id: String): PokeGame? = entries.firstOrNull { it.id == id }

        val byGeneration: Map<Int, List<PokeGame>> = entries.groupBy { it.generation }
    }
}
