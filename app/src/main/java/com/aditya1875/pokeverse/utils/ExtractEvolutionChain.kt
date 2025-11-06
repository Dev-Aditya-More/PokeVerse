package com.aditya1875.pokeverse.utils

import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionNode
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionStage

fun extractEvolutionChain(root: EvolutionNode): List<EvolutionStage> {
    val list = mutableListOf<EvolutionStage>()

    fun traverse(node: EvolutionNode) {
        val imageUrl = getImageUrlFromUrl(node.url)
        list += EvolutionStage(node.name.replaceFirstChar { it.uppercase() }, imageUrl)
        node.evolvesTo.forEach { traverse(it) }
    }

    traverse(root)
    return list
}

fun getImageUrlFromUrl(url: String): String {
    val id = url.trimEnd('/').split("/").last()
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/other/official-artwork/$id.png"
}
