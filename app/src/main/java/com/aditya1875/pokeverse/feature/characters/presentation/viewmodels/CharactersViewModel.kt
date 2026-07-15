package com.aditya1875.pokeverse.feature.characters.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.characters.domain.CharacterRepository
import com.aditya1875.pokeverse.feature.characters.domain.PokeCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class CharactersViewModel(
    private val repository: CharacterRepository
) : ViewModel() {

    val roles: List<String> = repository.getRoles()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedRole = MutableStateFlow<String?>(null)
    val selectedRole: StateFlow<String?> = _selectedRole.asStateFlow()

    val characters: StateFlow<List<PokeCharacter>> =
        combine(_searchQuery, _selectedRole) { query, role ->
            val q = query.trim().lowercase()
            repository.getCharacters().filter { character ->
                (role == null || character.role == role) &&
                        (q.isEmpty() ||
                                character.name.lowercase().contains(q) ||
                                character.role.lowercase().contains(q) ||
                                character.region.lowercase().contains(q) ||
                                character.signature.lowercase().contains(q))
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.getCharacters())

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    fun onRoleSelect(role: String?) {
        _selectedRole.value = if (_selectedRole.value == role) null else role
    }
}
