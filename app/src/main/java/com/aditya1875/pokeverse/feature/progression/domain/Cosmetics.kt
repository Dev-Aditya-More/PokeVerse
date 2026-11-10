package com.aditya1875.pokeverse.feature.progression.domain

import androidx.annotation.StringRes
import com.aditya1875.pokeverse.R

enum class CosmeticKind { FRAME, TITLE, BACKDROP, WIDGET, THEME }

/**
 * One thing a player can own and show off. Purely cosmetic: nothing here touches XP, rank or gameplay.
 *
 * @param price coins to buy it in the shop, or null when it can only be earned at a level.
 * @param levelUnlock the level that grants it for free (shown on the Level Path), or null.
 * @param themeName for [CosmeticKind.THEME] only: the `AppTheme` constant it unlocks.
 */
data class Cosmetic(
    val id: String,
    val kind: CosmeticKind,
    @StringRes val name: Int,
    val price: Int? = null,
    val levelUnlock: Int? = null,
    val themeName: String? = null
) {
    val isShopItem: Boolean get() = price != null
}

/** The whole catalogue. Ids are stored in DataStore/Firestore, so never rename one that has shipped. */
object Cosmetics {

    val all: List<Cosmetic> = listOf(
        // ── Frames (the profile card's border, and the avatar ring on the leaderboard) ────────────────────────────────────────────
        Cosmetic("frame_pokeball", CosmeticKind.FRAME, R.string.cosmetic_frame_pokeball, levelUnlock = 5),
        Cosmetic("frame_ultra", CosmeticKind.FRAME, R.string.cosmetic_frame_ultra, levelUnlock = 20),
        Cosmetic("frame_master", CosmeticKind.FRAME, R.string.cosmetic_frame_master, levelUnlock = 35),
        Cosmetic("frame_legend", CosmeticKind.FRAME, R.string.cosmetic_frame_legend, levelUnlock = 50),
        Cosmetic("frame_mythic", CosmeticKind.FRAME, R.string.cosmetic_frame_mythic, levelUnlock = 100),
        Cosmetic("frame_flame", CosmeticKind.FRAME, R.string.cosmetic_frame_flame, price = 250),
        Cosmetic("frame_tide", CosmeticKind.FRAME, R.string.cosmetic_frame_tide, price = 250),
        Cosmetic("frame_leaf", CosmeticKind.FRAME, R.string.cosmetic_frame_leaf, price = 250),
        Cosmetic("frame_volt", CosmeticKind.FRAME, R.string.cosmetic_frame_volt, price = 300),
        Cosmetic("frame_shiny", CosmeticKind.FRAME, R.string.cosmetic_frame_shiny, price = 800),

        // ── Titles (shown under your name) ───────────────────────────────────
        Cosmetic("title_rookie", CosmeticKind.TITLE, R.string.cosmetic_title_rookie, levelUnlock = 3),
        Cosmetic("title_ace", CosmeticKind.TITLE, R.string.cosmetic_title_ace, levelUnlock = 10),
        Cosmetic("title_veteran", CosmeticKind.TITLE, R.string.cosmetic_title_veteran, levelUnlock = 20),
        Cosmetic("title_elite", CosmeticKind.TITLE, R.string.cosmetic_title_elite, levelUnlock = 25),
        Cosmetic("title_champion", CosmeticKind.TITLE, R.string.cosmetic_title_champion, levelUnlock = 40),
        Cosmetic("title_legend", CosmeticKind.TITLE, R.string.cosmetic_title_legend, levelUnlock = 50),
        Cosmetic("title_master", CosmeticKind.TITLE, R.string.cosmetic_title_master, levelUnlock = 100),
        // The long stretch between 50 and 100 used to have nothing to unlock; these give it landmarks.
        Cosmetic("title_trailblazer", CosmeticKind.TITLE, R.string.cosmetic_title_trailblazer, levelUnlock = 45),
        Cosmetic("title_rival", CosmeticKind.TITLE, R.string.cosmetic_title_rival, levelUnlock = 55),
        Cosmetic("title_regionhero", CosmeticKind.TITLE, R.string.cosmetic_title_regionhero, levelUnlock = 70),
        Cosmetic("title_halloffame", CosmeticKind.TITLE, R.string.cosmetic_title_halloffame, levelUnlock = 80),
        Cosmetic("title_dexmaster", CosmeticKind.TITLE, R.string.cosmetic_title_dexmaster, levelUnlock = 90),
        Cosmetic("title_bugcatcher", CosmeticKind.TITLE, R.string.cosmetic_title_bugcatcher, price = 100),
        Cosmetic("title_fan", CosmeticKind.TITLE, R.string.cosmetic_title_fan, price = 150),
        Cosmetic("title_challenger", CosmeticKind.TITLE, R.string.cosmetic_title_challenger, price = 200),

        // ── Profile backdrops. All dark on purpose: every partner's colours stay readable on them. ──
        Cosmetic("backdrop_twilight", CosmeticKind.BACKDROP, R.string.cosmetic_backdrop_twilight, levelUnlock = 15),
        Cosmetic("backdrop_aurora", CosmeticKind.BACKDROP, R.string.cosmetic_backdrop_aurora, levelUnlock = 30),
        Cosmetic("backdrop_cosmos", CosmeticKind.BACKDROP, R.string.cosmetic_backdrop_cosmos, levelUnlock = 60),
        Cosmetic("backdrop_night", CosmeticKind.BACKDROP, R.string.cosmetic_backdrop_night, price = 200),
        Cosmetic("backdrop_deepsea", CosmeticKind.BACKDROP, R.string.cosmetic_backdrop_deepsea, price = 250),
        Cosmetic("backdrop_forge", CosmeticKind.BACKDROP, R.string.cosmetic_backdrop_forge, price = 250),

        // ── Streak widget looks ──────────────────────────────────────────────
        Cosmetic("widget_royal", CosmeticKind.WIDGET, R.string.cosmetic_widget_royal, levelUnlock = 25),
        Cosmetic("widget_ember", CosmeticKind.WIDGET, R.string.cosmetic_widget_ember, price = 300),
        Cosmetic("widget_ocean", CosmeticKind.WIDGET, R.string.cosmetic_widget_ocean, price = 300),
        Cosmetic("widget_mint", CosmeticKind.WIDGET, R.string.cosmetic_widget_mint, price = 300),

        // ── App themes that are normally Pro. Pro still unlocks all of them. ──
        Cosmetic("theme_sylveon", CosmeticKind.THEME, R.string.cosmetic_theme_sylveon, price = 1000, themeName = "SYLVEON"),
        Cosmetic("theme_lucario", CosmeticKind.THEME, R.string.cosmetic_theme_lucario, price = 1000, themeName = "LUCARIO"),
        Cosmetic("theme_greninja", CosmeticKind.THEME, R.string.cosmetic_theme_greninja, price = 1000, themeName = "GRENINJA"),
    )

    private val byId = all.associateBy { it.id }

    fun find(id: String): Cosmetic? = byId[id]

    /** Ids of every level-earned cosmetic a player at [level] should own, however long ago they passed it. */
    fun earnedByLevel(level: Int): List<String> =
        all.filter { it.levelUnlock != null && it.levelUnlock <= level }.map { it.id }

    fun ofKind(kind: CosmeticKind): List<Cosmetic> = all.filter { it.kind == kind }

    /** The coin-bought theme constant names, e.g. {"SYLVEON", ...}. */
    fun themeUnlockedBy(ownedIds: Set<String>): Set<String> =
        ownedIds.mapNotNull { byId[it]?.themeName }.toSet()
}
