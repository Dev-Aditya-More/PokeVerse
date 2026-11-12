package com.aditya1875.pokeverse.feature.pokemon.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.pokemon.dex.DexFilter
import com.aditya1875.pokeverse.feature.pokemon.dex.PokeBallIcon
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonFilter
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonType
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.Region
import com.aditya1875.pokeverse.utils.pokemonTypeColor

/**
 * Three rows of filters: region, type, and Pokédex progress. Every chip is the same pill ([FilterPill]); a selected
 * chip takes a tinted fill and a clear border in its own colour (the app's colour for regions and progress, the
 * type's colour for types), and nothing moves when the selection changes. Before, each of the five chip blocks was a
 * copy of the others with a bouncy scale and a check mark that pushed the chip wider when it was picked.
 */
@Composable
fun FilterBar(
    currentFilter: PokemonFilter,
    onRegionChange: (Region?) -> Unit,
    onTypeChange: (PokemonType?) -> Unit,
    modifier: Modifier = Modifier,
    isTypeFiltering: Boolean,
    dexFilter: DexFilter = DexFilter.ALL,
    onDexFilterChange: (DexFilter) -> Unit = {}
) {
    val primary = MaterialTheme.colorScheme.primary
    val rowPadding = PaddingValues(horizontal = 4.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Region
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = rowPadding,
            modifier = Modifier.fillMaxWidth()
        ) {
            item(key = "region_all") {
                FilterPill(
                    label = stringResource(R.string.filter_all_regions),
                    selected = currentFilter.selectedRegion == null,
                    accent = primary,
                    onClick = { onRegionChange(null) },
                )
            }
            items(Region.entries.toTypedArray(), key = { "region_${it.name}" }) { region ->
                FilterPill(
                    label = region.displayName,
                    selected = currentFilter.selectedRegion == region,
                    accent = primary,
                    onClick = { onRegionChange(region) },
                )
            }
        }

        // Type: each chip carries its own colour, so the row reads as a palette instead of 19 identical pills.
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = rowPadding,
            modifier = Modifier.fillMaxWidth()
        ) {
            item(key = "type_all") {
                FilterPill(
                    label = stringResource(R.string.filter_all_types),
                    selected = currentFilter.selectedType == null,
                    accent = primary,
                    onClick = { onTypeChange(null) },
                )
            }
            items(PokemonType.entries.toTypedArray(), key = { "type_${it.name}" }) { type ->
                val selected = currentFilter.selectedType == type
                val color = pokemonTypeColor(type.name.lowercase())
                FilterPill(
                    label = type.displayName,
                    selected = selected,
                    accent = color,
                    onClick = { onTypeChange(type) },
                    leading = {
                        if (selected && isTypeFiltering) {
                            // The type's list is being fetched.
                            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp, color = color)
                        } else {
                            Box(Modifier.size(8.dp).background(color, CircleShape))
                        }
                    },
                )
            }
        }

        // Pokédex progress
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = rowPadding,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(DexFilter.entries.toTypedArray(), key = { "dex_${it.name}" }) { filter ->
                FilterPill(
                    label = stringResource(
                        when (filter) {
                            DexFilter.ALL -> R.string.dex_filter_all
                            DexFilter.SEEN -> R.string.dex_seen
                            DexFilter.CAUGHT -> R.string.dex_caught
                        }
                    ),
                    selected = dexFilter == filter,
                    accent = if (filter == DexFilter.CAUGHT) Color(0xFFEF5350) else primary,
                    onClick = { onDexFilterChange(filter) },
                    leading = if (filter == DexFilter.CAUGHT) {
                        { PokeBallIcon(Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null,
                )
            }
        }
    }
}

/** One filter chip: a pill with a quiet outline, and a tinted fill plus a clear border in [accent] when selected. */
@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = leading,
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = ink.copy(alpha = 0.78f),
            selectedContainerColor = accent.copy(alpha = 0.20f),
            selectedLabelColor = accent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = ink.copy(alpha = 0.16f),
            selectedBorderColor = accent.copy(alpha = 0.80f),
            borderWidth = 1.dp,
            selectedBorderWidth = 1.5.dp,
        ),
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
