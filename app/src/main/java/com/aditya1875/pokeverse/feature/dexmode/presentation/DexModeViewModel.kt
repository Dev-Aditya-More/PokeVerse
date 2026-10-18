package com.aditya1875.pokeverse.feature.dexmode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.dexmode.data.DexModeStore
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonResult
import com.aditya1875.pokeverse.feature.pokemon.home.domain.repository.PokemonSearchRepository
import com.aditya1875.pokeverse.utils.PokemonSearchHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DexListEntry(val id: Int, val name: String)

data class DexEntryDetail(
    val id: Int,
    val name: String,
    val genus: String?,
    val types: List<String>,
    val heightM: Float,
    val weightKg: Float,
    val flavor: String?,
    val stats: List<Pair<String, Int>>,
    val moves: List<Pair<Int, String>>,
    val spriteUrl: String?,
    val cryUrl: String?
)

enum class DexScreen { BOOT, LIST, ENTRY }
enum class DexTab { INFO, STATS, MOVES }

data class DexUiState(
    val screen: DexScreen = DexScreen.BOOT,
    val entries: List<DexListEntry> = emptyList(),
    val listLoading: Boolean = true,
    val listError: Boolean = false,
    val cursor: Int = 0,
    val tab: DexTab = DexTab.INFO,
    val detail: DexEntryDetail? = null,
    val detailLoading: Boolean = false,
    val detailError: Boolean = false,
    val searchMiss: String? = null
) {
    val current: DexListEntry? get() = entries.getOrNull(cursor)
}

