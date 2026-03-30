package com.aditya1875.pokeverse.feature.analysis.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.BG
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.BLUE

@Composable
fun LoadingView() {
    Box(Modifier.fillMaxSize().background(BG), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = BLUE, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
            Text("Analyzing your team…",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Calculating type coverage and synergies",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f))
        }
    }
}
