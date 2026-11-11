package com.aditya1875.pokeverse.feature.leaderboard.domain.xp

import androidx.annotation.StringRes
import com.aditya1875.pokeverse.feature.progression.domain.LevelReward

sealed class XPEvent {
    object DailyLogin : XPEvent()

    // PokéQuiz
    data class QuizAnswer(val correct: Boolean) : XPEvent()
    data class QuizComplete(val score: Int, val total: Int) : XPEvent()

    // PokéMatch
    data class MatchComplete(val moves: Int, val par: Int) : XPEvent()

    // PokéGuess (silhouette game)
    data class GuessCorrect(val streak: Int) : XPEvent()
    /** [correct] is how many guesses the run got right: completion pays by effort, not for showing up. */
    data class GuessComplete(val correct: Int) : XPEvent()

    // PokéDuel (type-prediction game, dedicated events so labels + values are independent)
    data class DuelCorrect(val streak: Int) : XPEvent()
    data class DuelComplete(val correct: Int) : XPEvent()

    // TypeRush (rapid-fire type answer game)
    object RushCorrect : XPEvent()
    data class RushComplete(val score: Int, val total: Int) : XPEvent()

    // Bonus
    object FirstGameOfDay : XPEvent()
    object FirstExplorationOfDay : XPEvent()

    // Card Clash multiplayer
    // A match against another trainer pays double what one against the bot does.
    data class CardClashWin(val vsBot: Boolean = false) : XPEvent()
    data class CardClashRoundWin(val vsBot: Boolean = false) : XPEvent()
    data class CardClashPerfect(val vsBot: Boolean = false) : XPEvent()
    data class CardClashDraw(val vsBot: Boolean = false) : XPEvent()  // consolation for a drawn match

    // Wild Catch
    /** [rarityBonus]: a rarer Pokémon is worth more (see CatchTier.xpBonus). */
    data class WildCatchCaught(val streak: Int, val rarityBonus: Int = 0) : XPEvent()
    data class WildCatchComplete(val catches: Int) : XPEvent()

    // Pokémon Survivor
    data class SurvivorCorrect(val streak: Int) : XPEvent()
    data class SurvivorComplete(val correct: Int) : XPEvent()

    // Rocket Chase, awarded once per run, scaled by distance
    data class ChaseComplete(val meters: Int) : XPEvent()

    // Team Battle: paid once when a battle ends. [bonus] is the bot tier's extra for a win.
    data class BattleComplete(val won: Boolean, val bonus: Int) : XPEvent()

    // Online Team Battle against a real trainer: paid once when it ends, and only for a battle actually fought.
    data class OnlineBattleComplete(val won: Boolean) : XPEvent()

    // Easter Egg
    object EasterEggClaim : XPEvent()

    // Community, one-off bonuses, verified/deduplicated outside XPManager
    object ReferralWelcome : XPEvent()                       // you joined with a friend's code
    data class ReferralReward(val friends: Int) : XPEvent()  // friends joined with your code
    object FeedbackThanks : XPEvent()
    object DailyFact : XPEvent()                             // scratched open today's fact
    /** Finished today's Dex Daily; [xp] comes from DexDailyRules (fewer tries pay more). Once a day via its store. */
    data class DexDaily(val xp: Int) : XPEvent()

    /**
     * Whether this XP counts towards the WEEKLY leaderboard. Playing and showing up do; one-off community
     * bonuses (referrals, feedback) don't, or inviting friends would outrank actually playing.
     */
    val countsForWeekly: Boolean
        get() = !(this is ReferralWelcome || this is ReferralReward || this is FeedbackThanks)

    /** Once-a-day rewards for showing up, paid in full, outside [XPEconomy]'s tiers and rested pool. */
    val isRetentionBonus: Boolean
        get() = this is DailyLogin || this is FirstGameOfDay || this is FirstExplorationOfDay || this is EasterEggClaim ||
            this is ReferralWelcome || this is ReferralReward || this is FeedbackThanks || this is DailyFact || this is DexDaily
}

/**
 * Raw XP per event (before [XPEconomy]'s daily tiers and rested bonus).
 *
 * The yardstick: a typical, engaged session of any game (2-4 minutes) is worth about 40-60 raw XP, a great
 * one about 1.5x that, and a failed or abandoned one close to nothing. Rules that keep it fair:
 *  - Endless games pay a little per right answer, but only for the first [ENDLESS_PAID_CAP] of a run;
 *    after that a long run is its own reward. Their completion bonus is paid by effort (one XP per right
 *    answer up to the full amount), so starting a run and failing at once pays nothing.
 *  - A real opponent pays double what the bot does.
 *  - Wild Catch pays more for a rarer Pokémon, which is what rarity should be worth.
 *  - Once-a-day "show up" bonuses are modest next to actually playing.
 */
