package com.aditya1875.pokeverse.feature.game.battle.domain

import kotlin.random.Random

/**
 * Quick reactions in Team Battle: a small set of emoji a player can pop into a speech bubble over their side
 * of the arena. Emoji only, so they read the same in every language and can't carry a message.
 *
 * Against a real rival they travel through the room; against the stand-in rival (or in Practice) the rival
 * reacts by itself to how the fight is going, sparingly, so it feels like someone is on the other side.
 */
object Reactions {
    val ALL = listOf("👋", "👍", "🔥", "😮", "😅", "😬", "😎", "🤝")

    const val WAVE = 0
    const val THUMBS = 1
    const val FIRE = 2
    const val WOW = 3
    const val OOPS = 4
    const val YIKES = 5
    const val COOL = 6
    const val GG = 7

    /** How long a bubble stays up. */
    const val SHOW_MS = 2_600L

    /** Shortest gap between two reactions you send, so nobody can flood the other screen. */
    const val SEND_GAP_MS = 2_500L

    /** Shortest gap between two reactions the stand-in rival makes on its own. */
    const val AUTO_GAP_MS = 6_000L

    fun emoji(index: Int): String? = ALL.getOrNull(index)

    /** Moments in a fight a rival might react to, seen from the rival's side. */
    enum class Moment { START, RIVAL_HIT_HARD, RIVAL_TOOK_HARD, RIVAL_LOST_ONE, RIVAL_KO_ONE, RIVAL_LOSING_LAST, YOU_REACTED }

    /** What the stand-in rival says about [moment], or null to stay quiet (most of the time). */
    fun auto(moment: Moment, random: Random = Random.Default): Int? {
        val (chance, options) = when (moment) {
            Moment.START -> 0.55 to listOf(WAVE, THUMBS)
            Moment.RIVAL_HIT_HARD -> 0.45 to listOf(FIRE, COOL)
            Moment.RIVAL_TOOK_HARD -> 0.40 to listOf(WOW, YIKES)
            Moment.RIVAL_LOST_ONE -> 0.50 to listOf(OOPS, YIKES, WOW)
            Moment.RIVAL_KO_ONE -> 0.45 to listOf(COOL, FIRE, THUMBS)
            Moment.RIVAL_LOSING_LAST -> 0.70 to listOf(YIKES, GG)
            Moment.YOU_REACTED -> 0.35 to listOf(THUMBS, WAVE, OOPS)
        }
        if (random.nextDouble() >= chance) return null
        return options.random(random)
    }
}
