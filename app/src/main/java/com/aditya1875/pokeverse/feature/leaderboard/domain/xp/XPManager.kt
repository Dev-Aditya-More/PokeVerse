package com.aditya1875.pokeverse.feature.leaderboard.domain.xp

import androidx.annotation.StringRes
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.LevelConfig
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import com.aditya1875.pokeverse.utils.WeeklyReset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Single entry point for every XP change.
 *
 * - **Serialized**: awards run one at a time under [awardLock]. Each award is a
 *   read-modify-write of the profile, and two overlapping ones used to overwrite
 *   each other's XP.
 * - **Cheap**: every award saves locally right away, but Firestore only receives
 *   the latest profile once things go quiet ([SYNC_DEBOUNCE_MS]). A 30-answer
 *   session is now one or two cloud writes instead of ~60, and an older snapshot
 *   can never land after a newer one. If the app dies before the sync fires, the
 *   next launch's cloud reconcile pushes the newer local total.
 * - **Balanced**: game XP goes through [XPEconomy]; retention bonuses don't.
 */
class XPManager(
    private val repository: UserProfileRepository
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val awardLock = Mutex()
    private var pendingSync: Job? = null

    suspend fun awardDailyXP(): XPResult? = awardLock.withLock {
        val profile = repository.profileFlow.first()
        val today = today()
        if (profile.lastDailyXpDate == today) return@withLock null

        val daysAway = daysBetween(profile.lastDailyXpDate, today)
        val newStreak = if (daysAway == 1) profile.dailyStreak + 1 else 1
        val streakBonus = minOf((newStreak - 1) * XPValues.DAILY_STREAK_BONUS, 50)
        val totalGained = XPValues.DAILY_LOGIN + streakBonus
        val restedGain = XPEconomy.restedGain(daysAway, profile.restedXp)

        val label = XPLabel(
            title = R.string.xp_daily_showup,
            amount = XPValues.DAILY_LOGIN,
            notes = buildList {
                if (streakBonus > 0) add(XPNote.StreakBonus(streakBonus))
                if (restedGain > 0) add(XPNote.RestedBanked(profile.restedXp + restedGain))
            }
        )

        applyXP(profile, totalGained, label) { updated ->
            updated.copy(
                lastDailyXpDate = today,
                dailyStreak = newStreak,
                lastActiveDateMillis = System.currentTimeMillis(),
                restedXp = profile.restedXp + restedGain
            )
        }
    }

    suspend fun awardGameXP(event: XPEvent): XPResult = awardLock.withLock {
        val profile = repository.profileFlow.first()
        if (profile.isGuest) return@withLock noOpResult(profile)

        val today = today()
        val raw = rawXp(event, profile, today) ?: return@withLock noOpResult(profile)
        if (raw.amount <= 0) return@withLock noOpResult(profile)

        if (event.isRetentionBonus) {
            return@withLock applyXP(profile, raw.amount, XPLabel(raw.title, raw.titleArg, raw.amount, raw.flair, raw.notes)) { updated ->
                when (event) {
                    is XPEvent.FirstGameOfDay -> updated.copy(lastFirstGameXpDate = today)
                    is XPEvent.FirstExplorationOfDay -> updated.copy(lastExplorationXpDate = today)
                    is XPEvent.EasterEggClaim -> updated.copy(lastEasterEggXpDate = today)
                    else -> updated
                }
            }
        }

        val earnedToday = if (profile.dailyGameXpDate == today) profile.dailyGameXp else 0
        val award = XPEconomy.award(raw.amount, earnedToday, profile.restedXp)
        applyXP(profile, award.total, gameLabel(raw, award)) { updated ->
            updated.copy(
                dailyGameXp = earnedToday + raw.amount,
                dailyGameXpDate = today,
                restedXp = profile.restedXp - award.restedBonus
            )
        }
    }

    private class RawXp(
        val amount: Int,
        @StringRes val title: Int,
        val flair: String = "",
        val titleArg: Int? = null,
        val notes: List<XPNote> = emptyList()
    )

    /** Raw XP for [event], or null when a once-a-day reward was already claimed today. */
    private fun rawXp(event: XPEvent, profile: UserProfile, today: String): RawXp? = when (event) {
        is XPEvent.QuizAnswer ->
            RawXp(if (event.correct) XPValues.QUIZ_CORRECT else 0, R.string.xp_correct_answer)
        is XPEvent.QuizComplete -> {
            val perfect = event.score == event.total && event.total > 0
            RawXp(
                XPValues.QUIZ_COMPLETE + if (perfect) XPValues.QUIZ_PERFECT else 0,
                R.string.xp_quiz_complete, notes = if (perfect) listOf(XPNote.Perfect) else emptyList()
            )
        }
        is XPEvent.MatchComplete -> {
            val underPar = event.moves <= event.par
            RawXp(
                XPValues.MATCH_COMPLETE + if (underPar) XPValues.MATCH_UNDER_PAR else 0,
                R.string.xp_match_complete, notes = if (underPar) listOf(XPNote.UnderPar) else emptyList()
            )
        }
        is XPEvent.GuessCorrect -> streakAward(XPValues.GUESS_CORRECT, guessStreakBonus(event.streak), event.streak, R.string.xp_correct_guess)
        is XPEvent.GuessComplete -> RawXp(XPValues.GUESS_COMPLETE, R.string.xp_guess_complete)
        is XPEvent.DuelCorrect -> streakAward(XPValues.DUEL_CORRECT, guessStreakBonus(event.streak), event.streak, R.string.xp_right_call)
        is XPEvent.DuelComplete -> RawXp(XPValues.DUEL_COMPLETE, R.string.xp_duel_complete)
        is XPEvent.RushCorrect -> RawXp(XPValues.RUSH_CORRECT, R.string.xp_rush_hit)
        is XPEvent.RushComplete -> {
            val perfect = event.score == event.total && event.total > 0
            RawXp(
                XPValues.RUSH_COMPLETE + if (perfect) XPValues.RUSH_PERFECT else 0,
                R.string.xp_rush_complete, notes = if (perfect) listOf(XPNote.Perfect) else emptyList()
            )
        }
        is XPEvent.CardClashWin -> RawXp(XPValues.CLASH_WIN, R.string.xp_clash_victory)
        is XPEvent.CardClashRoundWin -> RawXp(XPValues.CLASH_ROUND_WIN, R.string.xp_round_won)
        is XPEvent.CardClashPerfect -> RawXp(XPValues.CLASH_PERFECT, R.string.xp_perfect_sweep)
        is XPEvent.CardClashDraw -> RawXp(XPValues.CLASH_DRAW, R.string.xp_clash_draw)
        is XPEvent.WildCatchCaught -> {
            val bonus = when {
                event.streak >= 6 -> XPValues.CATCH_STREAK_6
                event.streak >= 3 -> XPValues.CATCH_STREAK_3
                else -> 0
            }
            RawXp(XPValues.CATCH_CAUGHT + bonus, R.string.xp_caught, if (bonus > 0) " 🎣 x${event.streak}" else "")
        }
        is XPEvent.WildCatchComplete -> RawXp(XPValues.CATCH_COMPLETE, R.string.xp_wildcatch_complete)
        is XPEvent.SurvivorCorrect -> {
            val bonus = when {
                event.streak >= 10 -> XPValues.SURVIVOR_STREAK_10
                event.streak >= 5 -> XPValues.SURVIVOR_STREAK_5
                else -> 0
            }
            streakAward(XPValues.SURVIVOR_CORRECT, bonus, event.streak, R.string.xp_survived)
        }
        is XPEvent.SurvivorComplete -> RawXp(XPValues.SURVIVOR_COMPLETE, R.string.xp_survivor_complete)
        is XPEvent.ChaseComplete -> {
            val distanceBonus = (event.meters / 100 * XPValues.CHASE_PER_100M).coerceAtMost(XPValues.CHASE_DISTANCE_CAP)
            RawXp(XPValues.CHASE_COMPLETE + distanceBonus, R.string.xp_chase_escaped, " ⚡", titleArg = event.meters)
        }

        // Retention bonuses, deduplicated per calendar day.
        is XPEvent.DailyLogin -> RawXp(XPValues.DAILY_LOGIN, R.string.xp_daily_showup)
        is XPEvent.FirstGameOfDay ->
            if (profile.lastFirstGameXpDate == today) null
            else RawXp(XPValues.FIRST_GAME_OF_DAY, R.string.xp_first_game_today, " 🎮")
        is XPEvent.FirstExplorationOfDay ->
            if (profile.lastExplorationXpDate == today) null
            else RawXp(XPValues.FIRST_EXPLORATION_OF_DAY, R.string.xp_first_exploration_today, " 🔍")
        is XPEvent.EasterEggClaim ->
            if (profile.lastEasterEggXpDate == today) null
            else RawXp(XPValues.EASTER_EGG_CLAIM, R.string.xp_easter_egg, " ✨")
    }

    private fun guessStreakBonus(streak: Int) = when {
        streak >= 5 -> XPValues.GUESS_STREAK_5
        streak >= 2 -> XPValues.GUESS_STREAK_2
        else -> 0
    }

    private fun streakAward(base: Int, bonus: Int, streak: Int, @StringRes title: Int) =
        RawXp(base + bonus, title, if (bonus > 0) " 🔥 x$streak" else "")

    /** Says plainly why a number is bigger or smaller than usual — no hidden math. */
    private fun gameLabel(raw: RawXp, award: XPEconomy.Award): XPLabel = XPLabel(
        title = raw.title,
        titleArg = raw.titleArg,
        amount = award.total,
        flair = raw.flair,
        notes = buildList {
            addAll(raw.notes)
            if (award.restedBonus > 0) add(XPNote.RestedDouble)
            if (award.rate < 1f) add(XPNote.DailyRate((award.rate * 100).roundToInt()))
        }
    )

    private suspend fun applyXP(
        profile: UserProfile,
        gained: Int,
        label: XPLabel,
        extraUpdate: (UserProfile) -> UserProfile = { it }
    ): XPResult {
        val newTotal = profile.totalXp + gained
        val (newLevel, newCurrent, newNext) = LevelConfig.computeLevel(newTotal)
        val leveledUp = newLevel > profile.level

        // Same boundary as the resetWeeklyXp Cloud Function (Monday 00:00 IST) —
        // a rolling window here would let a stale local weeklyXp overwrite the
        // server reset on the next sync.
        val weekStart = WeeklyReset.startOfCurrentWeekMillis(System.currentTimeMillis())
        val shouldReset = profile.lastWeeklyReset < weekStart
        val baseWeeklyXp = if (shouldReset) 0 else profile.weeklyXp
        val newWeeklyResetTime = if (shouldReset) weekStart else profile.lastWeeklyReset

        val updated = extraUpdate(
            profile.copy(
                totalXp = newTotal, currentXp = newCurrent,
                nextLevelXp = newNext, level = newLevel, weeklyXp = baseWeeklyXp + gained,
                lastWeeklyReset = newWeeklyResetTime
            )
        )

        repository.saveProfile(updated)
        scheduleCloudSync(updated)

        return XPResult(
            xpGained = gained, newTotalXp = newTotal, newLevel = newLevel,
            newCurrentXp = newCurrent, newNextLevelXp = newNext,
            leveledUp = leveledUp, label = label
        )
    }

    /** Called under [awardLock]: replaces any not-yet-sent sync with this newer profile. */
    private fun scheduleCloudSync(profile: UserProfile) {
        if (profile.isGuest) return
        pendingSync?.cancel()
        pendingSync = syncScope.launch {
            delay(SYNC_DEBOUNCE_MS)
            repository.syncToFirestore(profile)
        }
    }

    private fun noOpResult(profile: UserProfile) = XPResult(
        xpGained = 0, newTotalXp = profile.totalXp, newLevel = profile.level,
        newCurrentXp = profile.currentXp, newNextLevelXp = profile.nextLevelXp,
        leveledUp = false, label = null
    )

    // SimpleDateFormat isn't thread-safe, so each call gets its own.
    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun today(): String = dateFormat().format(Date())

    /** Whole calendar days from [from] to [to] (yyyy-MM-dd); 0 when [from] is blank or unparseable. */
    private fun daysBetween(from: String, to: String): Int {
        if (from.isBlank()) return 0
        val format = dateFormat()
        val start = runCatching { format.parse(from) }.getOrNull() ?: return 0
        val end = runCatching { format.parse(to) }.getOrNull() ?: return 0
        // Rounded, not truncated: across a DST change two midnights are 23 or 25 hours apart.
        val dayMs = TimeUnit.DAYS.toMillis(1)
        return ((end.time - start.time + dayMs / 2) / dayMs).toInt().coerceAtLeast(0)
    }

    private companion object {
        const val SYNC_DEBOUNCE_MS = 3_000L
    }
}
