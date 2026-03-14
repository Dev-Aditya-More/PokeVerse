package com.aditya1875.pokeverse.data.remote.model

data class UserProfile(
    val uid: String = "",
    val username: String = "Trainer",
    val level: Int = 1,
    val totalXp: Int = 0,
    val currentXp: Int = 0,           // XP within current level (resets each level-up)
    val nextLevelXp: Int = 100,       // XP needed to reach next level
    val gamesPlayed: Int = 0,
    val bestTypeRushScore: Int = 0,
    val bestQuizScore: Int = 0,
    val bestMatchScore: Int = 0,
    val bestGuessScore: Int = 0,
    val isGuest: Boolean = true,

    // XP tracking
    val lastDailyXpDate: String = "",         // "yyyy-MM-dd" — for daily bonus dedup
    val dailyStreak: Int = 0,                 // consecutive days opened
    val lastActiveDateMillis: Long = 0L,

    // Leaderboard
    val rank: Int = 0,                        // written by Cloud Function
    val photoUrl: String = "",
    val email: String = ""
) {
    companion object {
        val GUEST = UserProfile(
            uid = "guest",
            username = "Guest Trainer",
            isGuest = true
        )
    }
}

// ─── XP Curve ─────────────────────────────────────────────────────────────────
// Level  1→2 :   100 XP   (easy start)
// Level  2→3 :   250 XP
// Level  3→4 :   500 XP
// Level  4→5 :   800 XP
// Level  5→10:  1000 XP each
// Level 10→20:  1500 XP each
// Level 20+  :  2000 XP each  (long grind for top ranks)
//
// With daily bonus (25 XP) + a few games/day (~100–200 XP):
//   Level 1→5  ≈ 2–3 days of casual play
//   Level 5→10 ≈ 1 week
//   Level 10→20 ≈ 2–3 weeks
//   Level 20+   ≈ long term grind

object LevelConfig {

    fun xpRequiredForLevel(level: Int): Int = when (level) {
        1 -> 100
        2 -> 250
        3 -> 500
        4 -> 800
        in 5..9 -> 1000
        in 10..19 -> 1500
        else -> 2000
    }

    // Returns Triple(level, currentXp within level, xpNeededForNextLevel)
    fun computeLevel(totalXp: Int): Triple<Int, Int, Int> {
        var remaining = totalXp
        var level = 1
        while (true) {
            val needed = xpRequiredForLevel(level)
            if (remaining < needed) {
                return Triple(level, remaining, needed)
            }
            remaining -= needed
            level++
        }
    }

    // Useful for profile display: cumulative XP to reach a given level
    fun totalXpToReachLevel(targetLevel: Int): Int {
        var total = 0
        for (l in 1 until targetLevel) {
            total += xpRequiredForLevel(l)
        }
        return total
    }
}