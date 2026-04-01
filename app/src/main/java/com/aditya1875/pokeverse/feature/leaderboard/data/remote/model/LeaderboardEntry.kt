package com.aditya1875.pokeverse.feature.leaderboard.data.remote.model

data class LeaderboardEntry(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val totalXp: Int = 0,
    val weeklyXp: Int = 0,
    val level: Int = 1,
    val rank: Int = 0,
)
