package com.aditya1875.pokeverse.feature.progression.domain

import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent

/**
 * Where Dex Coins come from, and the streak-protection rule. Pure Kotlin, unit-tested.
 *
 * Coins are deliberately *not* tied to how much XP you grind:
 *  - every faucet is a once-a-day bonus or a small, capped completion reward, so a marathon
 *    session earns the same coins as a short one (a casual day is ~25-35, the ceiling is [DAILY_MAX]);
 *  - nothing pays per answer, so there is nothing to farm;
 *  - coins buy cosmetics only, so they never affect XP, level or rank.
 */
object CoinEconomy {

    const val DAILY_LOGIN = 5
    const val FIRST_GAME_OF_DAY = 10
    const val FIRST_EXPLORATION = 5
    const val DAILY_FACT = 5
    const val DEX_DAILY_SOLVED = 10
    const val DEX_DAILY_TRIED = 3

    /** Finishing a game pays this, for the first [COMPLETIONS_PER_DAY] finished games each day. */
    const val COMPLETION = 4
    const val COMPLETIONS_PER_DAY = 5

    const val FREEZE_PRICE = 100
    const val FREEZE_MAX = 2

    /** The most coins one day of play can pay (excluding streak milestones and level rewards). */
    const val DAILY_MAX = DAILY_LOGIN + FIRST_GAME_OF_DAY + FIRST_EXPLORATION + DAILY_FACT +
        DEX_DAILY_SOLVED + COMPLETION * COMPLETIONS_PER_DAY

    /** Coins for [event]. [completionsToday] is how many finished games already paid today. */
    fun forEvent(event: XPEvent, completionsToday: Int): EventCoins = when (event) {
        XPEvent.FirstGameOfDay -> EventCoins(FIRST_GAME_OF_DAY)
        XPEvent.FirstExplorationOfDay -> EventCoins(FIRST_EXPLORATION)
        XPEvent.DailyFact -> EventCoins(DAILY_FACT)
        is XPEvent.DexDaily -> EventCoins(if (event.xp >= 15) DEX_DAILY_SOLVED else DEX_DAILY_TRIED)
        is XPEvent.QuizComplete, is XPEvent.MatchComplete, is XPEvent.GuessComplete, is XPEvent.DuelComplete,
        is XPEvent.RushComplete, is XPEvent.WildCatchComplete, is XPEvent.SurvivorComplete,
        is XPEvent.ChaseComplete, is XPEvent.BattleComplete, is XPEvent.OnlineBattleComplete, is XPEvent.CardClashWin, is XPEvent.CardClashDraw ->
            if (completionsToday < COMPLETIONS_PER_DAY) EventCoins(COMPLETION, countsAsCompletion = true)
            else EventCoins(0)
        else -> EventCoins(0)
    }

    data class EventCoins(val coins: Int, val countsAsCompletion: Boolean = false)

    /** Streak length to coins, in order. After the last one, every further 50 days pays [STREAK_REPEAT_COINS]. */
    val STREAK_MILESTONES: List<Pair<Int, Int>> = listOf(3 to 15, 7 to 40, 14 to 75, 30 to 150, 60 to 250, 100 to 500)
    const val STREAK_REPEAT_COINS = 300

    /** One-time-per-streak bonus for reaching [streak] days in a row (0 for an ordinary day). */
    fun streakMilestone(streak: Int): Int =
        STREAK_MILESTONES.firstOrNull { it.first == streak }?.second
            ?: if (streak > 100 && streak % 50 == 0) STREAK_REPEAT_COINS else 0

    /** The result of [guardStreak]. */
    data class StreakGuard(val kept: Boolean, val freezesUsed: Int)

    /**
     * Decides whether a returning player's streak survives.
     * [daysAway] = calendar days since the last daily check-in (1 = yesterday, i.e. nothing missed).
     * Each freeze covers one missed day. Freezes are all-or-nothing: if they can't cover the whole gap
     * the streak resets and none are spent, so a freeze is never wasted on a streak that was lost anyway.
     */
    fun guardStreak(daysAway: Int, freezes: Int): StreakGuard {
        val missed = daysAway - 1
        return when {
            missed <= 0 -> StreakGuard(kept = daysAway == 1, freezesUsed = 0)
            freezes >= missed -> StreakGuard(kept = true, freezesUsed = missed)
            else -> StreakGuard(kept = false, freezesUsed = 0)
        }
    }
}
