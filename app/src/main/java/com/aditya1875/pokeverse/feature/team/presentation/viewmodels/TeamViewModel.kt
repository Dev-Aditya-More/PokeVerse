package com.aditya1875.pokeverse.feature.team.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.team.data.local.dao.TeamDao
import com.aditya1875.pokeverse.feature.team.data.local.dao.TeamWithMembers
import com.aditya1875.pokeverse.feature.team.data.local.entity.TeamEntity
import com.aditya1875.pokeverse.feature.team.data.local.entity.TeamMemberEntity
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonResult
import com.aditya1875.pokeverse.feature.team.domain.usecase.AddPokemonToTeamUseCase
import com.aditya1875.pokeverse.feature.team.domain.usecase.CreateTeamUseCase
import com.aditya1875.pokeverse.feature.team.domain.usecase.RemovePokemonFromTeamUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class TeamViewModel(
    private val teamDao: TeamDao,
    private val addPokemonToTeamUseCase: AddPokemonToTeamUseCase,
    private val removePokemonFromTeamUseCase: RemovePokemonFromTeamUseCase,
    private val createTeamUseCase: CreateTeamUseCase
) : ViewModel() {

    private val _selectedTeamId = MutableStateFlow<String?>(null)
    val selectedTeamId: StateFlow<String?> = _selectedTeamId

    val allTeams: StateFlow<List<TeamEntity>> =
        teamDao.getAllTeams()
            .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    val allTeamsWithMembers: StateFlow<List<TeamWithMembers>> =
        teamDao.getAllTeamsWithMembers()
            .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    val currentTeam: StateFlow<TeamEntity?> =
        combine(allTeams, selectedTeamId) { teams, selectedId ->
            teams.firstOrNull { it.teamId == selectedId } ?: teams.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTeamMembers: StateFlow<List<TeamMemberEntity>> =
        currentTeam
            .flatMapLatest { team ->
                if (team != null) {
                    teamDao.getTeamMembers(team.teamId)
                } else {
                    flowOf(emptyList())
                }
            }
            .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val defaultTeam = teamDao.getDefaultTeam()
            _selectedTeamId.value = defaultTeam?.teamId
                ?: allTeams.value.firstOrNull()?.teamId
        }
    }

    // -------------------------
    // Team Selection
    // -------------------------

    fun selectTeam(teamId: String) {
        _selectedTeamId.value = teamId
    }

    fun createTeam(teamName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = createTeamUseCase(teamName)

            result
                .onSuccess {
                    _selectedTeamId.value = allTeams.value.lastOrNull()?.teamId
                    onSuccess()
                }
                .onFailure {
                    onError(it.message ?: "Failed to create team")
                }
        }
    }

    fun updateTeamName(teamId: String, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val team = teamDao.getTeamByIdOnce(teamId)
                if (team == null) {
                    onError("Team not found")
                    return@launch
                }

                // Check if new name already exists (excluding current team)
                if (team.teamName.lowercase() != newName.lowercase() &&
                    teamDao.isTeamNameExists(newName)) {
                    onError("A team with this name already exists!")
                    return@launch
                }

                teamDao.updateTeam(team.copy(teamName = newName.trim()))
                onSuccess()
            } catch (e: Exception) {
                Log.e("PokeVM", "Failed to update team name", e)
                onError("Failed to update team name")
            }
        }
    }

    fun deleteTeam(teamId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val team = teamDao.getTeamByIdOnce(teamId)
                if (team == null) {
                    onError("Team not found")
                    return@launch
                }

                if (team.isDefault) {
                    onError("Cannot delete default team")
                    return@launch
                }

                teamDao.deleteTeam(team)

                // Select another team after deletion
                val remainingTeams = allTeams.value.filter { it.teamId != teamId }
                _selectedTeamId.value = remainingTeams.firstOrNull()?.teamId

                onSuccess()
            } catch (e: Exception) {
                Log.e("PokeVM", "Failed to delete team", e)
                onError("Failed to delete team")
            }
        }
    }

    fun addToSpecificTeam(pokemonResult: PokemonResult, teamId: String) = viewModelScope.launch {
        addPokemonToTeamUseCase(pokemonResult.name, teamId)
    }

    fun removeFromTeam(pokemon: TeamMemberEntity) =
        viewModelScope.launch {
            removePokemonFromTeamUseCase(pokemon.name, pokemon.teamId)
        }

    fun isInTeam(name: String, teamId: String): Flow<Boolean> = teamDao.isInTeam(name, teamId)

    fun isInAnyTeam(name: String): Flow<Boolean> = allTeamsWithMembers
        .map { teams ->
            teams.any { team ->
                team.members.any { it.name.equals(name, ignoreCase = true) }
            }
        }

    @Deprecated("Use currentTeamMembers instead")
    val team: StateFlow<List<TeamMemberEntity>> = currentTeamMembers

    sealed class TeamAdditionResult {
        data class Success(val teamName: String, val wasAdded: Boolean) : TeamAdditionResult() // Add wasAdded flag
        object TeamFull : TeamAdditionResult()
        object AlreadyInTeam : TeamAdditionResult()
        data class Error(val message: String) : TeamAdditionResult()
    }

    fun togglePokemonInTeam(
        pokemonResult: PokemonResult,
        teamId: String,
        onResult: (TeamAdditionResult) -> Unit
    ) = viewModelScope.launch {
        try {
            val team = teamDao.getTeamByIdOnce(teamId)
            if (team == null) {
                onResult(TeamAdditionResult.Error("Team not found"))
                return@launch
            }

            val members = try {
                withTimeout(5000) {
                    teamDao.getTeamMembers(teamId).first()
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("PokeVM", "Timeout getting team members for $teamId")
                emptyList()
            } catch (e: Exception) {
                Log.e("PokeVM", "Error getting team members: ${e.message}")
                emptyList()
            }
            val isInTeam = members.any { it.name.equals(pokemonResult.name, ignoreCase = true) }

            if (isInTeam) {
                removePokemonFromTeamUseCase(pokemonResult.name, teamId)
                onResult(TeamAdditionResult.Success(team.teamName, wasAdded = false))
            } else {

                if (members.size >= 6) {
                    onResult(TeamAdditionResult.TeamFull)
                    return@launch
                }

                addPokemonToTeamUseCase(pokemonResult.name, teamId)

                onResult(TeamAdditionResult.Success(team.teamName, wasAdded = true))
            }
        } catch (e: Exception) {
            Log.e("PokeVM", "Failed to toggle Pokemon in team", e)
            onResult(TeamAdditionResult.Error(e.localizedMessage ?: "Unknown error"))
        }
    }

    fun isInSpecificTeam(pokemonName: String, teamId: String): Flow<Boolean> {
        return teamDao.isInTeam(pokemonName, teamId)
    }

    fun getTeamsForPokemon(pokemonName: String): Flow<List<TeamEntity>> {
        return allTeamsWithMembers.map { teams ->
            teams.filter { teamWithMembers ->
                teamWithMembers.members.any {
                    it.name.equals(pokemonName, ignoreCase = true)
                }
            }.map { it.team }
        }
    }

    fun getTeamMembers(teamId: String): Flow<List<TeamMemberEntity>> {
        return teamDao.getTeamMembers(teamId)
    }
}