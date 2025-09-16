package com.example.pokeverse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pokeverse.data.remote.model.PokemonFilter
import com.example.pokeverse.data.remote.model.Region

@Composable
fun FilterBar(
    currentFilter: PokemonFilter,
    onRegionChange: (Region?) -> Unit,
) {

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
            .padding(top = 8.dp)
    ) {
        item {
            FilterChip(
                selected = currentFilter.selectedRegion == null,
                onClick = { onRegionChange(null) },
                label = { Text("All Regions") },
                modifier = Modifier.padding(vertical = 4.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF802525),
                    selectedLabelColor = Color.White
                )
            )
        }
        items(Region.entries.toTypedArray()) { region ->
            FilterChip(
                selected = currentFilter.selectedRegion == region,
                onClick = { onRegionChange(region) },
                label = { Text(region.displayName) },
                modifier = Modifier.padding(vertical = 4.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF802525),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}
