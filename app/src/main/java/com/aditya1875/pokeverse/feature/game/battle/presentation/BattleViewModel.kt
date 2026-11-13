package com.aditya1875.pokeverse.feature.game.battle.presentation

import kotlinx.coroutines.flow.first
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import kotlinx.coroutines.withTimeoutOrNull
import coil.request.ImageRequest
import coil.ImageLoader
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEngine
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEvent
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleMatchmaker
import com.aditya1875.pokeverse.feature.game.battle.data.SavedTeam
import com.aditya1875.pokeverse.feature.game.battle.domain.TeamFormat
import com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules
import com.aditya1875.pokeverse.feature.game.battle.data.SavedTeamsRepository
import com.aditya1875.pokeverse.feature.game.battle.data.loadFighter
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleState
import com.aditya1875.pokeverse.feature.game.battle.domain.BotBrain
import com.aditya1875.pokeverse.feature.game.battle.domain.BotTier
import com.aditya1875.pokeverse.feature.game.battle.domain.Fighter
import com.aditya1875.pokeverse.feature.game.battle.domain.Who
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionEconomy
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgress
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import com.aditya1875.pokeverse.utils.LegendaryPokemon
import com.aditya1875.pokeverse.utils.LoadErrorKind
import com.aditya1875.pokeverse.utils.toLoadErrorKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