class DexModeViewModel(
    private val searchRepo: PokemonSearchRepository,
    private val detailRepo: PokemonDetailRepo,
    private val store: DexModeStore
) : ViewModel() {

    private val _state = MutableStateFlow(DexUiState())
    val state: StateFlow<DexUiState> = _state.asStateFlow()

    private var detailJob: Job? = null

    init {
        store.markDiscovered()
        loadEntries()
    }

    fun loadEntries() {
        _state.update { it.copy(listLoading = true, listError = false) }
        viewModelScope.launch {
            val entries = searchRepo.getAllPokemonNames()
                .mapNotNull { r -> idFromUrl(r.url)?.let { DexListEntry(it, r.name) } }
                .filter { it.id in 1..NATIONAL_DEX_SIZE }
                .distinctBy { it.id }
                .sortedBy { it.id }
            _state.update { it.copy(entries = entries, listLoading = false, listError = entries.isEmpty()) }
        }
    }

    fun finishBoot() {
        if (_state.value.screen == DexScreen.BOOT) _state.update { it.copy(screen = DexScreen.LIST) }
    }

    /** Returns false when the cursor was already at an edge (so the UI can skip the blip). */
    fun moveCursor(delta: Int): Boolean {
        val s = _state.value
        if (s.entries.isEmpty()) return false
        val next = (s.cursor + delta).coerceIn(0, s.entries.lastIndex)
        if (next == s.cursor) return false
        _state.update { it.copy(cursor = next) }
        if (s.screen == DexScreen.ENTRY) loadDetail()
        return true
    }

    fun setCursor(index: Int) {
        if (index in _state.value.entries.indices) _state.update { it.copy(cursor = index) }
    }

    fun open() {
        val s = _state.value
        when (s.screen) {
            DexScreen.BOOT -> finishBoot()
            DexScreen.LIST -> if (s.current != null) {
                _state.update { it.copy(screen = DexScreen.ENTRY, tab = DexTab.INFO) }
                loadDetail()
            }
            DexScreen.ENTRY -> if (s.detailError) loadDetail()
        }
    }

    /** Returns false when there's nothing left to go back to (the screen should close). */
    fun back(): Boolean {
        return when (_state.value.screen) {
            DexScreen.ENTRY -> {
                detailJob?.cancel()
                _state.update { it.copy(screen = DexScreen.LIST, detail = null, detailLoading = false, detailError = false) }
                true
            }
            else -> false
        }
    }

    fun changeTab(delta: Int): Boolean {
        val tabs = DexTab.entries
        val next = (_state.value.tab.ordinal + delta).coerceIn(0, tabs.lastIndex)
        if (next == _state.value.tab.ordinal) return false
        _state.update { it.copy(tab = tabs[next]) }
        return true
    }

    /** Tries each spoken/typed candidate in turn; jumps to and opens the first match. */
    fun search(candidates: List<String>): Boolean {
        val entries = _state.value.entries
        if (entries.isEmpty()) return false
        val index = candidates.firstNotNullOfOrNull { matchIndex(it, entries) }
        if (index == null) {
            _state.update { it.copy(searchMiss = candidates.firstOrNull()?.trim()) }
            return false
        }
        _state.update { it.copy(cursor = index, searchMiss = null, screen = DexScreen.LIST) }
        open()
        return true
    }

    fun clearSearchMiss() = _state.update { it.copy(searchMiss = null) }

    private fun matchIndex(raw: String, entries: List<DexListEntry>): Int? {
        val text = raw.trim().lowercase()
        if (text.isEmpty()) return null
        Regex("\\d+").find(text)?.value?.toIntOrNull()?.let { num ->
            entries.indexOfFirst { it.id == num }.takeIf { it >= 0 }?.let { return it }
        }
        val spoken = text.replace(" ", "-")
        val pool = entries.map { PokemonResult(it.name) }
        val best = PokemonSearchHelper.searchPokemon(pool, spoken, limit = 1).firstOrNull()
            ?: PokemonSearchHelper.searchPokemon(pool, text.replace(" ", ""), limit = 1).firstOrNull()
            ?: return null
        return entries.indexOfFirst { it.name == best.pokemon.name }.takeIf { it >= 0 }
    }

    private fun loadDetail() {
        val entry = _state.value.current ?: return
        detailJob?.cancel()
        _state.update { it.copy(detail = null, detailLoading = true, detailError = false) }
        detailJob = viewModelScope.launch {
            try {
                val pokemonCall = async { detailRepo.getPokemonByName(entry.id.toString()) }
                val speciesCall = async { runCatching { detailRepo.getPokemonSpeciesByName(entry.id.toString()) }.getOrNull() }
                val p = pokemonCall.await()
                val species = speciesCall.await()

                val flavor = species?.flavorTextEntries
                    ?.firstOrNull { it.language.name == "en" }
                    ?.flavorText
                    ?.replace(Regex("\\s+"), " ")
                    ?.trim()
                val genus = species?.genera?.firstOrNull { it.language.name == "en" }?.genus

                val moves = p.moves.mapNotNull { m ->
                    m.version_group_details
                        .filter { it.move_learn_method.name == "level-up" }
                        .minByOrNull { it.level_learned_at }
                        ?.let { it.level_learned_at to m.move.name }
                }.sortedWith(compareBy({ it.first }, { it.second }))

                val detail = DexEntryDetail(
                    id = p.id,
                    name = entry.name,
                    genus = genus,
                    types = p.types.map { it.type.name },
                    heightM = p.height / 10f,
                    weightKg = p.weight / 10f,
                    flavor = flavor,
                    stats = p.stats.map { it.stat.name to it.base_stat },
                    moves = moves,
                    spriteUrl = p.sprites.front_default ?: p.sprites.other?.officialArtwork?.frontDefault,
                    cryUrl = p.cries?.legacy ?: p.cries?.latest
                )
                store.markSeen(entry.id)
                _state.update { it.copy(detail = detail, detailLoading = false) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(detailLoading = false, detailError = true) }
            }
        }
    }

    companion object {
        const val NATIONAL_DEX_SIZE = 1025

        private val ID_REGEX = Regex("/pokemon/(\\d+)/?$")
        fun idFromUrl(url: String): Int? = ID_REGEX.find(url)?.groupValues?.get(1)?.toIntOrNull()
    }
}

/** Device-style names: NIDORAN♀, MR. MIME, FARFETCH'D… */
fun dexName(raw: String): String = when (raw) {
    "nidoran-f" -> "NIDORAN♀"
    "nidoran-m" -> "NIDORAN♂"
    "mr-mime" -> "MR. MIME"
    "mime-jr" -> "MIME JR."
    "mr-rime" -> "MR. RIME"
    "farfetchd" -> "FARFETCH'D"
    "sirfetchd" -> "SIRFETCH'D"
    "type-null" -> "TYPE: NULL"
    "ho-oh" -> "HO-OH"
    "porygon-z" -> "PORYGON-Z"
    else -> raw.replace('-', ' ').uppercase()
}

/** Spoken form for TTS: "Mr. Mime", "Nidoran female". */
fun spokenName(raw: String): String = when (raw) {
    "nidoran-f" -> "Nidoran female"
    "nidoran-m" -> "Nidoran male"
    else -> dexName(raw).lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
}

/** Pixel sprite for list previews, built from the dex number (no detail fetch needed). */
fun dexSpriteUrl(id: Int): String =
    if (id <= 905) "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"
    else "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
