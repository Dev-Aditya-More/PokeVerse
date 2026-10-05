package com.aditya1875.pokeverse.feature.game.chase.domain.engine

import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseConfig
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEntity
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEvent
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseStatus
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseStep
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseWorld
import com.aditya1875.pokeverse.feature.game.chase.domain.model.EntityKind
import com.aditya1875.pokeverse.feature.game.chase.domain.model.NetWarning
import com.aditya1875.pokeverse.feature.game.chase.domain.model.SpawnState
import kotlin.math.abs
import kotlin.random.Random

/**
 * The whole Rocket Chase simulation as pure functions: (world, input) -> new world.
 * No Android, Compose or coroutine imports, so it's deterministic under a seeded
 * [Random] and fully unit-testable. The ViewModel drives [step] once per frame.
 */
class ChaseEngine(
    val config: ChaseConfig = ChaseConfig(),
    random: Random = Random.Default
) {
    private val spawner = ChaseSpawner(config, random)
    private val middleLane = config.laneCount / 2

    fun newWorld(): ChaseWorld = ChaseWorld(
        targetLane = middleLane,
        playerX = middleLane.toFloat(),
        speed = config.startSpeed,
        lives = config.maxLives,
        maxLives = config.maxLives,
        spawn = SpawnState(nextNetInMs = spawner.netInterval(0f))
    )

    fun moveLane(world: ChaseWorld, direction: Int): ChaseWorld {
        if (world.status != ChaseStatus.Running) return world
        val target = (world.targetLane + direction).coerceIn(0, config.laneCount - 1)
        return world.copy(targetLane = target)
    }

    fun step(world: ChaseWorld, frameMs: Long): ChaseStep {
        if (world.status != ChaseStatus.Running) return ChaseStep(world)

        val dtMs = frameMs.coerceIn(0L, config.maxFrameMs)
        val dt = dtMs / 1000f
        val difficulty = (world.meters / config.rampMeters).coerceIn(0f, 1f)
        val speed = lerp(config.startSpeed, config.maxSpeed, difficulty)
        val scroll = speed * dt
        val metersRun = scroll * config.metersPerScreen
        val events = mutableListOf<ChaseEvent>()

        var next = world.copy(
            speed = speed,
            meters = world.meters + metersRun,
            metersSinceHit = world.metersSinceHit + metersRun,
            elapsedMs = world.elapsedMs + dtMs,
            invulnerableMs = (world.invulnerableMs - dtMs).coerceAtLeast(0L),
            thunderFlashMs = (world.thunderFlashMs - dtMs).coerceAtLeast(0L),
            playerX = slideToward(world.playerX, world.targetLane.toFloat(), config.laneSwitchSpeed * dt),
            entities = world.entities
                .map { it.copy(y = it.y + scroll) }
                .filter { it.y < config.despawnY }
        )
        next = spawnRows(next, scroll, difficulty)
        next = updateNets(next, dtMs, difficulty, events)
        next = resolveCollisions(next, events)
        next = shakeOffRocket(next, events)

        if (next.lives <= 0) {
            next = next.copy(lives = 0, status = ChaseStatus.Caught, warnings = emptyList())
            events += ChaseEvent.Caught
        }
        return ChaseStep(next, events)
    }

    /** Clears every obstacle on screen and any incoming net. Needs a full charge. */
    fun thunderbolt(world: ChaseWorld): ChaseStep {
        if (!world.canThunderbolt) return ChaseStep(world)
        val (cleared, kept) = world.entities.partition { it.kind.isObstacle && it.y in -0.05f..1f }
        val next = world.copy(
            entities = kept,
            warnings = emptyList(),
            charge = 0f,
            bonusPoints = world.bonusPoints + cleared.size * config.thunderClearPoints,
            thunderFlashMs = config.thunderFlashMs
        )
        return ChaseStep(next, listOf(ChaseEvent.ThunderUsed(cleared.size)))
    }

    fun canRevive(world: ChaseWorld): Boolean =
        world.status == ChaseStatus.Caught && world.revivesUsed < config.maxRevives

    /** Back in the race with one life, a clear track and a short grace period. */
    fun revive(world: ChaseWorld): ChaseWorld {
        if (!canRevive(world)) return world
        val difficulty = (world.meters / config.rampMeters).coerceIn(0f, 1f)
        return world.copy(
            status = ChaseStatus.Running,
            lives = 1,
            invulnerableMs = config.reviveInvulnerableMs,
            entities = world.entities.filterNot { it.kind.isObstacle },
            warnings = emptyList(),
            metersSinceHit = 0f,
            revivesUsed = world.revivesUsed + 1,
            spawn = world.spawn.copy(rowProgress = 0f, nextNetInMs = spawner.netInterval(difficulty))
        )
    }

    // ── Step phases ─────────────────────────────────────────────────────────

    private fun spawnRows(world: ChaseWorld, scroll: Float, difficulty: Float): ChaseWorld {
        val progress = world.spawn.rowProgress + scroll
        val gap = spawner.rowGap(difficulty)
        if (progress < gap) return world.copy(spawn = world.spawn.copy(rowProgress = progress))

        val (row, spawn) = spawner.spawnRow(world.spawn, difficulty)
        return world.copy(
            entities = world.entities + row,
            spawn = spawn.copy(rowProgress = progress - gap)
        )
    }

    private fun updateNets(
        world: ChaseWorld,
        dtMs: Long,
        difficulty: Float,
        events: MutableList<ChaseEvent>
    ): ChaseWorld {
        val (landed, pending) = world.warnings
            .map { it.copy(remainingMs = it.remainingMs - dtMs) }
            .partition { it.remainingMs <= 0L }

        var next = world.copy(warnings = pending)
        landed.forEach { net ->
            next = next.withEntity(EntityKind.LANDED_NET, net.lane, config.playerY)
            if (isOnLane(next.playerX, net.lane) && next.invulnerableMs == 0L) {
                next = hit(next, events)
            } else {
                events += ChaseEvent.NetDodged
            }
        }

        if (next.meters < config.netsStartMeters) return next
        val netIn = next.spawn.nextNetInMs - dtMs
        if (netIn > 0L) return next.copy(spawn = next.spawn.copy(nextNetInMs = netIn))

        events += ChaseEvent.NetIncoming
        return next.copy(
            // Aimed where Pikachu is heading, so it can't be dodged by just finishing a slide.
            warnings = next.warnings + NetWarning(next.targetLane, config.netWarningMs, config.netWarningMs),
            spawn = next.spawn.copy(nextNetInMs = spawner.netInterval(difficulty))
        )
    }

    private fun resolveCollisions(world: ChaseWorld, events: MutableList<ChaseEvent>): ChaseWorld {
        val touching = world.entities.filter {
            it.kind != EntityKind.LANDED_NET &&
                abs(it.y - config.playerY) < config.hitRangeY &&
                isOnLane(world.playerX, it.lane)
        }
        if (touching.isEmpty()) return world

        var next = world
        val consumed = mutableSetOf<Long>()
        touching.forEach { entity ->
            when {
                entity.kind.isPickup -> {
                    next = collect(next, entity.kind)
                    consumed += entity.id
                    events += ChaseEvent.Collected(entity.kind)
                }
                // Obstacles pass straight through Pikachu while it's blinking.
                next.invulnerableMs == 0L -> {
                    next = hit(next, events)
                    consumed += entity.id
                }
            }
        }
        return next.copy(entities = next.entities.filterNot { it.id in consumed })
    }

    private fun shakeOffRocket(world: ChaseWorld, events: MutableList<ChaseEvent>): ChaseWorld {
        if (world.lives >= world.maxLives || world.metersSinceHit < config.lifeRegenMeters) return world
        events += ChaseEvent.ShookOffRocket
        return world.copy(lives = world.lives + 1, metersSinceHit = 0f)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun hit(world: ChaseWorld, events: MutableList<ChaseEvent>): ChaseWorld {
        val lives = world.lives - 1
        events += ChaseEvent.Hit(livesLeft = lives)
        return world.copy(lives = lives, invulnerableMs = config.invulnerableMs, metersSinceHit = 0f)
    }

    private fun collect(world: ChaseWorld, kind: EntityKind): ChaseWorld = when (kind) {
        EntityKind.THUNDER_STONE -> world.copy(
            charge = 1f,
            bonusPoints = world.bonusPoints + config.berryPoints
        )
        else -> world.copy(
            berries = world.berries + 1,
            charge = (world.charge + config.berryCharge).coerceAtMost(1f),
            bonusPoints = world.bonusPoints + config.berryPoints
        )
    }

    private fun ChaseWorld.withEntity(kind: EntityKind, lane: Int, y: Float): ChaseWorld {
        val id = spawn.nextEntityId
        return copy(
            entities = entities + ChaseEntity(id, kind, lane, y),
            spawn = spawn.copy(nextEntityId = id + 1)
        )
    }

    private fun isOnLane(playerX: Float, lane: Int) = abs(playerX - lane) < config.laneHitTolerance

    private fun slideToward(current: Float, target: Float, maxDelta: Float): Float = when {
        abs(target - current) <= maxDelta -> target
        target > current -> current + maxDelta
        else -> current - maxDelta
    }

    private fun lerp(from: Float, to: Float, t: Float) = from + (to - from) * t
}
