package com.aditya1875.pokeverse.feature.leaderboard.data.remote.model

data class LeaderboardEntry(
    val uid: String = "",
    val displayName: String = "",
    val bio: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val totalXp: Int = 0,
    val weeklyXp: Int = 0,
    val level: Int = 1,
    val rank: Int = 0,
    val previousRank: Int = 0,
    val lastWeeklyReset: Long? = null,
    /** Cosmetic only, gold ring + badge. Never affects ranking. */
    val isPremium: Boolean = false,
    /** Cosmetic only, equipped avatar frame / title ids from the Poké Coin shop. */
    val frame: String = "",
    val title: String = "",
    /** Cosmetic only, profile backdrop id, partner Pokémon id and favorite type, shown on the trainer's row. */
    val backdrop: String = "",
    val starter: String = "",
    val favoriteType: String = ""
)
