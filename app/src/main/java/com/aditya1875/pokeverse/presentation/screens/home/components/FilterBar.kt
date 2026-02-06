package com.aditya1875.pokeverse.presentation.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.aditya1875.pokeverse.data.remote.model.PokemonType
import com.aditya1875.pokeverse.data.remote.model.Region

@Composable
fun FilterBar(
    currentFilter: PokemonFilter,
    onRegionChange: (Region?) -> Unit,
    onTypeChange: (PokemonType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // Region Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
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
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else null,
                    modifier = Modifier.padding(vertical = 2.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    modifier = Modifier.padding(vertical = 2.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Type Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                val selected = currentFilter.selectedType == null
                FilterChip(
                    onClick = { onTypeChange(null) },
                    label = { Text("All Types") },
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
                    modifier = Modifier.padding(vertical = 2.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            items(PokemonType.entries.toTypedArray()) { type ->
                val selected = currentFilter.selectedType == type
                FilterChip(
                    onClick = { onTypeChange(type) },
                    label = { Text(type.displayName) },
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
                    modifier = Modifier.padding(vertical = 2.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}