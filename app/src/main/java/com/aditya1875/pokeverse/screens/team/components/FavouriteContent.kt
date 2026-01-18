package com.aditya1875.pokeverse.screens.team.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aditya1875.pokeverse.data.local.entity.FavouriteEntity

@Composable
fun FavoritesContent(
    favorites: List<FavouriteEntity>,
    navController: NavController,
    onRemove: (FavouriteEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (favorites.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.Star,
                title = "No Favorites",
                subtitle = "Add Pokemon to your favorites from the home screen",
                color = MaterialTheme.colorScheme.secondary // THEME-AWARE (yellow)
            )
        } else {
            // Favorites count card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface // THEME-AWARE
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
                        text = "Total Favorites",
                        color = MaterialTheme.colorScheme.onSurface, // THEME-AWARE
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = favorites.size.toString(),
                        color = MaterialTheme.colorScheme.secondary, // THEME-AWARE (yellow)
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Favorites List
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(favorites, key = { it.name }) { favorite ->
                    ImprovedFavoriteCard(
                        favorite = favorite,
                        navController = navController,
                        onRemove = onRemove
                    )
                }
            }
        }
    }
}
