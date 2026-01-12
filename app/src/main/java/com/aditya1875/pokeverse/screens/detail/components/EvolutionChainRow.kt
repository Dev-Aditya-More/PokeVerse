package com.aditya1875.pokeverse.screens.detail.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionChainUi

@Composable
fun EvolutionChainRow(
    evolution: EvolutionChainUi,
    onPokemonClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {

        // LEFT (previous)
        evolution.previous?.let { prev ->
            EvolutionSideItem(
                name = prev.name,
                direction = ArrowDirection.LEFT,
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = onPokemonClick
            )
        }

        // RIGHT (next)
        evolution.next?.let { next ->
            EvolutionSideItem(
                name = next.name,
                direction = ArrowDirection.RIGHT,
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = onPokemonClick
            )
        }
    }
}
