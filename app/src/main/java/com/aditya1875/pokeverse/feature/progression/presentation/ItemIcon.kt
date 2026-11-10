package com.aditya1875.pokeverse.feature.progression.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.feature.core.ui.components.ShardGem
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType
import com.aditya1875.pokeverse.feature.game.wildcatch.presentation.components.BallIcon
import com.aditya1875.pokeverse.feature.progression.domain.ShopItem
import com.aditya1875.pokeverse.feature.progression.domain.ShopItems
import androidx.compose.ui.graphics.Color

/** The official item sprite for a PokeAPI item name (same sprite source the app already uses for Pokémon). */
fun itemSpriteUrl(apiName: String) =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/$apiName.png"

/**
 * The picture of a shop item: the real item sprite for stones and held items, the drawn ball for balls, Honey
 * for the Rare Lure (the item that draws Pokémon in), and a cut gem for the shard pouch. No emoji.
 */
@Composable
fun ItemIcon(item: ShopItem, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    val ball = BallType.entries.firstOrNull { it.itemId == item.id }
    when {
        ball != null -> BallIcon(ball, size, modifier)
        item.id == ShopItems.ID_SHARD_POUCH -> ShardGem(size * 0.8f, Color(0xFF7DD3FC), modifier)
        else -> AsyncImage(
            model = itemSpriteUrl(if (item.id == ShopItems.ID_LURE) "honey" else item.id),
            contentDescription = null,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
            // The sprites are small pixel art: keep them crisp instead of smoothing them into a blur.
            filterQuality = FilterQuality.None,
        )
    }
}
