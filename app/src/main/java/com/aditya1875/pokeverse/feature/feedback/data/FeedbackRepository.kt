package com.aditya1875.pokeverse.feature.feedback.data

import android.content.Context
import android.util.Log
import com.aditya1875.pokeverse.BuildConfig
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.util.Locale

/** Ids must match FEEDBACK_WISHES in functions/index.js. */
enum class FeedbackWish(val id: String) {
    MORE_GAMES("more_games"),
    MULTIPLAYER("multiplayer"),
    TEAM_BUILDER("team_builder"),
    SHINY_HUNTING("shiny_hunting"),
    POKEMON_GO("pokemon_go"),
    COMPETITIVE("competitive"),
    REGIONAL_DEX("regional_dex"),
    MORE_LANGUAGES("more_languages"),
    WIDGETS("widgets"),
    OTHER("other")
}

class FeedbackRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val hasSubmitted: Boolean get() = prefs.getLong(KEY_LAST_SUBMIT_MS, 0L) > 0L

    /** One send per day keeps the form from being spammed. */
    val canSubmitNow: Boolean
        get() = System.currentTimeMillis() - prefs.getLong(KEY_LAST_SUBMIT_MS, 0L) >= SUBMIT_GAP_MS

    suspend fun submit(wishes: Set<FeedbackWish>, message: String): Boolean = try {
        FirebaseFunctions.getInstance()
            .getHttpsCallable("submitFeedback")
            .call(
                mapOf(
                    "wishes" to wishes.map { it.id },
                    "message" to message.trim().take(MAX_MESSAGE),
                    "appVersion" to BuildConfig.VERSION_NAME,
                    "locale" to Locale.getDefault().toLanguageTag()
                )
            )
            .await()
        prefs.edit().putLong(KEY_LAST_SUBMIT_MS, System.currentTimeMillis()).apply()
        true
    } catch (e: Exception) {
        Log.w("Feedback", "submitFeedback failed", e)
        false
    }

    /** True the first time only — the thank-you XP is a one-off. */
    fun claimThanksOnce(): Boolean {
        if (prefs.getBoolean(KEY_THANKED, false)) return false
        prefs.edit().putBoolean(KEY_THANKED, true).apply()
        return true
    }

    fun promptHistory(): Pair<Int, Long> =
        prefs.getInt(KEY_ASKS, 0) to prefs.getLong(KEY_LAST_ASK_MS, 0L)

    fun markPromptShown() {
        prefs.edit()
            .putInt(KEY_ASKS, prefs.getInt(KEY_ASKS, 0) + 1)
            .putLong(KEY_LAST_ASK_MS, System.currentTimeMillis())
            .apply()
    }

    companion object {
        const val MAX_MESSAGE = 1000
        private const val PREFS = "feedback"
        private const val KEY_LAST_SUBMIT_MS = "last_submit_ms"
        private const val KEY_THANKED = "thanks_xp_claimed"
        private const val KEY_ASKS = "prompt_asks"
        private const val KEY_LAST_ASK_MS = "prompt_last_ms"
        private const val SUBMIT_GAP_MS = 24L * 60 * 60 * 1000
    }
}
