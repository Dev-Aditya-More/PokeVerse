package com.aditya1875.pokeverse.utils

/**
 * National Dex IDs for Legendary and Mythical Pokémon, covering every ID
 * range this app actually generates (games cap out at 1010 today). A static
 * set is used instead of the species endpoint's `is_legendary`/`is_mythical`
 * fields so games can show the badge with zero extra network calls — fetching
 * species data per random encounter would undo the loading-speed work done
 * alongside this.
 */
object LegendaryPokemon {
    private val ids = setOf(
        // Gen 1 — birds + Mewtwo, Mew
        144, 145, 146, 150, 151,
        // Gen 2 — beasts + Lugia, Ho-Oh, Celebi
        243, 244, 245, 249, 250, 251,
        // Gen 3 — Regis, Eon duo, weather trio, Jirachi, Deoxys
        377, 378, 379, 380, 381, 382, 383, 384, 385, 386,
        // Gen 4 — lake trio, creation trio, Heatran, Regigigas, Cresselia, Manaphy, Darkrai, Shaymin, Arceus
        // (Phione, 489, is a regular Pokémon and intentionally excluded)
        480, 481, 482, 483, 484, 485, 486, 487, 488, 490, 491, 492, 493,
        // Gen 5 — Victini, swords of justice, forces of nature, dragons, Keldeo, Meloetta, Genesect
        494, 638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 649,
        // Gen 6 — Xerneas, Yveltal, Zygarde, Diancie, Hoopa, Volcanion
        716, 717, 718, 719, 720, 721,
        // Gen 7 — Tapus, Cosmog line, Necrozma, Magearna, Marshadow, Zeraora, Meltan line
        785, 786, 787, 788, 789, 790, 791, 792, 800, 801, 802, 807, 808, 809,
        // Gen 8 — Zacian, Zamazenta, Eternatus, Kubfu line, Zarude, Regi additions, riders, Calyrex
        888, 889, 890, 891, 892, 893, 894, 895, 896, 897, 898,
        // Gen 9 — Treasures of Ruin, Paradox box legendaries (only IDs reachable by this app's 1..1010 range)
        1001, 1002, 1003, 1004, 1007, 1008
    )

    fun isLegendary(id: Int): Boolean = id in ids
}
