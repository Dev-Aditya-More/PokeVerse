package com.aditya1875.pokeverse.feature.evolution.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionEconomy
import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionOption
import com.aditya1875.pokeverse.feature.evolution.domain.EvolutionPaths
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.dex.EvolutionWallet
import com.aditya1875.pokeverse.feature.pokemon.dex.EvolveResult
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgress
import com.aditya1875.pokeverse.feature.progression.data.ProgressionRepository
import com.aditya1875.pokeverse.feature.progression.data.ProgressionState
import kotlinx.coroutines.flow.first
import com.aditya1875.pokeverse.feature.progression.domain.ShopItem
import com.aditya1875.pokeverse.feature.progression.domain.ShopItems
import com.aditya1875.pokeverse.utils.LoadErrorKind
import com.aditya1875.pokeverse.utils.toLoadErrorKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One evolution the trainer could pick, with everything the card needs to draw it. */
data class EvolutionChoice(
    val option: EvolutionOption,
    val toDisplayName: String,
    val artUrl: String,
    /** The new form is on the trainer's roster already, so it can't be made twice. */
    val alreadyHave: Boolean,
    /** The new form has been caught at some point, so its art may be shown in full colour. */
    val inDex: Boolean = false,
    /** The stone or held item this evolution needs, or null when it costs shards. */
    val item: ShopItem? = null,
    /** How many of [item] the trainer holds. */
    val itemHeld: Int = 0,
) {
    val needsItem: Boolean get() = item != null
    /** Shards this evolution costs: none when an item pays for it. */
    val shardCost: Int get() = if (needsItem) 0 else option.cost
}

/** The moment right after a successful evolution, played as a cinematic. */
data class EvolutionMoment(
    val fromName: String,
    val fromArt: String,
    val toName: String,
    val toArt: String,
)

data class EvolutionUiState(
    val loading: Boolean = true,
    val error: LoadErrorKind? = null,
    val choices: List<EvolutionChoice> = emptyList(),
    val wallet: EvolutionWallet = EvolutionWallet(),
    val caught: Boolean = false,
    val busy: Boolean = false,
    val moment: EvolutionMoment? = null,
    val message: EvolveResult? = null,
)

class EvolutionViewModel(
    private val repo: PokemonDetailRepo,
    private val pokedex: PokedexRepository,
    private val progression: ProgressionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EvolutionUiState())
    val state: StateFlow<EvolutionUiState> = _state

    private var loadedFor: String? = null
    private var loadJob: Job? = null
    private var walletJob: Job? = null
    private var fromName = ""
    private var fromId = 0
    private var fromArt = ""

    /** Loads what [speciesName] can evolve into. Safe to call again for the same Pokémon (no-op). */
    fun load(speciesName: String, id: Int, art: String, force: Boolean = false) {
        if (!force && loadedFor == speciesName) return
        loadedFor = speciesName
        fromName = speciesName
        fromId = id
        fromArt = art
        _state.update { it.copy(loading = true, error = null, choices = emptyList(), moment = null) }

        walletJob?.cancel()
        walletJob = viewModelScope.launch {
            combineWallet()
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val species = repo.getPokemonSpeciesByName(speciesName)
                val chainId = species.evolutionChain?.url?.trimEnd('/')?.substringAfterLast('/')?.toIntOrNull()
                if (chainId == null) {
                    _state.update { it.copy(loading = false, choices = emptyList()) }
                    return@launch
                }
                val chain = repo.getEvolutionChain(chainId).chain
                val next = EvolutionPaths.steps(chain, speciesName)
                val options = coroutineScope {
                    next.map { step ->
                        async {
                            val p = repo.getPokemonByName(step.name)
                            val art = p.sprites.other?.officialArtwork?.frontDefault
                                ?: p.sprites.other?.home?.frontDefault
                                ?: p.sprites.front_default.orEmpty()
                            EvolutionChoice(
                                option = EvolutionOption(id, p.id, step.name, step.depth, EvolutionEconomy.cost(step.depth)),
                                toDisplayName = step.name.replaceFirstChar { it.uppercase() },
                                artUrl = art,
                                alreadyHave = false,
                                item = ShopItems.find(step.itemId.orEmpty()),
                            )
                        }
                    }.awaitAll()
                }
                // The ownership flow only emits when something changes, so read it now for the first draw.
                val prog = pokedex.progress.first()
                val items = progression.state.first()
                _state.update {
                    it.copy(loading = false, error = null, choices = options.map { c -> c.withOwnership(prog, items) })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toLoadErrorKind()) }
            }
        }
    }

    private suspend fun combineWallet() {
        kotlinx.coroutines.flow.combine(pokedex.wallet, pokedex.progress, progression.state) { w, prog, items ->
            Triple(w, prog, items)
        }.collectLatest { (w, prog, items) ->
            _state.update { s ->
                s.copy(
                    wallet = w,
                    // Only a Pokémon still on the roster can evolve: an evolved-away form is gone for good.
                    caught = fromId in prog.roster,
                    choices = s.choices.map { it.withOwnership(prog, items) },
                )
            }
        }
    }

    /** Fills in what the trainer owns: the form itself (roster / Pokédex) and how many of its item they hold. */
    private fun EvolutionChoice.withOwnership(prog: DexProgress, items: ProgressionState) = copy(
        alreadyHave = option.toId in prog.roster,
        inDex = option.toId in prog.caught,
        itemHeld = item?.let { items.held(it.id) } ?: 0,
    )

    fun retry() {
        loadedFor?.let { load(it, fromId, fromArt, force = true) }
    }

    fun evolve(choice: EvolutionChoice) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            // An evolution that needs a stone or held item uses one up first; if the evolution then fails
            // for any reason, the item goes back so it is never lost for nothing.
            val item = choice.item
            val tookItem = item != null && progression.useItem(item.id)
            val result = if (item != null && !tookItem) EvolveResult.NoItem
            else pokedex.evolve(choice.option.fromId, choice.option.toId, choice.shardCost)
            if (tookItem && result != EvolveResult.Success) progression.refundItem(item!!.id)
            _state.update {
                it.copy(
                    busy = false,
                    message = result.takeIf { r -> r != EvolveResult.Success },
                    moment = if (result == EvolveResult.Success) {
                        EvolutionMoment(
                            fromName = fromName.replaceFirstChar { c -> c.uppercase() },
                            fromArt = fromArt,
                            toName = choice.toDisplayName,
                            toArt = choice.artUrl,
                        )
                    } else null,
                )
            }
        }
    }

    fun momentShown() = _state.update { it.copy(moment = null) }
}
