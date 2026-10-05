package com.aditya1875.pokeverse.feature.game.chase

import com.aditya1875.pokeverse.feature.game.chase.domain.engine.ChaseEngine
import com.aditya1875.pokeverse.feature.game.chase.domain.engine.ChaseSpawner
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseConfig
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEntity
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseEvent
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseStatus
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseWorld
import com.aditya1875.pokeverse.feature.game.chase.domain.model.EntityKind
import com.aditya1875.pokeverse.feature.game.chase.domain.model.NetWarning
import com.aditya1875.pokeverse.feature.game.chase.domain.model.SpawnState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class ChaseEngineTest {

    /** No random spawns or nets, so each test controls exactly what's on the track. */
    private val quietConfig = ChaseConfig(
        rowGapStart = 1_000f,
        rowGapMin = 1_000f,
        netsStartMeters = Float.MAX_VALUE
    )
    private val engine = ChaseEngine(quietConfig, Random(42))

    private fun worldWith(vararg entities: ChaseEntity): ChaseWorld =
        engine.newWorld().copy(entities = entities.toList(), spawn = SpawnState(nextEntityId = 100))

    private fun obstacleAt(lane: Int, id: Long = 1) =
        ChaseEntity(id, EntityKind.EKANS, lane, quietConfig.playerY)

    @Test
    fun `new run starts in the middle lane with full lives`() {
        val world = engine.newWorld()
        assertEquals(1, world.targetLane)
        assertEquals(1f, world.playerX)
        assertEquals(3, world.lives)
        assertEquals(ChaseStatus.Running, world.status)
    }

    @Test
    fun `steering is clamped to the track`() {
        val world = engine.newWorld()
        assertEquals(0, engine.steerTo(world, -4).targetLane)
        assertEquals(2, engine.steerTo(world, 9).targetLane)
    }

    @Test
    fun `lane slide eases in and is speed-capped, never teleporting`() {
        var world = engine.steerTo(engine.newWorld().copy(targetLane = 0, playerX = 0f), 2)
        world = engine.step(world, 16).world
        assertTrue("moved", world.playerX > 0f)
        assertTrue("capped by laneSwitchSpeed", world.playerX <= quietConfig.laneSwitchSpeed * 0.016f + 0.0001f)
        repeat(60) { world = engine.step(world, 16).world }
        assertEquals(2f, world.playerX)
    }

    @Test
    fun `obstacle in pikachu's lane costs a life and grants invulnerability`() {
        val step = engine.step(worldWith(obstacleAt(lane = 1)), 16)
        assertEquals(2, step.world.lives)
        assertTrue(step.world.invulnerableMs > 0)
        assertTrue(step.world.entities.isEmpty())
        assertTrue(step.events.any { it is ChaseEvent.Hit })
    }

    @Test
    fun `obstacle in another lane is dodged`() {
        val step = engine.step(worldWith(obstacleAt(lane = 0)), 16)
        assertEquals(3, step.world.lives)
        assertTrue(step.events.none { it is ChaseEvent.Hit })
    }

    @Test
    fun `obstacles pass through while invulnerable`() {
        val afterHit = engine.step(worldWith(obstacleAt(lane = 1)), 16).world
        val second = afterHit.copy(entities = listOf(obstacleAt(lane = 1, id = 2)))
        val step = engine.step(second, 16)
        assertEquals(2, step.world.lives)
    }

    @Test
    fun `three hits gets pikachu caught and only one revive is allowed`() {
        var world = engine.newWorld().copy(lives = 1)
        world = engine.step(world.copy(entities = listOf(obstacleAt(lane = 1))), 16).world
        assertEquals(ChaseStatus.Caught, world.status)
        assertTrue(engine.canRevive(world))

        world = engine.revive(world)
        assertEquals(ChaseStatus.Running, world.status)
        assertEquals(1, world.lives)

        world = world.copy(invulnerableMs = 0, entities = listOf(obstacleAt(lane = 1)))
        world = engine.step(world, 16).world
        assertEquals(ChaseStatus.Caught, world.status)
        assertFalse(engine.canRevive(world))
    }

    // ── Thunderbolt ─────────────────────────────────────────────────────────

    @Test
    fun `berries charge thunderbolt, which clears attackers and resets the meter`() {
        var world = engine.newWorld()
        repeat(5) { i ->
            val berry = ChaseEntity(10L + i, EntityKind.ORAN_BERRY, 1, quietConfig.playerY)
            world = engine.step(world.copy(entities = listOf(berry)), 16).world
        }
        assertEquals(1f, world.charge, 0.001f)
        assertEquals(5, world.berries)

        world = world.copy(entities = listOf(ChaseEntity(50, EntityKind.KOFFING, 0, 0.3f)))
        val step = engine.thunderbolt(world)
        assertTrue(step.world.entities.isEmpty())
        assertEquals(0f, step.world.charge)
        assertTrue(step.events.single() is ChaseEvent.ThunderUsed)
    }

    @Test
    fun `a thunder stone fills the meter outright`() {
        val stone = ChaseEntity(11, EntityKind.THUNDER_STONE, 1, quietConfig.playerY)
        val world = engine.step(engine.newWorld().copy(entities = listOf(stone)), 16).world
        assertTrue(world.canThunderbolt)
    }

    @Test
    fun `thunderbolt does nothing without a full meter`() {
        val world = engine.newWorld().copy(charge = 0.6f, entities = listOf(obstacleAt(lane = 0)))
        val step = engine.thunderbolt(world)
        assertEquals(world, step.world)
        assertTrue(step.events.isEmpty())
    }

    @Test
    fun `runs start with no shield, so pikachu is hittable straight away`() {
        val step = ChaseEngine(quietConfig, Random(1)).let { it.step(it.newWorld().copy(entities = listOf(obstacleAt(lane = 1))), 16) }
        assertEquals(2, step.world.lives)
    }

    // ── Agility ─────────────────────────────────────────────────────────────

    @Test
    fun `agility is a single free lifeline per run`() {
        val world = engine.newWorld()
        assertEquals(1, world.agilityCharges)
        val used = engine.useAgility(world)
        assertTrue(used.world.isAgile)
        assertEquals(0, used.world.agilityCharges)
        assertTrue(used.events.contains(ChaseEvent.AgilityStarted))
        // Can't stack a second dash, and no charges left anyway.
        assertEquals(used.world, engine.useAgility(used.world).world)
    }

    @Test
    fun `agility zaps attackers one at a time, nearest first`() {
        val near = ChaseEntity(1, EntityKind.EKANS, 0, 0.6f)
        val far = ChaseEntity(2, EntityKind.KOFFING, 2, 0.2f)
        var world = engine.useAgility(engine.newWorld().copy(entities = listOf(near, far))).world

        val first = engine.step(world, 16)
        assertEquals(listOf(far.id), first.world.entities.map { it.id })
        assertEquals(1, first.events.count { it is ChaseEvent.Zapped })

        world = first.world
        repeat((quietConfig.agilityStrikeIntervalMs / 16 + 1).toInt()) { world = engine.step(world, 16).world }
        assertTrue(world.entities.none { it.kind.isObstacle })
    }

    @Test
    fun `agility pulls in pickups from every lane`() {
        val sideBerry = ChaseEntity(5, EntityKind.ORAN_BERRY, 2, quietConfig.playerY - 0.08f)
        val world = engine.useAgility(engine.newWorld().copy(entities = listOf(sideBerry))).world
        val step = engine.step(world, 16)
        assertEquals(1, step.world.berries)
    }

    @Test
    fun `agility ends after ten seconds with a short grace blink`() {
        var world = engine.useAgility(engine.newWorld()).world
        val events = mutableListOf<ChaseEvent>()
        repeat((quietConfig.agilityMs / 50 + 1).toInt()) { val step = engine.step(world, 50); world = step.world; events += step.events }
        assertFalse(world.isAgile)
        assertTrue(world.invulnerableMs > 0)
        assertTrue(events.contains(ChaseEvent.AgilityEnded))
    }

    @Test
    fun `extra agility can be earned only up to the cap`() {
        var world = engine.useAgility(engine.newWorld()).world
        repeat(quietConfig.agilityMaxExtra) {
            assertTrue(engine.canEarnAgility(world))
            world = engine.grantAgility(world)
        }
        assertEquals(quietConfig.agilityMaxExtra, world.agilityCharges)
        assertFalse(engine.canEarnAgility(world))
        assertEquals(world, engine.grantAgility(world))
    }

    private val incomingNet = NetWarning(lane = 1, remainingMs = 10, totalMs = 900)

    private fun snaredWorld(): ChaseWorld =
        engine.step(engine.newWorld().copy(warnings = listOf(incomingNet)), 16).world

    @Test
    fun `a net landing on pikachu snares it instead of costing a life`() {
        val step = engine.step(engine.newWorld().copy(warnings = listOf(incomingNet)), 16)
        assertTrue(step.world.isSnared)
        assertEquals(3, step.world.lives)
        assertTrue(step.events.contains(ChaseEvent.Snared))
    }

    @Test
    fun `a net is dodged by leaving its lane`() {
        val moved = engine.newWorld().copy(playerX = 2f, targetLane = 2, warnings = listOf(incomingNet))
        val step = engine.step(moved, 16)
        assertFalse(step.world.isSnared)
        assertTrue(step.events.contains(ChaseEvent.NetDodged))
    }

    @Test
    fun `snared pikachu can't steer or slide`() {
        val snared = snaredWorld()
        val steered = engine.steerTo(snared, 0)
        assertEquals(snared.targetLane, steered.targetLane)
        assertEquals(snared.playerX, engine.step(steered, 16).world.playerX)
    }

    @Test
    fun `enough struggles break free with lives intact`() {
        var world = snaredWorld()
        val events = mutableListOf<ChaseEvent>()
        repeat(quietConfig.snareStruggles) {
            val step = engine.struggle(world)
            world = step.world
            events += step.events
        }
        assertFalse(world.isSnared)
        assertEquals(3, world.lives)
        assertEquals(ChaseEvent.BrokeFree, events.last())
    }

    @Test
    fun `running out the snare timer costs a life and frees pikachu`() {
        var world = snaredWorld()
        repeat((quietConfig.snareMs / 50 + 2).toInt()) { world = engine.step(world, 50).world }
        assertFalse(world.isSnared)
        assertEquals(2, world.lives)
    }

    @Test
    fun `thunderbolt also breaks pikachu out of a net`() {
        val step = engine.thunderbolt(snaredWorld().copy(charge = 1f))
        assertFalse(step.world.isSnared)
        assertTrue(step.events.contains(ChaseEvent.BrokeFree))
    }

    @Test
    fun `agility breaks pikachu out of a net`() {
        val step = engine.useAgility(snaredWorld())
        assertFalse(step.world.isSnared)
        assertTrue(step.events.contains(ChaseEvent.BrokeFree))
    }

    @Test
    fun `struggling does nothing when not snared`() {
        val world = engine.newWorld()
        assertEquals(world, engine.struggle(world).world)
    }

    @Test
    fun `nets start targeting pikachu once past the start distance`() {
        val netEngine = ChaseEngine(quietConfig.copy(netsStartMeters = 0f), Random(1))
        val world = netEngine.newWorld().copy(spawn = SpawnState(nextNetInMs = 1))
        val step = netEngine.step(world, 16)
        assertEquals(1, step.world.warnings.size)
        assertEquals(world.targetLane, step.world.warnings.single().lane)
        assertTrue(step.events.contains(ChaseEvent.NetIncoming))
    }

    @Test
    fun `running far enough without a hit wins a heart back`() {
        val world = engine.newWorld().copy(lives = 2, metersSinceHit = quietConfig.lifeRegenMeters)
        val step = engine.step(world, 16)
        assertEquals(3, step.world.lives)
        assertTrue(step.events.contains(ChaseEvent.ShookOffRocket))
    }

    @Test
    fun `a long frame hitch is clamped`() {
        val step = engine.step(engine.newWorld(), 5_000)
        assertEquals(quietConfig.maxFrameMs, step.world.elapsedMs)
    }

    @Test
    fun `spawned rows always leave a reachable gap`() {
        val config = ChaseConfig()
        val spawner = ChaseSpawner(config, Random(7))
        var spawn = SpawnState()
        repeat(5_000) {
            val previousGaps = spawn.lastFreeLanes
            val (row, next) = spawner.spawnRow(spawn, difficulty = 1f)
            val blocked = row.filter { it.kind.isObstacle }.map { it.lane }.toSet()
            assertTrue("row blocks every lane", blocked.size < config.laneCount)
            if (previousGaps.isNotEmpty()) {
                assertTrue(
                    "gap $blocked unreachable from $previousGaps",
                    next.lastFreeLanes.any { gap -> previousGaps.any { abs(it - gap) <= 1 } }
                )
            }
            row.filter { it.kind.isPickup }.forEach { assertTrue(it.lane !in blocked) }
            spawn = next
        }
    }
}