object XPValues {
    // Retention bonuses, paid in full, never tiered.
    const val DAILY_LOGIN = 20
    const val DAILY_STREAK_BONUS = 5
    /** The streak bonus stops growing here, so day 30 is not worth ten times day 3. */
    const val DAILY_STREAK_CAP = 35
    const val FIRST_GAME_OF_DAY = 30
    const val FIRST_EXPLORATION_OF_DAY = 10
    const val EASTER_EGG_CLAIM = 15

    // Community bonuses, also outside the tiers (and off the weekly leaderboard).
    const val REFERRAL_WELCOME = 150
    const val REFERRAL_PER_FRIEND = 300
    const val FEEDBACK_THANKS = 25
    const val DAILY_FACT = 10

    /** Right answers/catches paid individually in an endless run; further ones pay nothing extra. */
    const val ENDLESS_PAID_CAP = 20

    /** A perfect score only means something over at least this many rounds. */
    const val MIN_ROUNDS_FOR_PERFECT = 5

    const val QUIZ_CORRECT = 3
    const val QUIZ_COMPLETE = 15
    const val QUIZ_PERFECT = 15

    const val MATCH_COMPLETE = 30
    const val MATCH_UNDER_PAR = 15

    const val GUESS_CORRECT = 3
    const val GUESS_STREAK_3 = 1
    const val GUESS_STREAK_6 = 2
    const val GUESS_COMPLETE = 10

    // PokéDuel, same baseline as PokéGuess since the streak mechanic is identical
    const val DUEL_CORRECT = 3
    const val DUEL_COMPLETE = 10

    // TypeRush, rapid-fire, so the smallest per-answer value
    const val RUSH_CORRECT = 2
    const val RUSH_COMPLETE = 15
    const val RUSH_PERFECT = 15
    /** A TypeRush run shorter than this is not a finished run. */
    const val RUSH_MIN_ROUNDS = 5

    // Card Clash: a match is about five minutes and six rounds, so it pays the most per game.
    const val CLASH_WIN = 40
    const val CLASH_ROUND_WIN = 4
    const val CLASH_PERFECT = 15
    const val CLASH_DRAW = 15
    const val CLASH_BOT_WIN = 20
    const val CLASH_BOT_ROUND_WIN = 2
    const val CLASH_BOT_PERFECT = 8
    const val CLASH_BOT_DRAW = 8

    const val CATCH_CAUGHT = 3
    const val CATCH_STREAK_3 = 1
    const val CATCH_STREAK_6 = 2
    const val CATCH_COMPLETE = 10

    const val SURVIVOR_CORRECT = 3
    const val SURVIVOR_STREAK_5 = 1
    const val SURVIVOR_STREAK_10 = 2
    const val SURVIVOR_COMPLETE = 10

    /** Rocket Chase pays by distance: a short stumble earns nothing, a long clean run is one of the best payouts. */
    const val CHASE_MIN_METERS = 150
    const val CHASE_COMPLETE = 10
    const val CHASE_PER_100M = 4
    const val CHASE_DISTANCE_CAP = 80

    // Team Battle: a win is worth about a good game; a loss still pays a little for the attempt.
    const val BATTLE_WIN = 25
    const val BATTLE_LOSS = 8

    // Online Team Battle: a real opponent is worth more than a bot, win or lose.
    const val ONLINE_WIN = 40
    const val ONLINE_LOSS = 10
}

// ─── Result returned after awarding XP ───────────────────────────────────────
data class XPResult(
    val xpGained: Int,
    val newTotalXp: Int,
    val newLevel: Int,
    val newCurrentXp: Int,
    val newNextLevelXp: Int,
    val leveledUp: Boolean,
    /** What the popup says; null for a no-op award. Turned into text by the UI, in the user's language. */
    val label: XPLabel? = null,
    /** Dex Coins paid alongside this award (shown in the popup note). */
    val coinsGained: Int = 0,
    /** Level rewards unlocked by this award, oldest first; non-empty only on a level-up. */
    val levelRewards: List<LevelReward> = emptyList()
)

/**
 * A language-neutral XP popup message: "<title> +<amount> XP<flair>" plus optional notes.
 * Built as resource ids + numbers so the domain needs no Context, and the text always
 * follows the language the user picked (even right after an in-app language switch).
 */
data class XPLabel(
    @StringRes val title: Int,
    val titleArg: Int? = null,
    val amount: Int,
    /** Emoji/number decoration that reads the same in every language, e.g. " 🔥 x5". */
    val flair: String = "",
    val notes: List<XPNote> = emptyList()
)

sealed interface XPNote {
    data class StreakBonus(val xp: Int) : XPNote
    data class RestedBanked(val xp: Int) : XPNote
    data object RestedDouble : XPNote
    data class DailyRate(val percent: Int) : XPNote
    data object Perfect : XPNote
    data object UnderPar : XPNote
    data class Coins(val amount: Int) : XPNote
    data class StreakMilestone(val days: Int, val coins: Int) : XPNote
    data class StreakSaved(val freezesUsed: Int) : XPNote
}