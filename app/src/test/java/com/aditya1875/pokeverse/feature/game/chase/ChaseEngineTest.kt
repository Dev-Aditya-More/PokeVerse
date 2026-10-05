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
    fun `moving lanes is clamped to the track`() {
        var world = engine.newWorld()
        repeat(5) { world = engine.moveLane(world, -1) }
        assertEquals(0, world.targetLane)
        repeat(5) { world = engine.moveLane(world, +1) }
        assertEquals(2, world.targetLane)
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

    @Test
    fun `berries charge thunderbolt, which clears obstacles and resets the charge`() {
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
    fun `thunderbolt does nothing without a full charge`() {
        val world = engine.newWorld().copy(charge = 0.6f, entities = listOf(obstacleAt(lane = 0)))
        val step = engine.thunderbolt(world)
        assertEquals(world, step.world)
        assertTrue(step.events.isEmpty())
    }

    @Test
    fun `a landing net hits pikachu only if it's still in that lane`() {
        val incoming = NetWarning(lane = 1, remainingMs = 10, totalMs = 900)
        val stayed = engine.step(engine.newWorld().copy(warnings = listOf(incoming)), 16)
        assertEquals(2, stayed.world.lives)

        val moved = engine.newWorld().copy(playerX = 2f, targetLane = 2, warnings = listOf(incoming))
        val dodged = engine.step(moved, 16)
        assertEquals(3, dodged.world.lives)
        assertTrue(dodged.events.contains(ChaseEvent.NetDodged))
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