/** Team Battle: choose from the Pokémon you've caught, then fight a bot of matching strength. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BattleViewModel(
    private val repo: PokemonDetailRepo,
    private val pokedex: PokedexRepository,
    private val xpManager: XPManager,
    private val imageLoader: ImageLoader,
    private val appContext: Context,
    private val userRepository: UserProfileRepository,
    private val savedTeams: SavedTeamsRepository,
) : ViewModel(), BattleHost {

    companion object {
        const val MAX_TEAM = 6

        /** A battle that ended before this turn pays nothing, so quitting early can't farm XP. */
        const val MIN_TURNS_FOR_REWARD = 3
        private const val SCOUT_ROUNDS = 6
        private const val SCOUT_PER_ROUND = 6
        private const val BST_TOLERANCE = 0.2f
    }

    sealed interface Phase {
        data object Picking : Phase
        data object Preparing : Phase
        data class Failed(val kind: LoadErrorKind) : Phase
        data object Battling : Phase
    }

    data class Picker(
        val caught: List<Int> = emptyList(),
        val selected: List<Int> = emptyList(),
        val tier: BotTier = BotTier.TRAINER,
        val loaded: Boolean = false,
        /** The team collection: saved teams that load in one tap. */
        val saved: List<SavedTeam> = emptyList(),
        /** The saved team last loaded, so a changed version can be saved back over it in one tap. */
        val activeTeam: String? = null,
        /** The last tap was refused because the team already has its Legendaries. */
        val legendaryNotice: Boolean = false,
        /** What kind of team is being built: it sets the size, the Legendary limit and which saved teams show. */
        val format: TeamFormat = TeamFormat.PRACTICE,
    ) {
        val legendaries: Int get() = TeamRules.legendaryCount(selected)
    }

    private val _phase = MutableStateFlow<Phase>(Phase.Picking)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    private val _picker = MutableStateFlow(Picker())
    val picker: StateFlow<Picker> = _picker.asStateFlow()

    private val _battle = MutableStateFlow<BattleState?>(null)
    val battle: StateFlow<BattleState?> = _battle.asStateFlow()

    /** Changes with every new battle, so the arena throws away the last one's animation state. */
    private val _battleKey = MutableStateFlow(0)
    override val battleKey: StateFlow<Int> = _battleKey.asStateFlow()

    /** What happened in the latest turn, in order, for the screen to play back. */
    private val _events = MutableSharedFlow<List<BattleEvent>>(extraBufferCapacity = 16)
    override val events: SharedFlow<List<BattleEvent>> = _events.asSharedFlow()

    private val _xp = MutableSharedFlow<XPResult>(extraBufferCapacity = 8)
    val xpResult: SharedFlow<XPResult> = _xp.asSharedFlow()

    /** Evo Shards the finished battle paid, for the result card. 0 until the battle ends. */
    private val _shards = MutableStateFlow(0)
    override val shardsGained: StateFlow<Int> = _shards.asStateFlow()

    private val _shardsCapped = MutableStateFlow(false)
    override val shardsCapped: StateFlow<Boolean> = _shardsCapped.asStateFlow()

    /**
     * A practice battle has no online extras. The one exception is the rival's name: when a quick match found
     * nobody, the stand-in opponent carries a trainer name, set by [startVsStandIn] and kept for rematches.
     */
    private val _hud = MutableStateFlow(ArenaHud())
    override val hud: StateFlow<ArenaHud> = _hud.asStateFlow()

    private val rng: Random = Random.Default
    private var rewarded = false
    private var activeTier = BotTier.TRAINER

    private val formatFlow = MutableStateFlow(TeamFormat.PRACTICE)

    init {
        viewModelScope.launch {
            // The collection on show follows the format: 3 vs 3, 6 vs 6 and Practice each keep their own teams.
            formatFlow.flatMapLatest { savedTeams.teams(it) }
                .collect { teams -> _picker.update { it.copy(saved = teams) } }
        }
        viewModelScope.launch {
            pokedex.progress.collect { p ->
                if (!p.loaded) return@collect
                _picker.update { cur ->
                    cur.copy(
                        // The roster, not the Pokédex: a Pokémon that evolved is used up and can't fight any more.
                        caught = p.roster.sorted(),
                        selected = cur.selected.filter { it in p.roster },
                        loaded = true,
                    )
                }
            }
        }
    }

    // ── Picking ─────────────────────────────────────────────────────────────

    /**
     * Switches what kind of team is being built (Practice, 3 vs 3, 6 vs 6). The picked team is cut down to
     * what the new format allows, and the saved teams on show become that format's own collection.
     */
    fun setFormat(format: TeamFormat) {
        if (formatFlow.value == format) return
        formatFlow.value = format
        _picker.update { cur ->
            cur.copy(
                format = format,
                selected = TeamRules.trim(cur.selected, format),
                activeTeam = null,
                legendaryNotice = false,
            )
        }
    }

    fun toggle(id: Int) = _picker.update { cur ->
        when {
            id in cur.selected -> cur.copy(selected = cur.selected - id, legendaryNotice = false)
            cur.selected.size >= cur.format.maxTeam -> cur
            // A team may bring at most the format's number of Legendary or Mythical Pokémon.
            !TeamRules.canAdd(cur.selected, id, cur.format) -> cur.copy(legendaryNotice = true)
            else -> cur.copy(selected = cur.selected + id, legendaryNotice = false)
        }
    }

    /** Fills the team with a random full team from the roster, within every limit. */
    fun quickFill() = _picker.update { cur ->
        cur.copy(selected = TeamRules.quickFill(cur.caught, rng), activeTeam = null, legendaryNotice = false)
    }

    /**
     * Fills the team with a type-varied suggestion of [size] from the roster, with at most [maxLegendaries]
     * Legendaries. Replaces whatever is picked now. The index is passed in, so this stays free of any lookup.
     */
    fun suggestTeam(size: Int, maxLegendaries: Int, index: Map<Int, com.aditya1875.pokeverse.utils.PokemonIndex.Entry>) =
        _picker.update { cur ->
            val team = com.aditya1875.pokeverse.feature.game.battle.domain.TeamSuggester.suggest(cur.caught, index, size, maxLegendaries, rng)
            cur.copy(selected = TeamRules.trim(team), activeTeam = null, legendaryNotice = false)
        }

    fun clearTeam() = _picker.update { it.copy(selected = emptyList(), activeTeam = null, legendaryNotice = false) }

    fun setTier(tier: BotTier) = _picker.update { it.copy(tier = tier) }

    // ── Team collection ─────────────────────────────────────────────────────

    /** Puts a whole saved team on the field. Pokémon you no longer have (evolved away) are skipped. */
    fun loadTeam(team: SavedTeam) = _picker.update { cur ->
        val owned = cur.caught.toSet()
        cur.copy(
            selected = TeamRules.trim(team.ids.filter { it in owned }, cur.format),
            activeTeam = team.name,
            legendaryNotice = false,
        )
    }

    /** Saves the picked team under [name]. A name already in use is replaced. */
    fun saveCurrentTeam(name: String) {
        savedTeams.save(_picker.value.format, name, _picker.value.selected)
    }

    fun deleteTeam(team: SavedTeam) = savedTeams.delete(_picker.value.format, team.name)

    fun backToPicker() {
        _hud.value = ArenaHud()
        _battle.value = null
        _phase.value = Phase.Picking
    }

    // ── Starting a battle ───────────────────────────────────────────────────

    /** A quick match found nobody: the same team fights a stand-in rival who goes by [rivalName]. */
    fun startVsStandIn(rivalName: String) {
        _hud.value = ArenaHud(opponentName = rivalName)
        start()
    }

    fun start() {
        val cur = _picker.value
        if (cur.selected.isEmpty() || _phase.value is Phase.Preparing) return
        if (TeamRules.legendaryCount(cur.selected) > TeamRules.MAX_LEGENDARIES) return
        _phase.value = Phase.Preparing
        activeTier = cur.tier
        viewModelScope.launch {
            try {
                val mine = coroutineScope { cur.selected.map { async { fetchFighter(it) } }.awaitAll() }
                val foe = buildFoeTeam(mine, activeTier)
                // Fetch every sprite first (a few seconds at most), so nobody pops in late mid-fight.
                withTimeoutOrNull(6_000) {
                    coroutineScope {
                        (mine.map { it.first } + foe).map { f ->
                            async { runCatching { imageLoader.execute(ImageRequest.Builder(appContext).data(f.spriteUrl).build()) } }
                        }.awaitAll()
                    }
                }
                rewarded = false
                _shards.value = 0
                _shardsCapped.value = false
                _battleKey.value += 1
                _battle.value = BattleEngine.start(mine.map { it.first }, foe)
                _phase.value = Phase.Battling
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A dropped connection must not lose the player's team choice: they can simply retry.
                _phase.value = Phase.Failed(e.toLoadErrorKind())
            }
        }
    }

    fun retry() = start()

    private suspend fun fetchFighter(id: Int): Pair<Fighter, Int> = repo.loadFighter(id)

    /**
     * A foe team the same size as yours and, in total base stats, close to what the tier asks for, so a
     * battle is fair whatever the player caught. Scouting fetches a few candidates at a time.
     */
    private suspend fun buildFoeTeam(mine: List<Pair<Fighter, Int>>, tier: BotTier): List<Fighter> {
        val target = tier.targetBst(mine.map { it.second }.average().toInt())
        val used = mine.map { it.first.id }.toMutableSet()
        val chosen = mutableListOf<Pair<Fighter, Int>>()
        val spare = mutableListOf<Pair<Fighter, Int>>()
        var lastError: Exception? = null
        var fetchedAny = false

        repeat(SCOUT_ROUNDS) {
            if (chosen.size >= mine.size) return@repeat
            val ids = generateSequence { (1..DexProgress.CATCHABLE).random(rng) }
                .filter { it !in used && (tier == BotTier.CHAMPION || !LegendaryPokemon.isLegendary(it)) }
                .distinct().take(SCOUT_PER_ROUND).toList()
            used += ids
            val results = coroutineScope {
                ids.map { id -> async { runCatching { fetchFighter(id) } } }.awaitAll()
            }
            results.forEach { r ->
                r.onSuccess { found ->
                    fetchedAny = true
                    // A foe your team cannot hurt at all would make the battle unwinnable: never pick one.
                    if (!BattleMatchmaker.canDamage(mine.map { it.first }, found.first)) return@onSuccess
                    if (abs(found.second - target) <= target * BST_TOLERANCE && chosen.size < mine.size) chosen += found
                    else spare += found
                }.onFailure { e -> if (e is Exception) lastError = e }
            }
        }
        // Not enough in range: take the closest of what was scouted rather than failing the battle.
        if (chosen.size < mine.size) {
            chosen += spare.sortedBy { abs(it.second - target) }.take(mine.size - chosen.size)
        }
        if (chosen.isEmpty()) throw (lastError ?: IllegalStateException("no opponent found"))
        if (!fetchedAny) throw (lastError ?: IllegalStateException("no opponent found"))
        return chosen.map { it.first }
    }

    // ── Fighting ────────────────────────────────────────────────────────────

    override fun act(action: BattleAction, tera: Boolean, guard: Boolean) {
        val st = _battle.value ?: return
        if (st.over || st.playerMustSwitch) return
        val bot = BotBrain(activeTier.skill)
        val foeAction = bot.choose(st, rng)
        // The bot adds a Guard to its own move when it judges the hit coming is worth one of its two Guards.
        val foeGuard = bot.wantsGuard(st, foeAction, rng)
        val result = BattleEngine.resolveTurn(
            st, action, foeAction, rng, activeTier.skill,
            playerGuard = guard, foeGuard = foeGuard,
        )
        _battle.value = result.state
        _events.tryEmit(result.events)
        if (result.state.over) finish(result.state)
    }

    override fun chooseReplacement(index: Int) {
        val st = _battle.value ?: return
        val result = BattleEngine.forceSwitch(st, index)
        _battle.value = result.state
        _events.tryEmit(result.events)
    }

    private fun finish(state: BattleState) {
        if (rewarded) return
        rewarded = true
        viewModelScope.launch { userRepository.incrementBattlesPlayed() }
        val won = state.winner == Who.PLAYER
        // The profile's Team Battle record counts every battle won, even a short one.
        if (won) viewModelScope.launch {
            val wins = userRepository.profileFlow.first().bestBattleScore
            userRepository.updateBestScore("battle", wins + 1)
        }
        // Walking away after one exchange must not pay: XP is for battles that were actually fought.
        if (state.turn < MIN_TURNS_FOR_REWARD) return
        val wanted = EvolutionEconomy.forBattle(won)
        viewModelScope.launch {
            _shards.value = pokedex.awardShards(wanted)
            _shardsCapped.value = _shards.value < wanted
        }
        viewModelScope.launch {
            val result = xpManager.awardGameXP(XPEvent.BattleComplete(won, if (won) activeTier.xpBonus else 0))
            if (result.xpGained > 0) _xp.emit(result)
        }
    }
}
