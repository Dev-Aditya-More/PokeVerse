package com.aditya1875.pokeverse.feature.game.wildcatch.presentation.viewmodels

import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPValues
import coil.request.ImageRequest
import coil.ImageLoader
import android.content.Context
import kotlinx.coroutines.flow.update
import com.aditya1875.pokeverse.utils.LoadErrorKind
import com.aditya1875.pokeverse.utils.toLoadErrorKind
import com.aditya1875.pokeverse.feature.challenges.data.ChallengeRepository
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchTier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.game.core.data.local.dao.GameScoreDao
import com.aditya1875.pokeverse.feature.game.core.data.local.entity.GameScoreEntity
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.ThrowAccuracy
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.WildCatchDifficulty
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.WildCatchPokemon
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.state.WildCatchGameState
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgress
import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionEconomy
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchOdds
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.CatchSpawns
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.RecentEncounters
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.aditya1875.pokeverse.feature.progression.data.ProgressionRepository
import com.aditya1875.pokeverse.feature.progression.domain.ShopItems
import com.aditya1875.pokeverse.utils.LegendaryPokemon
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Share of encounters drawn from Pokémon the trainer has already caught. */
private const val REPEAT_SPAWN_CHANCE = 0.15f

/** How often a random pick that is already in the Pokédex is passed over for a new one, so a collection fills in. */
private const val PREFER_UNCAUGHT_CHANCE = 0.65f

/**
 * What the trainer can throw right now: the ball in hand, how many of each bought ball they hold, and
 * the Rare Lure. A plain Poké Ball is free and always available, so it has no stock entry.
 */
data class CatchBag(
    val selected: BallType = BallType.POKE,
    val stock: Map<BallType, Int> = emptyMap(),
    val lureStock: Int = 0,
    /** Encounters left on the lure that is burning (0 = none lit). */
    val lureLeft: Int = 0,
    /** Free runs left this game. At zero, running away costs a rewarded ad (free for Pro). */
    val runsLeft: Int = WildCatchViewModel.MAX_FREE_RUNS,
)

