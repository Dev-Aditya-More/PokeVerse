package com.aditya1875.pokeverse.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EvolutionSideItem(
    name: String,
    direction: ArrowDirection,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit
) {
    val contentAlpha = 0.85f
    val icon = if (direction == ArrowDirection.LEFT) {
        Icons.AutoMirrored.Filled.ArrowBack
    } else {
        Icons.AutoMirrored.Filled.ArrowForward
    }

    Column(
        modifier = modifier
            .width(72.dp)
            .clip(RoundedCornerShape(4.dp)) // Defines the ripple shape
            .clickable { onClick(name) }
            .padding(vertical = 4.dp), // Adds a bit of "breathing room" for the tap target
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = contentAlpha)
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = name.replaceFirstChar { it.titlecase() },
            style = MaterialTheme.typography.labelSmall, // Proper typography usage
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White.copy(alpha = contentAlpha),
            textAlign = TextAlign.Center
        )
    }
}
enum class ArrowDirection {
    LEFT, RIGHT
}
