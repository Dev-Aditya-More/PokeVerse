package com.aditya1875.pokeverse.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.data.remote.model.PokemonFilter
import com.aditya1875.pokeverse.data.remote.model.Region

@Composable
fun FilterBar(
    currentFilter: PokemonFilter,
    onRegionChange: (Region?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp)
    ) {
        item {
            val selected = currentFilter.selectedRegion == null
            FilterChip(
                onClick = { onRegionChange(null) },
                label = { Text("All Regions") },
                selected = selected,
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Selected",
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                            tint = MaterialTheme.colorScheme.onPrimary // THEME-AWARE
                        )
                    }
                } else null,
                modifier = Modifier.padding(vertical = 4.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, // THEME-AWARE
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer // THEME-AWARE
                )
            )
        }

        items(Region.entries.toTypedArray()) { region ->
            val selected = currentFilter.selectedRegion == region
            FilterChip(
                onClick = { onRegionChange(region) },
                label = { Text(region.displayName) },
                selected = selected,
                leadingIcon = if (selected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Selected",
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else null,
                modifier = Modifier.padding(vertical = 4.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}


@Preview
@Composable
fun FilterPreview(modifier: Modifier = Modifier) {

    FilterBar(
        currentFilter = PokemonFilter(),
        onRegionChange = {
            println("Selected region: $it")
        }
    )
}