class WildCatchViewModel(
    private val repo: PokemonDetailRepo,
    private val xpManager: XPManager,
    private val gameScoreDao: GameScoreDao,
    private val userRepository: UserProfileRepository,
    private val pokedex: PokedexRepository,
    private val progression: ProgressionRepository,
    private val challenges: ChallengeRepository,
    private val imageLoader: ImageLoader,
    private val appContext: Context
) : ViewModel() {

    companion object {
        const val MAX_LIVES = 3
        /** Free runs per game; after these, running away is a rewarded ad. */
        const val MAX_FREE_RUNS = 3
        /** Chance each encounter is a legendary or mythical while a Rare Lure is working. */
        private const val LURE_LEGENDARY_CHANCE = 0.25f
        /** Rounds of candidates tried to find the rolled category before settling for the nearest one. */
        private const val SEARCH_ROUNDS = 3
        private const val STREAK_FOR_LIFE = 5 // recover a heart every 5-catch streak
        private const val STREAK_BONUS_PER_STEP = 10
        private const val STREAK_BONUS_CAP = 50
    }

    private val _gameState = MutableStateFlow<WildCatchGameState>(WildCatchGameState.Idle)
    val gameState: StateFlow<WildCatchGameState> = _gameState.asStateFlow()

    private val _xpResult = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xpResult.asSharedFlow()

    private var currentLives = MAX_LIVES
    private var pokemonCount = 0
    private var catches = 0
    private var currentScore = 0
    private var currentStreak = 0
    private var nextPokemon: WildCatchPokemon? = null
    private var sessionBestScore = 0

    // Caught ids as of the last dex update: tells a first catch from a repeat when the ball settles.
    private var knownCaught: Set<Int> = emptySet()

    // ── Bag: balls and the Rare Lure ──────────────────────────────────────────────
    private val _bag = MutableStateFlow(CatchBag())
    val bag: StateFlow<CatchBag> = _bag.asStateFlow()

    init {
        viewModelScope.launch { pokedex.progress.collect { knownCaught = it.caught } }
        viewModelScope.launch {
            progression.state.collect { s ->
                _bag.update { cur ->
                    val stock = BallType.entries.filter { it.itemId != null }.associateWith { s.held(it.itemId!!) }
                    cur.copy(
                        stock = stock,
                        lureStock = s.held(ShopItems.ID_LURE),
                        // A ball that just ran out falls back to the free one instead of staying selected.
                        selected = if (cur.selected != BallType.POKE && (stock[cur.selected] ?: 0) == 0) BallType.POKE
                        else cur.selected,
                    )
                }
            }
        }
    }

    fun selectBall(ball: BallType) {
        _bag.update { if (ball == BallType.POKE || (it.stock[ball] ?: 0) > 0) it.copy(selected = ball) else it }
    }

    /** Lights a Rare Lure: the next few encounters are far more likely to be legendary or mythical. */
    fun useLure() {
        if (_bag.value.lureLeft > 0 || _bag.value.lureStock <= 0) return
        viewModelScope.launch {
            if (!progression.useItem(ShopItems.ID_LURE)) return@launch
            _bag.update { it.copy(lureLeft = ShopItems.LURE_ENCOUNTERS) }
            // The encounter already queued was rolled without the lure.
            nextPokemon = null
            prefetchNext()
        }
    }

    fun startGame() {
        _bag.update { it.copy(runsLeft = MAX_FREE_RUNS) }
        sinceLegendary = CatchSpawns.LEGENDARY_COOLDOWN - CatchSpawns.LEGENDARY_START_HEAD
        currentLives = MAX_LIVES
        pokemonCount = 0
        catches = 0
        currentScore = 0
        currentStreak = 0
        nextPokemon = null
        _gameState.value = WildCatchGameState.Loading
        viewModelScope.launch {
            sessionBestScore = userRepository.profileFlow.first().bestWildCatchScore
            val pokemon = loadRandomPokemon()
            if (pokemon != null) {
                pokemonCount++
                _gameState.value = encounter(pokemon)
                prefetchNext()
            } else {
                // Used to drop back to Idle, which left the loading spinner up forever.
                _gameState.value = WildCatchGameState.LoadFailed(lastLoadError, inGame = false)
            }
        }
    }

    /** Retry after a failed load: picks the run back up exactly where it stopped. */
    fun retryLoad() {
        val failed = _gameState.value as? WildCatchGameState.LoadFailed ?: return
        if (!failed.inGame) {
            startGame()
            return
        }
        loadNextEncounter()
    }

    /** Ends a run whose next Pokémon won't load, keeping the score earned so far. */
    fun endRunAfterFailure() {
        if (_gameState.value is WildCatchGameState.LoadFailed) finishGame()
    }

    private var lastLoadError = LoadErrorKind.OFFLINE

    private fun loadNextEncounter() {
        _gameState.value = WildCatchGameState.Loading
        viewModelScope.launch {
            val pokemon = loadRandomPokemon()
            if (pokemon != null) {
                pokemonCount++
                _gameState.value = encounter(pokemon)
                prefetchNext()
            } else {
                _gameState.value = WildCatchGameState.LoadFailed(lastLoadError, inGame = true)
            }
        }
    }

    // ringFraction: 1f = ring at full size (worst), 0f = ring smallest (perfect).
    // Both the timing windows and the odds get tougher as more Pokémon appear -
    // see WildCatchDifficulty for the ramp.
    fun throwBall(ringFraction: Float) {
        val state = _gameState.value as? WildCatchGameState.Throwing ?: return

        val thresholds = WildCatchDifficulty.ringThresholds(state.pokemonCount)
        val accuracy = when {
            ringFraction <= thresholds.excellent -> ThrowAccuracy.EXCELLENT
            ringFraction <= thresholds.great -> ThrowAccuracy.GREAT
            ringFraction <= thresholds.good -> ThrowAccuracy.GOOD
            else -> ThrowAccuracy.MISS
        }

        // The ball in hand: a bought ball is used up by the throw, whether or not it holds.
        val bag = _bag.value
        val ball = bag.selected.takeIf { it == BallType.POKE || (bag.stock[it] ?: 0) > 0 } ?: BallType.POKE
        ball.itemId?.let { id -> viewModelScope.launch { progression.useItem(id) } }

        val catchChance = CatchOdds.chance(accuracy, state.pokemonCount, state.pokemon.tier, ball)
        val caught = Random.nextFloat() < catchChance
        val newCatch = caught && state.pokemon.id !in knownCaught
        var lifeRecovered = false
        var scoreGain = 0
        var shardsGained = 0

        if (caught) {
            val wanted = EvolutionEconomy.forCatch(accuracy, newCatch, state.pokemon.tier)
            val caughtId = state.pokemon.id
            viewModelScope.launch {
                val granted = pokedex.awardShards(wanted)
                // The result screen is already up; fill in what the daily cap really allowed.
                _gameState.update { cur ->
                    if (cur is WildCatchGameState.ShakeResult && cur.pokemon.id == caughtId)
                        cur.copy(shardsGained = granted, shardsCapped = granted < wanted)
                    else cur
                }
            }
            catches++
            currentStreak++
            knownCaught = knownCaught + state.pokemon.id
            viewModelScope.launch {
                pokedex.markCaught(state.pokemon.id, state.pokemon.tier)
                challenges.recordCatch(state.pokemon.tier, newCatch, currentStreak)
            }
            // Streaks pay: each catch in a row adds a little, so a run of clean throws feels like momentum.
            val streakBonus = ((currentStreak - 1) * STREAK_BONUS_PER_STEP).coerceIn(0, STREAK_BONUS_CAP)
            scoreGain = 100 + accuracy.scoreBonus + state.pokemon.tier.scoreBonus + streakBonus
            currentScore += scoreGain
            // Recover a life every STREAK_FOR_LIFE consecutive catches
            if (currentStreak % STREAK_FOR_LIFE == 0 && currentLives < MAX_LIVES) {
                currentLives++
                lifeRecovered = true
            }
            // Paid per catch for the first few of a run, and more for a rarer Pokémon; the rest pays at the end.
            if (catches <= XPValues.ENDLESS_PAID_CAP) viewModelScope.launch {
                val result = xpManager.awardGameXP(XPEvent.WildCatchCaught(streak = currentStreak, rarityBonus = state.pokemon.tier.xpBonus))
                if (result.xpGained > 0) _xpResult.emit(result)
            }
        } else {
            currentStreak = 0
            currentLives--
        }

        // shakeCount tells the visual story: perfect near-miss = 3 nail-biting shakes,
        // clear miss = 1 shake so the player immediately understands why they failed
        val shakeCount = if (caught) 3 else when (accuracy) {
            ThrowAccuracy.EXCELLENT -> 3
            ThrowAccuracy.GREAT -> 2
            ThrowAccuracy.GOOD -> 1
            ThrowAccuracy.MISS -> 1
        }

        _gameState.value = WildCatchGameState.ShakeResult(
            pokemon = state.pokemon,
            caught = caught,
            accuracy = accuracy,
            lifeRecovered = lifeRecovered,
            pokemonCount = state.pokemonCount,
            catches = catches,
            score = currentScore,
            lives = currentLives,
            shakeCount = shakeCount,
            newCatch = newCatch,
            bestScore = sessionBestScore,
            scoreGain = scoreGain,
            streak = currentStreak,
            caughtTotal = knownCaught.size,
            shardsGained = shardsGained,
            ball = ball
        )
    }

    fun nextRound() {
        if (currentLives <= 0) {
            finishGame()
            return
        }
        val cached = nextPokemon
        nextPokemon = null
        if (cached != null) {
            pokemonCount++
            _gameState.value = encounter(cached)
            prefetchNext()
        } else {
            loadNextEncounter()
        }
    }

    /**
     * Run from this Pokémon: no life is lost, but it ends the catch streak, and the next one is tougher
     * all the same (it still counts as an encounter), so running can't be used to stall.
     */
    fun runAway(bypassLimit: Boolean = false) {
        if (_gameState.value !is WildCatchGameState.Throwing) return
        val left = _bag.value.runsLeft
        // Out of free runs: the caller must have earned this one (a rewarded ad, or Pro), or nothing happens.
        if (!bypassLimit && left <= 0) return
        if (!bypassLimit) _bag.update { it.copy(runsLeft = left - 1) }
        currentStreak = 0
        nextRound()
    }

    fun reviveGame() {
        currentLives = 1
        nextRound()
    }

    fun resetGame() {
        _gameState.value = WildCatchGameState.Idle
        currentLives = MAX_LIVES
        pokemonCount = 0
        catches = 0
        currentScore = 0
        currentStreak = 0
        nextPokemon = null
    }

    /** A wild Pokémon appears: it is now seen, whether or not it ends up caught. */
    private fun encounter(pokemon: WildCatchPokemon): WildCatchGameState.Throwing {
        viewModelScope.launch { pokedex.markSeen(pokemon.id) }
        _bag.update { if (it.lureLeft > 0) it.copy(lureLeft = it.lureLeft - 1) else it }
        return throwingState(pokemon)
    }

    private fun throwingState(pokemon: WildCatchPokemon) = WildCatchGameState.Throwing(
        pokemon = pokemon,
        cycleDurationMs = (speedFor(pokemonCount) * pokemon.tier.ringSpeedFactor).toLong(),
        pokemonCount = pokemonCount,
        catches = catches,
        score = currentScore,
        streak = currentStreak,
        lives = currentLives,
        bestScore = sessionBestScore
    )

    private fun prefetchNext() {
        viewModelScope.launch {
            val next = loadRandomPokemon()
            nextPokemon = next
            // Download the sprite now, while the player is busy, so the next round shows it at once.
            next?.let { imageLoader.enqueue(ImageRequest.Builder(appContext).data(it.spriteUrl).build()) }
        }
    }

    /** Ids of the last few encounters, so the same Pokémon never comes round again straight away. */
    private val recent = RecentEncounters()

    /** Encounters since the last Legendary, which decides whether one may come up by chance (see CatchSpawns). */
    private var sinceLegendary = CatchSpawns.LEGENDARY_COOLDOWN - CatchSpawns.LEGENDARY_START_HEAD

    private fun noteEncounter(p: WildCatchPokemon) {
        recent.remember(p.id)
        sinceLegendary = if (p.tier == CatchTier.LEGENDARY) 0 else sinceLegendary + 1
    }

    /**
     * The next wild Pokémon. The category (Common, Uncommon, Rare, Ultra Rare, Legendary) is rolled first,
     * by [CatchSpawns]' odds, and then a Pokémon of that category is looked for, a few candidates at a time.
     * Nothing seen in the last [CatchSpawns.RECENT_MEMORY] encounters is offered. If the exact category
     * can't be found, the nearest one found is used rather than failing the encounter.
     */
    private suspend fun loadRandomPokemon(): WildCatchPokemon? {
        val lured = _bag.value.lureLeft > 0 && Random.nextFloat() < LURE_LEGENDARY_CHANCE
        val target = if (lured) CatchTier.LEGENDARY
        else CatchSpawns.rollTier(pokemonCount, Random, legendaryAllowed = sinceLegendary >= CatchSpawns.LEGENDARY_COOLDOWN)
        var nearest: WildCatchPokemon? = null

        repeat(SEARCH_ROUNDS) {
            val ids = candidateIds(target)
            val found = coroutineScope {
                ids.map { id -> async { runCatching { fetchEncounter(id) } } }.awaitAll()
            }
            for (result in found) {
                result.onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    lastLoadError = e.toLoadErrorKind()
                }
                val p = result.getOrNull() ?: continue
                if (p.tier == target) return p.also { noteEncounter(it) }
                val best = nearest
                if (best == null || CatchSpawns.distance(p.tier, target) < CatchSpawns.distance(best.tier, target)) nearest = p
            }
        }
        return nearest?.also { noteEncounter(it) }
    }

    /** A handful of distinct candidates for [target], never one seen lately. */
    private fun candidateIds(target: CatchTier): List<Int> {
        val skip = recent.snapshot()
        val want = if (target <= CatchTier.UNCOMMON) 2 else 4
        val ids = LinkedHashSet<Int>()
        when (target) {
            CatchTier.LEGENDARY -> ids += LegendaryPokemon.pick(DexProgress.CATCHABLE, want, skip)
            CatchTier.EPIC -> ids += CatchSpawns.ULTRA_RARE_SEEDS.filter { it !in skip }.shuffled().take(want)
            else -> Unit
        }
        // Some encounters are Pokémon you already have, so repeats (and their shards) still happen.
        if (target != CatchTier.LEGENDARY && Random.nextFloat() < REPEAT_SPAWN_CHANCE) {
            knownCaught.filter { it in 1..DexProgress.CATCHABLE && it !in skip && !LegendaryPokemon.isLegendary(it) }
                .randomOrNull()?.let { ids += it }
        }
        var guard = 0
        while (ids.size < want && guard++ < 40) {
            val id = (1..DexProgress.CATCHABLE).random()
            // A legendary would be classed Legendary, so it can't satisfy any other category.
            if (id in skip || (target != CatchTier.LEGENDARY && LegendaryPokemon.isLegendary(id))) continue
            // Leaning towards species not caught yet keeps the Pokédex filling in; repeats still come, a few encounters apart.
            if (id in knownCaught && Random.nextFloat() < PREFER_UNCAUGHT_CHANCE) continue
            ids += id
        }
        return ids.toList()
    }

    private suspend fun fetchEncounter(id: Int): WildCatchPokemon {
        val p = repo.getPokemonByName(id.toString())
        val sprite = p.sprites.other?.officialArtwork?.frontDefault
            ?: p.sprites.front_default ?: error("no artwork for #$id")
        return WildCatchPokemon(
            id = id,
            name = p.name,
            spriteUrl = sprite,
            types = p.types.map { it.type.name },
            tier = CatchTier.of(id, p.stats.sumOf { it.base_stat }),
        )
    }

    // Speed scales with how many Pokémon have appeared, pressure mounts over time
    private fun speedFor(count: Int): Long = when {
        count <= 3 -> 3000L
        count <= 6 -> 2600L
        count <= 10 -> 2200L
        count <= 15 -> 1900L
        count <= 20 -> 1650L
        count <= 25 -> 1450L
        count <= 30 -> 1250L
        else -> 1050L
    }

    private fun finishGame() {
        viewModelScope.launch {
            val result = xpManager.awardGameXP(XPEvent.WildCatchComplete(catches))
            if (result.xpGained > 0) _xpResult.emit(result)
            userRepository.updateBestScore("wildcatch", currentScore)
            userRepository.incrementGamesPlayed()
            val catchRate = if (pokemonCount > 0) catches.toFloat() / pokemonCount else 0f
            gameScoreDao.insertScore(
                GameScoreEntity(
                    gameType = "wildcatch",
                    difficulty = "SURVIVAL",
                    score = currentScore,
                    moves = pokemonCount,
                    timeSeconds = 0,
                    stars = when {
                        catchRate >= 0.85f -> 3
                        catchRate >= 0.60f -> 2
                        else -> 1
                    }
                )
            )
        }
        _gameState.value = WildCatchGameState.Finished(
            catches = catches,
            pokemonCount = pokemonCount,
            score = currentScore,
            isNewBest = currentScore > sessionBestScore
        )
    }
}
