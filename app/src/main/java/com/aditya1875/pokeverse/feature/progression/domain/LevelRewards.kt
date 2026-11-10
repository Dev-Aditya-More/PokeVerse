package com.aditya1875.pokeverse.feature.progression.domain

/** What reaching a level gives: coins always, plus any cosmetics placed on that level. */
data class LevelReward(
    val level: Int,
    val coins: Int,
    val cosmeticIds: List<String> = emptyList()
) {
    /** Every 5th level is a milestone: bigger payout, shown highlighted on the Level Path. */
    val isMilestone: Boolean get() = level % 5 == 0
}

/**
 * The level reward track.
 *
 *  - Levels 2-4 pay out quickly so a new player feels progress inside the first day or two.
 *  - Every 5th level is a milestone with a real payout and (up to level 100) a cosmetic.
 *  - Past 100 it keeps paying coins so the curve never goes dead.
 *
 * Cosmetics come from [Cosmetics] (`levelUnlock`), so the track and the catalogue can't disagree.
 */
object LevelRewards {

    fun forLevel(level: Int): LevelReward? {
        if (level < 2) return null
        val cosmetics = Cosmetics.all.filter { it.levelUnlock == level }.map { it.id }
        return LevelReward(level, coinsFor(level), cosmetics)
    }

    /** Rewards for every level in (from, to], oldest first. Used for level-ups and for catching up existing players. */
    fun between(from: Int, to: Int): List<LevelReward> =
        ((from + 1)..to).mapNotNull { forLevel(it) }

    /** The next level at or after [level] that has a cosmetic, for the "next unlock" teaser. */
    fun nextCosmeticMilestone(level: Int): LevelReward? =
        (level + 1..level + 200).firstNotNullOfOrNull { l ->
            forLevel(l)?.takeIf { it.cosmeticIds.isNotEmpty() }
        }

    /**
     * A level takes 1000-2000 XP (one to two weeks of normal play), so what it pays has to be worth
     * the wait: ordinary levels pay about as much as a day or two of daily bonuses, and milestones
     * about a week or more. Past level 100 every level keeps paying.
     */
    private fun coinsFor(level: Int): Int {
        val milestone = level % 5 == 0
        return when {
            level == 2 -> 15
            level == 3 -> 25
            level == 4 -> 35
            milestone -> when {
                level == 5 -> 100
                level == 10 -> 150
                level == 15 -> 175
                level == 20 -> 250
                level == 25 -> 250
                level == 30 || level == 35 -> 300
                level < 50 -> 350
                level == 50 -> 600
                level < 75 -> 400
                level == 75 -> 700
                level < 100 -> 450
                level == 100 -> 1000
                else -> 500
            }
            level < 10 -> 25
            level < 20 -> 40
            level < 35 -> 50
            else -> 60
        }
    }
}
