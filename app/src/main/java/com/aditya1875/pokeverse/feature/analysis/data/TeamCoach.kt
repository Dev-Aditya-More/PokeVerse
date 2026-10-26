package com.aditya1875.pokeverse.feature.analysis.data

import android.content.Context
import androidx.core.content.edit
import com.aditya1875.pokeverse.feature.analysis.presentation.components.TeamAnalysis
import com.aditya1875.pokeverse.feature.analysis.presentation.components.TeamMemberWithTypes
import com.aditya1875.pokeverse.utils.LocaleHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface CoachResult {
    data class Success(val text: String, val remaining: Int) : CoachResult
    data object LimitReached : CoachResult
    data object Failed : CoachResult
}

/**
 * "Coach's Read": a short, personal take on the team written by Gemini Flash.
 *
 * [com.aditya1875.pokeverse.feature.analysis.presentation.components.TeamAnalyzer] stays the source of
 * truth for every number and type matchup; the model is only handed those finished facts and asked to
 * talk about them, so it can't invent matchups. Reads are capped per day (a failed call costs nothing)
 * and cached per team + language, so reopening the screen never spends one.
 *
 * The limit lives on the device, which is fine for a free perk. Hard enforcement would need a
 * Cloud Function in front of the model.
 */
class TeamCoach(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("team_coach", Context.MODE_PRIVATE)

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL,
            generationConfig = generationConfig { temperature = 0.7f },
            systemInstruction = content { text(SYSTEM_PROMPT) },
        )
    }

    fun remainingToday(): Int = (DAILY_LIMIT - usedToday()).coerceAtLeast(0)

    /** A previously generated read for this exact team, in the current language, or null. */
    fun cached(team: List<TeamMemberWithTypes>): String? =
        prefs.getString(cacheKey(team), null)

    suspend fun read(team: List<TeamMemberWithTypes>, analysis: TeamAnalysis): CoachResult {
        cached(team)?.let { return CoachResult.Success(it, remainingToday()) }
        if (remainingToday() <= 0) return CoachResult.LimitReached

        return try {
            val text = withContext(Dispatchers.IO) {
                withTimeout(TIMEOUT_MS) {
                    model.generateContent(buildPrompt(team, analysis)).text?.trim()
                }
            }
            if (text.isNullOrBlank()) return CoachResult.Failed
            val clean = text.replace("**", "").replace("*", "")
            spendOne()
            remember(team, clean)
            CoachResult.Success(clean, remainingToday())
        } catch (_: TimeoutCancellationException) {
            CoachResult.Failed
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            CoachResult.Failed
        }
    }

    private fun buildPrompt(team: List<TeamMemberWithTypes>, analysis: TeamAnalysis): String {
        val roster = team.joinToString("; ") { "${it.name} (${it.types.joinToString("/")})" }
        val weak = analysis.defensiveWeaknesses.entries
            .filter { it.value.size >= 2 }
            .sortedByDescending { it.value.size }
            .take(4)
            .joinToString("; ") { "${it.key} hits ${it.value.size} members (${it.value.joinToString(", ")})" }
            .ifBlank { "none shared by 2+ members" }
        val blind = analysis.offensiveCoverage.filterValues { it == 0 }.keys.joinToString(", ")
            .ifBlank { "none" }
        val resists = analysis.resistances.entries
            .filter { it.value.size >= 2 }
            .sortedByDescending { it.value.size }
            .take(4)
            .joinToString(", ") { it.key }
            .ifBlank { "none" }

        return buildString {
            appendLine("Team (${team.size}): $roster")
            appendLine("Coverage score: ${analysis.coverageScore}/100")
            appendLine("Biggest weaknesses: $weak")
            appendLine("Types the team cannot hit super-effectively: $blind")
            appendLine("Types the team resists well: $resists")
            append("Write the coach's read in ${languageName()}.")
        }
    }

    private fun languageName(): String = when (LocaleHelper.getSelectedLanguage(appContext)) {
        LocaleHelper.LANG_PORTUGUESE_BR -> "Brazilian Portuguese"
        LocaleHelper.LANG_FRENCH -> "French"
        // The app's Hindi copy is casual Hinglish: Devanagari with common game words kept in English.
        LocaleHelper.LANG_HINDI -> "casual Hindi in Devanagari script, keeping gaming words like team, type and Pokémon names in English"
        else -> "English"
    }

    private fun cacheKey(team: List<TeamMemberWithTypes>): String {
        val roster = team.map { it.name }.sorted().joinToString(",")
        val lang = LocaleHelper.getSelectedLanguage(appContext)
        return "read_" + "$lang|$roster".hashCode().toUInt().toString(16)
    }

    private fun remember(team: List<TeamMemberWithTypes>, text: String) {
        prefs.edit {
            // Cap the cache; a handful of recent teams is all anyone revisits.
            val stale = prefs.all.keys.filter { it.startsWith("read_") }
            if (stale.size >= MAX_CACHED) stale.forEach { remove(it) }
            putString(cacheKey(team), text)
        }
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun usedToday(): Int =
        if (prefs.getString(KEY_DAY, null) == today()) prefs.getInt(KEY_USED, 0) else 0

    private fun spendOne() {
        val used = usedToday() + 1
        prefs.edit {
            putString(KEY_DAY, today())
            putInt(KEY_USED, used)
        }
    }

    companion object {
        const val DAILY_LIMIT = 5

        // Stable (not a short-term-availability) Flash model; see Firebase AI Logic "supported models".
        private const val MODEL = "gemini-3.5-flash"
        private const val TIMEOUT_MS = 25_000L
        private const val MAX_CACHED = 20
        private const val KEY_DAY = "day"
        private const val KEY_USED = "used"

        private const val SYSTEM_PROMPT = """
You are a friendly, upbeat Pokémon team coach inside a mobile app.
You are given pre-computed, authoritative facts about the player's team. Never contradict them and never invent type matchups beyond them.
Write at most 70 words as plain text: no markdown, no lists, no emoji.
Shape: one sentence of verdict that names a standout team member; one sentence on the biggest risk; one concrete tip, which may name up to two real Pokémon that would fix it.
Only mention Pokémon that really exist. Keep Pokémon names as they are in English.
"""
    }
}
