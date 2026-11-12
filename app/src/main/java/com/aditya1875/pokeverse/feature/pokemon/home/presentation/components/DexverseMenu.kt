package com.aditya1875.pokeverse.feature.pokemon.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R

/**
 * The menu behind the "Dexverse" title: what the Home list shows, and the other tools.
 *
 * Every row is built the same way (a rounded tinted tile with its own icon, then the label), so the icons share one
 * size, weight and alignment, and the two sections line up. The mode on screen is highlighted. Before this the
 * Pokédex entries had no icons at all, the tools had four unrelated ones, and one label wrapped to two lines.
 */
@Composable
fun DexverseMenu(
    expanded: Boolean,
    contentMode: HomeContentMode,
    onDismiss: () -> Unit,
    onContentMode: (HomeContentMode) -> Unit,
    onDexMode: () -> Unit,
    onFaceMatch: () -> Unit,
    onCompare: () -> Unit,
    onStatCalculator: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        // A touch of the theme colour in the surface, so the popup belongs to the app instead of being flat grey.
        containerColor = lerp(colors.surface, colors.primary, 0.08f),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        modifier = Modifier.widthIn(min = 300.dp)
    ) {
        SectionLabel(stringResource(R.string.home_section_pokedex))
        ModeRow(Icons.Default.CatchingPokemon, R.string.home_menu_pokemon, contentMode == HomeContentMode.POKEMON, colors.primary) { onContentMode(HomeContentMode.POKEMON) }
        ModeRow(Icons.Default.Backpack, R.string.home_menu_items, contentMode == HomeContentMode.ITEMS, colors.primary) { onContentMode(HomeContentMode.ITEMS) }
        ModeRow(Icons.Default.Spa, R.string.home_menu_berries, contentMode == HomeContentMode.BERRIES, colors.primary) { onContentMode(HomeContentMode.BERRIES) }
        ModeRow(Icons.Default.MilitaryTech, R.string.home_menu_badges, contentMode == HomeContentMode.BADGES, colors.primary) { onContentMode(HomeContentMode.BADGES) }
        ModeRow(Icons.Default.Face, R.string.home_menu_characters, contentMode == HomeContentMode.CHARACTERS, colors.primary) { onContentMode(HomeContentMode.CHARACTERS) }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            color = colors.onSurface.copy(alpha = 0.10f)
        )

        SectionLabel(stringResource(R.string.home_section_other_tools))
        // One accent per tool, all drawn the same way, so the list is colourful without being mismatched.
        ModeRow(Icons.Default.PhoneAndroid, R.string.dex_menu_title, false, Color(0xFFFF7043), bold = true, onClick = onDexMode)
        ModeRow(Icons.Default.CameraAlt, R.string.home_menu_face_match, false, Color(0xFFBA68C8), onClick = onFaceMatch)
        ModeRow(Icons.Default.CompareArrows, R.string.home_menu_compare, false, Color(0xFF4FC3F7), onClick = onCompare)
        ModeRow(Icons.Default.Calculate, R.string.home_menu_stat_calculator, false, Color(0xFFFFCA28), onClick = onStatCalculator)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

/** One row: a tinted icon tile, the label, and a check when it is the mode currently shown. */
@Composable
private fun ModeRow(
    icon: ImageVector,
    label: Int,
    selected: Boolean,
    tint: Color,
    bold: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(shape)
            .background(if (selected) tint.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = if (selected) 0.28f else 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected || bold) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) tint else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        } else {
            Spacer(Modifier.size(18.dp))
        }
    }
}
