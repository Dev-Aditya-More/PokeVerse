package com.aditya1875.pokeverse.feature.team.presentation.components

import com.aditya1875.pokeverse.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aditya1875.pokeverse.feature.team.data.local.entity.FavouriteEntity

/** The favorites tab as list items, so it scrolls with the page instead of sitting under pinned headers. */
fun LazyListScope.favoriteContentItems(
    favorites: List<FavouriteEntity>,
    navController: NavController,
    onRemove: (FavouriteEntity) -> Unit,
    assetsEnabled: Boolean
) {
    if (favorites.isEmpty()) {
        item(key = "empty") {
            EmptyStateCard(
        icon = Icons.Default.Star,
        title = stringResource(R.string.favorites_empty_title),
        subtitle = stringResource(R.string.favorites_empty_subtitle),
        color = MaterialTheme.colorScheme.secondary
    )
        }
        return
    }

    item(key = "count") {
        Column {
            // Favorites count card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.favorites_total),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = favorites.size.toString(),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    itemsIndexed(favorites, key = { index, it -> "${it.name}_$index" }) { _, favorite ->
        Box(Modifier.padding(bottom = 12.dp)) {
            ImprovedFavoriteCard(
                favorite = favorite,
                navController = navController,
                onRemove = onRemove,
                assetsEnabled = assetsEnabled
            )
        }
    }
}
