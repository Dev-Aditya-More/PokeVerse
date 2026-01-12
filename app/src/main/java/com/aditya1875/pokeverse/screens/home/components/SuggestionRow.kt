package com.aditya1875.pokeverse.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.data.remote.model.PokemonResult
import com.aditya1875.pokeverse.data.remote.model.PokemonSearchIndex

@Composable
fun SuggestionRow(
    pokemon: PokemonResult,
    onClick: () -> Unit
) {
    val pokemonId = remember(pokemon.url) {
        pokemon.url.trimEnd('/')
            .split("/")
            .lastOrNull()
            ?.toIntOrNull()
    }

    val spriteUrl = pokemonId?.let {
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$it.png"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Sprite
        AsyncImage(
            model = spriteUrl,
            contentDescription = pokemon.name,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f)),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = pokemon.name.replaceFirstChar { it.uppercase() },
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )

            if (pokemonId != null) {
                Text(
                    text = "#${pokemonId.toString().padStart(4, '0')}",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
