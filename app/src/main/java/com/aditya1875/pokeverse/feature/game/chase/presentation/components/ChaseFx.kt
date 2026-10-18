package com.aditya1875.pokeverse.feature.game.chase.presentation.components

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseConfig
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEvent
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseWorld
import com.aditya1875.pokeverse.feature.game.chase.domain.model.EntityKind

/**
 * Short-lived visual effects spawned from engine events (pickup bursts, "+10"
 * pop-ups, impact rings). Presentation-only state: timed against the world's
 * own clock, so they freeze with the game when it's paused.
 */
internal class ChaseFx {

    data class Burst(
        val lane: Float,
        val y: Float,
        val atMs: Long,
        val color: Color,
        val label: String?,
        val ring: Boolean,
        val sparks: Int
    )

    val bursts = mutableStateListOf<Burst>()

    fun onEvent(event: ChaseEvent, world: ChaseWorld, config: ChaseConfig) {
        val now = world.elapsedMs
        bursts.removeAll { now - it.atMs > LIFETIME_MS }
        val atPikachu = world.playerX to config.playerY
        val burst = when (event) {
            is ChaseEvent.Collected -> when (event.kind) {
                EntityKind.ORAN_BERRY -> Burst(atPikachu.first, atPikachu.second, now, BerryBlue, "+${config.berryPoints}", ring = false, sparks = 8)
                EntityKind.THUNDER_STONE -> Burst(atPikachu.first, atPikachu.second, now, StoneGreen, "⚡ MAX", ring = true, sparks = 12)
                else -> null
            }
            is ChaseEvent.Hit -> Burst(atPikachu.first, atPikachu.second, now, HitRed, null, ring = true, sparks = 10)
            ChaseEvent.BrokeFree, ChaseEvent.ShookOffRocket ->
                Burst(atPikachu.first, atPikachu.second, now, Color.White, null, ring = true, sparks = 10)
            ChaseEvent.AgilityStarted -> Burst(atPikachu.first, atPikachu.second, now, Spark, null, ring = true, sparks = 14)
            is ChaseEvent.ThunderUsed -> if (event.cleared > 0)
                Burst(atPikachu.first, atPikachu.second - 0.08f, now, Spark, "+${event.cleared * config.thunderClearPoints}", ring = false, sparks = 0)
            else null
            else -> null
        }
        burst?.let { bursts += it }
    }

    fun clear() = bursts.clear()

    companion object {
        const val LIFETIME_MS = 750L
        val BerryBlue = Color(0xFF4FC3F7)
        val StoneGreen = Color(0xFF69F0AE)
        val HitRed = Color(0xFFFF5252)
        val Spark = Color(0xFFFFEB3B)
    }
}
