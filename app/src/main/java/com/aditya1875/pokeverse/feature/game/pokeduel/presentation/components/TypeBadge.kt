package com.aditya1875.pokeverse.feature.game.pokeduel.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TypeBadge(type: String) {
    Text(
        text = type.replaceFirstChar { it.uppercase() },
        modifier = Modifier
            .background(typeColor(type), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

fun typeColor(type: String): Color = when (type.lowercase()) {
    "fire" -> Color(0xFFFF6B35)
    "water" -> Color(0xFF4FC3F7)
    "grass" -> Color(0xFF66BB6A)
    "electric" -> Color(0xFFFFD54F)
    "ice" -> Color(0xFF80DEEA)
    "fighting" -> Color(0xFFEF5350)
    "poison" -> Color(0xFFAB47BC)
    "ground" -> Color(0xFFD4A96A)
    "flying" -> Color(0xFF90CAF9)
    "psychic" -> Color(0xFFF06292)
    "bug" -> Color(0xFF8BC34A)
    "rock" -> Color(0xFFB0A090)
    "ghost" -> Color(0xFF7E57C2)
    "dragon" -> Color(0xFF5C6BC0)
    "dark" -> Color(0xFF5D4037)
    "steel" -> Color(0xFF90A4AE)
    "fairy" -> Color(0xFFF48FB1)
    else -> Color(0xFF9E9E9E)
}