package com.aditya1875.pokeverse.feature.leaderboard.domain.xp

sealed class XPEvent {
    object DailyLogin : XPEvent()

    // PokéQuiz
    data class QuizAnswer(val correct: Boolean) : XPEvent()
    data class QuizComplete(val score: Int, val total: Int) : XPEvent()

    // PokéMatch
    data class MatchComplete(val moves: Int, val par: Int) : XPEvent()

    // PokéGuess (silhouette game)
    data class GuessCorrect(val streak: Int) : XPEvent()
    object GuessComplete : XPEvent()

    // PokéDuel (type-prediction game — dedicated events so labels + values are independent)
    data class DuelCorrect(val streak: Int) : XPEvent()
    object DuelComplete : XPEvent()

    // TypeRush (rapid-fire type answer game)
    object RushCorrect : XPEvent()
    data class RushComplete(val score: Int, val total: Int) : XPEvent()

    // Bonus
    object FirstGameOfDay : XPEvent()
    object FirstExplorationOfDay : XPEvent()

    // Card Clash multiplayer
    object CardClashWin : XPEvent()
    object CardClashRoundWin : XPEvent()
    object CardClashPerfect : XPEvent()
    object CardClashDraw : XPEvent()  // consolation for a drawn match

    // Wild Catch
    data class WildCatchCaught(val streak: Int) : XPEvent()
    object WildCatchComplete : XPEvent()

    // Pokémon Survivor
    data class SurvivorCorrect(val streak: Int) : XPEvent()
    object SurvivorComplete : XPEvent()

    // Rocket Chase — awarded once per run, scaled by distance
    data class ChaseComplete(val meters: Int) : XPEvent()

    // Easter Egg
    object EasterEggClaim : XPEvent()

    /** Once-a-day rewards for showing up — paid in full, outside [XPEconomy]'s tiers and rested pool. */
    val isRetentionBonus: Boolean
        get() = this is DailyLogin || this is FirstGameOfDay || this is FirstExplorationOfDay || this is EasterEggClaim
}

/**
 * Raw XP per event (before [XPEconomy]'s daily tiers and rested bonus).
 *
 * Balanced so a typical session of any game is worth roughly 40–70 raw XP.
 * Per-answer values stay small on purpose: endless modes used to pay 20–50 XP
 * per answer, which let one long session outrun weeks of regular play.
 */
object XPValues {
    // Retention bonuses — paid in full, never tiered.
    const val DAILY_LOGIN = 25
    const val DAILY_STREAK_BONUS = 10
    const val FIRST_GAME_OF_DAY = 40
    const val FIRST_EXPLORATION_OF_DAY = 20
    const val EASTER_EGG_CLAIM = 25

    const val QUIZ_CORRECT = 3
    const val QUIZ_COMPLETE = 15
    const val QUIZ_PERFECT = 15

    const val MATCH_COMPLETE = 30
    const val MATCH_UNDER_PAR = 15

    const val GUESS_CORRECT = 4
    const val GUESS_STREAK_2 = 1
    const val GUESS_STREAK_5 = 3
    const val GUESS_COMPLETE = 10

    // PokéDuel — same baseline as PokéGuess since the streak mechanic is identical
    const val DUEL_CORRECT = 4
    const val DUEL_COMPLETE = 10

    // TypeRush — rapid-fire, so the smallest per-answer value
    const val RUSH_CORRECT = 2
    const val RUSH_COMPLETE = 15
    const val RUSH_PERFECT = 15

    const val CLASH_WIN = 40
    const val CLASH_ROUND_WIN = 5
    const val CLASH_PERFECT = 20
    const val CLASH_DRAW = 15        // played a full match, earned something

    const val CATCH_CAUGHT = 4
    const val CATCH_STREAK_3 = 2
    const val CATCH_STREAK_6 = 4
    const val CATCH_COMPLETE = 10

    const val SURVIVOR_CORRECT = 3
    const val SURVIVOR_STREAK_5 = 1
    const val SURVIVOR_STREAK_10 = 3
    const val SURVIVOR_COMPLETE = 10

    const val CHASE_COMPLETE = 10
    const val CHASE_PER_100M = 2
    const val CHASE_DISTANCE_CAP = 40
}

// ─── Result returned after awarding XP ───────────────────────────────────────
data class XPResult(
    val xpGained: Int,
    val newTotalXp: Int,
    val newLevel: Int,
    val newCurrentXp: Int,
    val newNextLevelXp: Int,
    val leveledUp: Boolean,
    val label: String
)