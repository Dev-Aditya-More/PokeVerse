package com.example.pokeverse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeverse.data.remote.model.PokemonListResponse
import com.example.pokeverse.domain.repository.PokemonRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PokemonViewModel(
    private val repository: PokemonRepo
) : ViewModel() {

    private val _pokemonList = MutableStateFlow<PokemonListResponse?>(null)
    val pokemonList: StateFlow<PokemonListResponse?> = _pokemonList

    fun loadPokemonList(limit: Int = 50, offset: Int = 0) {
        viewModelScope.launch {
            try {
                val result = repository.getPokemonList(limit, offset)
                _pokemonList.value = result
            } catch (e: Exception) {
                // Handle error (add error state if needed)
                _pokemonList.value = null
            }
        }
    }
}