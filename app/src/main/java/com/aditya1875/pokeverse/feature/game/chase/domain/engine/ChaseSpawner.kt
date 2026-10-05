package com.aditya1875.pokeverse.feature.game.chase.domain.engine

import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseConfig
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEntity
import com.aditya1875.pokeverse.feature.game.chase.domain.model.EntityKind
import com.aditya1875.pokeverse.feature.game.chase.domain.model.SpawnState
import kotlin.math.abs
import kotlin.random.Random

/**
 * Decides what each new obstacle row looks like. Its one hard rule: a row never
 * blocks every lane, and a two-lane block always leaves its gap within one
 * lane-switch of the previous row's gap — so every run is always survivable.
 */
class ChaseSpawner(
    private val config: ChaseConfig,
    private val random: Random
) {
    fun rowGap(difficulty: Float): Float =
        lerp(config.rowGapStart, config.rowGapMin, difficulty)

    fun netInterval(difficulty: Float): Long =
        lerp(config.netIntervalStartMs.toFloat(), config.netIntervalMinMs.toFloat(), difficulty).toLong()

    /** Builds one row at the top of the track and returns the updated spawn bookkeeping. */
    fun spawnRow(spawn: SpawnState, difficulty: Float): Pair<List<ChaseEntity>, SpawnState> {
        val lanes = 0 until config.laneCount
        val blockTwo = random.nextFloat() < config.doubleBlockChanceMax * difficulty
        val freeLanes = if (blockTwo) setOf(pickReachableGap(spawn.lastFreeLanes)) else {
            val blocked = lanes.random(random)
            lanes.toSet() - blocked
        }

        var nextId = spawn.nextEntityId
        val row = buildList {
            (lanes.toSet() - freeLanes).forEach { lane ->
                add(ChaseEntity(nextId++, EntityKind.obstacles.random(random), lane, config.spawnY))
            }
            if (random.nextFloat() < config.pickupChance) {
                val kind = if (random.nextFloat() < config.thunderStoneChance) EntityKind.THUNDER_STONE else EntityKind.ORAN_BERRY
                add(ChaseEntity(nextId++, kind, freeLanes.random(random), config.spawnY))
            }
        }
        return row to spawn.copy(lastFreeLanes = freeLanes, nextEntityId = nextId)
    }

    /** Agility's reward: a row of berries (sometimes a Thunder Stone) and no obstacles. */
    fun spawnPickupRain(spawn: SpawnState): Pair<List<ChaseEntity>, SpawnState> {
        val lanes = (0 until config.laneCount).shuffled(random).take(config.agilityPickupsPerRow)
        var nextId = spawn.nextEntityId
        val row = lanes.map { lane ->
            val kind = if (random.nextFloat() < config.agilityThunderStoneChance) EntityKind.THUNDER_STONE else EntityKind.ORAN_BERRY
            ChaseEntity(nextId++, kind, lane, config.spawnY)
        }
        // Every lane is open, so the next obstacle row is free to put its gap anywhere.
        return row to spawn.copy(lastFreeLanes = (0 until config.laneCount).toSet(), nextEntityId = nextId)
    }

    private fun pickReachableGap(previousGaps: Set<Int>): Int {
        val lanes = 0 until config.laneCount
        if (previousGaps.isEmpty()) return lanes.random(random)
        return lanes.filter { lane -> previousGaps.any { abs(it - lane) <= 1 } }.random(random)
    }

    private fun lerp(from: Float, to: Float, t: Float) = from + (to - from) * t
}
