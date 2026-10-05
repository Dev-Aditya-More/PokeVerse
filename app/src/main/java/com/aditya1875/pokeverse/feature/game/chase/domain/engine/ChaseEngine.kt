package com.aditya1875.pokeverse.feature.game.chase.domain.engine

import com.aditya1875.pokeverse.feature.game.chase.domain.model.Agility
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseConfig
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEntity
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEvent
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseStatus
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseStep
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseWorld
import com.aditya1875.pokeverse.feature.game.chase.domain.model.EntityKind
import com.aditya1875.pokeverse.feature.game.chase.domain.model.NetWarning
import com.aditya1875.pokeverse.feature.game.chase.domain.model.Snare
import com.aditya1875.pokeverse.feature.game.chase.domain.model.SpawnState
import com.aditya1875.pokeverse.feature.game.chase.domain.model.Strike
import kotlin.math.abs
import kotlin.math.exp
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
        agilityCharges = config.agilityFreeCharges,
        spawn = SpawnState(nextNetInMs = spawner.netInterval(0f))
    )

    // ── Input ───────────────────────────────────────────────────────────────

    /** Sends Pikachu toward [lane]. Ignored while it's tangled in a net. */
    fun steerTo(world: ChaseWorld, lane: Int): ChaseWorld {
        if (world.status != ChaseStatus.Running || world.isSnared) return world
        return world.copy(targetLane = lane.coerceIn(0, config.laneCount - 1))
    }

    /** One tap's worth of struggling against a net. Does nothing when Pikachu isn't snared. */
    fun struggle(world: ChaseWorld): ChaseStep {
        val snare = world.snare ?: return ChaseStep(world)
        if (world.status != ChaseStatus.Running) return ChaseStep(world)
        val left = snare.strugglesLeft - 1
        return if (left <= 0) {
            ChaseStep(world.copy(snare = null), listOf(ChaseEvent.BrokeFree))
        } else {
            ChaseStep(world.copy(snare = snare.copy(strugglesLeft = left)), listOf(ChaseEvent.Struggled(left)))
        }
    }

    fun canUseAgility(world: ChaseWorld): Boolean =
        world.status == ChaseStatus.Running && !world.isAgile && world.agilityCharges > 0

    /** Spends a charge: a 10 s dash that zaps attackers one by one and rains berries. Also breaks a snare. */
    fun useAgility(world: ChaseWorld): ChaseStep {
        if (!canUseAgility(world)) return ChaseStep(world)
        val next = world.copy(
            agility = Agility(config.agilityMs, config.agilityMs),
            agilityCharges = world.agilityCharges - 1,
            snare = null,
            warnings = emptyList()
        )
        val events = buildList {
            add(ChaseEvent.AgilityStarted)
            if (world.isSnared) add(ChaseEvent.BrokeFree)
        }
        return ChaseStep(next, events)
    }

    /** Clears every attacker on screen and any incoming net, and zaps Pikachu out of a snare. Needs a full meter. */
    fun thunderbolt(world: ChaseWorld): ChaseStep {
        if (!world.canThunderbolt) return ChaseStep(world)
        val (cleared, kept) = world.entities.partition { it.kind.isObstacle && it.y in -0.05f..1f }
        val next = world.copy(
            entities = kept,
            warnings = emptyList(),
            snare = null,
            charge = 0f,
            bonusPoints = world.bonusPoints + cleared.size * config.thunderClearPoints,
            thunderFlashMs = config.thunderFlashMs
        )
        val events = buildList {
            add(ChaseEvent.ThunderUsed(cleared.size))
            if (world.isSnared) add(ChaseEvent.BrokeFree)
        }
        return ChaseStep(next, events)
    }

    /** True while this run can still earn an extra Agility (rewarded ad / premium). */
    fun canEarnAgility(world: ChaseWorld): Boolean = world.agilityExtrasEarned < config.agilityMaxExtra

    fun grantAgility(world: ChaseWorld): ChaseWorld {
        if (!canEarnAgility(world)) return world
        return world.copy(
            agilityCharges = world.agilityCharges + 1,
            agilityExtrasEarned = world.agilityExtrasEarned + 1
        )
    }

    fun canRevive(world: ChaseWorld): Boolean =
        world.status == ChaseStatus.Caught && world.revivesUsed < config.maxRevives

    /** Back in the race with one life, a clear track and a short grace period. */
    fun revive(world: ChaseWorld): ChaseWorld {
        if (!canRevive(world)) return world
        return world.copy(
            status = ChaseStatus.Running,
            lives = 1,
            invulnerableMs = config.reviveInvulnerableMs,
            entities = world.entities.filterNot { it.kind.isObstacle },
            warnings = emptyList(),
            snare = null,
            agility = null,
            metersSinceHit = 0f,
            revivesUsed = world.revivesUsed + 1,
            spawn = world.spawn.copy(rowProgress = 0f, nextNetInMs = spawner.netInterval(difficultyOf(world)))
        )
    }

    // ── Simulation ──────────────────────────────────────────────────────────

    fun step(world: ChaseWorld, frameMs: Long): ChaseStep {
        if (world.status != ChaseStatus.Running) return ChaseStep(world)

        val dtMs = frameMs.coerceIn(0L, config.maxFrameMs)
        val dt = dtMs / 1000f
        val difficulty = difficultyOf(world)
        val baseSpeed = lerp(config.startSpeed, config.maxSpeed, difficulty)
        val speed = if (world.isAgile) baseSpeed * config.agilitySpeedBoost else baseSpeed
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
            strikes = world.strikes
                .map { it.copy(remainingMs = it.remainingMs - dtMs, y = it.y + scroll) }
                .filter { it.remainingMs > 0L },
            playerX = if (world.isSnared) world.playerX else slide(world.playerX, world.targetLane.toFloat(), dt),
            entities = world.entities
                .map { it.copy(y = it.y + scroll) }
                .filter { it.y < config.despawnY }
        )
        next = spawnRows(next, scroll, difficulty)
        next = tickSnare(next, dtMs, events)
        next = tickAgility(next, dtMs, events)
        next = updateNets(next, dtMs, difficulty, events)
        next = resolveCollisions(next, events)
        next = shakeOffRocket(next, events)

        if (next.lives <= 0) {
            next = next.copy(lives = 0, status = ChaseStatus.Caught, warnings = emptyList(), snare = null, agility = null)
            events += ChaseEvent.Caught
        }
        return ChaseStep(next, events)
    }

    private fun spawnRows(world: ChaseWorld, scroll: Float, difficulty: Float): ChaseWorld {
        val progress = world.spawn.rowProgress + scroll
        val gap = spawner.rowGap(difficulty)
        if (progress < gap) return world.copy(spawn = world.spawn.copy(rowProgress = progress))

        val (row, spawn) = if (world.isAgile) spawner.spawnPickupRain(world.spawn)
        else spawner.spawnRow(world.spawn, difficulty)
        return world.copy(
            entities = world.entities + row,
            spawn = spawn.copy(rowProgress = progress - gap)
        )
    }

    /** Running out the snare timer means Team Rocket reeled Pikachu in: lose a life, net breaks. */
    private fun tickSnare(world: ChaseWorld, dtMs: Long, events: MutableList<ChaseEvent>): ChaseWorld {
        val snare = world.snare ?: return world
        val remaining = snare.remainingMs - dtMs
        if (remaining > 0L) return world.copy(snare = snare.copy(remainingMs = remaining))
        return hit(world.copy(snare = null), events)
    }

    /** The dash: zaps the nearest attacker ahead every strike interval, then a brief grace blink. */
    private fun tickAgility(world: ChaseWorld, dtMs: Long, events: MutableList<ChaseEvent>): ChaseWorld {
        val agility = world.agility ?: return world
        val remaining = agility.remainingMs - dtMs
        if (remaining <= 0L) {
            events += ChaseEvent.AgilityEnded
            // A short grace blink so the first post-dash row can't ambush Pikachu.
            return world.copy(agility = null, invulnerableMs = maxOf(world.invulnerableMs, AGILITY_EXIT_GRACE_MS))
        }

        var next = world
        var nextStrike = agility.nextStrikeMs - dtMs
        if (nextStrike <= 0L) {
            val target = world.entities
                .filter { it.kind.isObstacle && it.y in VISIBLE_TOP..(config.playerY + config.hitRangeY) }
                .maxByOrNull { it.y } // nearest to Pikachu first
            if (target != null) {
                next = zap(next, target, events)
                nextStrike = config.agilityStrikeIntervalMs
            } else {
                nextStrike = 0L // nothing in range — strike the moment something appears
            }
        }
        return next.copy(agility = agility.copy(remainingMs = remaining, nextStrikeMs = nextStrike))
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
            val caughtInIt = isOnLane(next.playerX, net.lane) && !next.isProtected && !next.isSnared
            if (caughtInIt) {
                // The net pins Pikachu in place — it stays drawn over Pikachu instead of scrolling away.
                next = next.copy(
                    snare = Snare(config.snareMs, config.snareMs, config.snareStruggles),
                    targetLane = net.lane,
                    playerX = net.lane.toFloat()
                )
                events += ChaseEvent.Snared
            } else {
                next = next.withEntity(EntityKind.LANDED_NET, net.lane, config.playerY)
                events += ChaseEvent.NetDodged
            }
        }

        // No new net while Pikachu is tangled or dashing — one thing at a time.
        if (next.meters < config.netsStartMeters || next.isSnared || next.isAgile) return next
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
        val touching = world.entities.filter { entity ->
            if (entity.kind == EntityKind.LANDED_NET) return@filter false
            val distanceY = abs(entity.y - config.playerY)
            // While dashing, pickups are pulled in from every lane.
            val magnetised = world.isAgile && entity.kind.isPickup && distanceY < config.agilityMagnetRangeY
            magnetised || (distanceY < config.hitRangeY && isOnLane(world.playerX, entity.lane))
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
                next.isAgile -> {
                    next = zap(next, entity, events)
                }
                // Obstacles pass straight through while blinking.
                !next.isProtected -> {
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

    private fun zap(world: ChaseWorld, target: ChaseEntity, events: MutableList<ChaseEvent>): ChaseWorld {
        events += ChaseEvent.Zapped(target.kind)
        return world.copy(
            entities = world.entities.filterNot { it.id == target.id },
            bonusPoints = world.bonusPoints + config.agilityZapPoints,
            strikes = world.strikes + Strike(target.lane, target.y, config.strikeFlashMs)
        )
    }

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

    /**
     * Eases toward the target lane (fast start, soft landing) but never faster than
     * [ChaseConfig.laneSwitchSpeed], so a far-lane tap reads as a dash, not a teleport.
     */
    private fun slide(current: Float, target: Float, dt: Float): Float {
        val eased = (target - current) * (1f - exp(-config.laneEasing * dt))
        val maxStep = config.laneSwitchSpeed * dt
        val next = current + eased.coerceIn(-maxStep, maxStep)
        return if (abs(target - next) < 0.01f) target else next
    }

    private fun isOnLane(playerX: Float, lane: Int) = abs(playerX - lane) < config.laneHitTolerance

    private fun difficultyOf(world: ChaseWorld) = (world.meters / config.rampMeters).coerceIn(0f, 1f)

    private fun lerp(from: Float, to: Float, t: Float) = from + (to - from) * t

    private companion object {
        /** Attackers above this (still off the top edge) can't be targeted yet. */
        const val VISIBLE_TOP = -0.02f
        const val AGILITY_EXIT_GRACE_MS = 1_000L
    }
}
