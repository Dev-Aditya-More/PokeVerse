package com.example.pokeverse.domain.repository

import com.example.pokeverse.data.local.dao.PokemonDao
import com.example.pokeverse.data.local.entity.PokemonDetailEntity
import com.example.pokeverse.data.local.entity.PokemonListEntity
import com.example.pokeverse.data.remote.PokeApi
import com.example.pokeverse.data.remote.model.PokemonDescription
import com.example.pokeverse.data.remote.model.PokemonResponse
import com.example.pokeverse.data.remote.model.PokemonResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PokemonLocalRepo(
    private val api: PokeApi,
    private val dao: PokemonDao,
    private val gson: Gson
) {

    suspend fun getPokemonList(page: Int): List<PokemonResult> {
        val cached = dao.getPokemonList(page)
        return if (cached != null) {
            val type = object : TypeToken<List<PokemonResult>>() {}.type
            gson.fromJson(cached.resultsJson, type)
        } else {
            val response = api.getPokemonList(offset = page * 20)
            val json = gson.toJson(response.results)
            dao.insertPokemonList(PokemonListEntity(page, json))
            response.results
        }
    }

    suspend fun getPokemonDetail(name: String): PokemonResponse {
        val cached = dao.getPokemonDetail(name)
        return if (cached != null) {
            gson.fromJson(cached.responseJson, PokemonResponse::class.java)
        } else {
            val response = api.getPokemonByName(name)
            val json = gson.toJson(response)
            dao.insertPokemonDetail(PokemonDetailEntity(name, json))
            response
        }
    }
}
