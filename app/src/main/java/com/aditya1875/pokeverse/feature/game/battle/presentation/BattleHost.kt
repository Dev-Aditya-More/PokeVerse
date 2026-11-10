package com.aditya1875.pokeverse.feature.game.battle.presentation

import com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * What the shared battle arena needs from whoever runs the battle: a practice battle against the bot
 * ([BattleViewModel]) or an online one against another trainer ([OnlineBattleViewModel]). The arena draws
 * and animates; the host decides what happens.
 */
interface BattleHost {
    /** What happened in the latest turn, in order, for the arena to play back. */
    val events: SharedFlow<List<BattleEvent>>

    /** Changes with every new battle, so the arena throws away the last one's animation state. */
    val battleKey: StateFlow<Int>

    val shardsGained: StateFlow<Int>
    val shardsCapped: StateFlow<Boolean>

    /** Extras that only an online battle has; a practice battle leaves everything at its default. */
    val hud: StateFlow<ArenaHud>

    fun act(action: BattleAction)
    fun chooseReplacement(index: Int)

    /** The arena has finished playing back what it was given, so any turn clock may now start. */
    fun playbackFinished() {}
}

/** The online-only parts of the arena: who you face, the clock, and what you are waiting for. */
data class ArenaHud(
    val online: Boolean = false,
    val opponentName: String? = null,
    /** Seconds left to choose, or null when it is not your turn to choose. */
    val secondsLeft: Int? = null,
    /** Your choice is in (or only your rival has to choose), so the buttons stay off. */
    val waiting: Boolean = false,
    /** The rival has locked in their move for this turn. */
    val opponentLocked: Boolean = false,
    /** The rival has not been heard from for a while. */
    val opponentAway: Boolean = false,
    /** The battle ended because the rival left or stopped answering. */
    val opponentLeft: Boolean = false,
    /** Turns the clock has played for you so far; at the limit you are disqualified. */
    val myMissedTurns: Int = 0,
    /** You were disqualified for missing too many turns. */
    val iWasDisqualified: Boolean = false,
    /** The rival was disqualified for missing too many turns, so the win is yours. */
    val opponentDisqualified: Boolean = false,
    /** XP and shards were skipped for this battle (too short, or this rival has paid out enough today). */
    val rewardsLimited: Boolean = false,
    /** XP this battle paid, for the result card. */
    val xpGained: Int = 0,
)
