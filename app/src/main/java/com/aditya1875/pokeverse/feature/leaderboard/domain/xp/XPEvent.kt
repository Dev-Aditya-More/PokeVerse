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
}

object XPValues {
    const val DAILY_LOGIN = 25
    const val DAILY_STREAK_BONUS = 10

    const val QUIZ_CORRECT = 5
    const val QUIZ_COMPLETE = 20
    const val QUIZ_PERFECT = 30

    const val MATCH_COMPLETE = 30
    const val MATCH_UNDER_PAR = 20

    const val GUESS_CORRECT = 15
    const val GUESS_STREAK_2 = 10
    const val GUESS_STREAK_5 = 25
    const val GUESS_COMPLETE = 20

    // PokéDuel — same baseline as PokéGuess since the streak mechanic is identical
    const val DUEL_CORRECT = 15
    const val DUEL_COMPLETE = 20

    // TypeRush — same baseline as PokéQuiz since it's rapid-fire answers
    const val RUSH_CORRECT = 5
    const val RUSH_COMPLETE = 20
    const val RUSH_PERFECT = 30

    const val FIRST_GAME_OF_DAY = 50
    const val FIRST_EXPLORATION_OF_DAY = 20

    const val CLASH_WIN = 60
    const val CLASH_ROUND_WIN = 8
    const val CLASH_PERFECT = 40
    const val CLASH_DRAW = 15        // played a full match, earned something
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