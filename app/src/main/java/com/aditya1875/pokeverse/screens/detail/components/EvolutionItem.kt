package com.aditya1875.pokeverse.screens.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EvolutionSideItem(
    name : String,
    direction: ArrowDirection,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    val contentAlpha = 0.85f
    val icon = if (direction == ArrowDirection.LEFT) {
        Icons.AutoMirrored.Filled.ArrowBackIos
    } else {
        Icons.AutoMirrored.Filled.ArrowForwardIos
    }

    Column(
        modifier = modifier
            .width(72.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick(name) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
        )
    }
}
enum class ArrowDirection {
    LEFT, RIGHT
}
