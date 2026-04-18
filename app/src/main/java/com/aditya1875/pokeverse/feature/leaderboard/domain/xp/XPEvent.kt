package com.aditya1875.pokeverse.feature.leaderboard.domain.xp

sealed class XPEvent {
    object DailyLogin : XPEvent()

    // PokéQuiz
    data class QuizAnswer(val correct: Boolean) : XPEvent()
    data class QuizComplete(val score: Int, val total: Int) : XPEvent()

    // PokéMatch
    data class MatchComplete(val moves: Int, val par: Int) : XPEvent()

    // PokéGuess
    data class GuessCorrect(val streak: Int) : XPEvent()
    object GuessComplete : XPEvent()

    // Bonus
    object FirstGameOfDay : XPEvent()

    object FirstExplorationOfDay : XPEvent()
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

    const val FIRST_GAME_OF_DAY = 50
    const val FIRST_EXPLORATION_OF_DAY = 20  // ← NEW
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