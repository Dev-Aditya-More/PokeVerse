package com.aditya1875.pokeverse.presentation.screens.analysis.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OverallScoreCard(score: Int) {
    val scoreColor = when {
        score >= 80 -> Color(0xFF00E676)
        score >= 60 -> Color(0xFF00BCD4)
        score >= 40 -> Color(0xFFFFA726)
        else -> Color(0xFFFF6B6B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            scoreColor.copy(alpha = 0.15f),
                            scoreColor.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            scoreColor.copy(alpha = 0.3f),
                            scoreColor.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TEAM RATING",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                // Circular score with animation potential
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    // Background circle
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 16.dp.toPx())
                        )
                    }

                    // Progress circle
                    CircularProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = scoreColor,
                        strokeWidth = 16.dp,
                        trackColor = Color.Transparent
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 56.sp
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = when {
                                score >= 80 -> "EXCELLENT"
                                score >= 60 -> "GOOD"
                                score >= 40 -> "FAIR"
                                else -> "POOR"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = scoreColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = when {
                        score >= 80 -> "🔥 Battle-ready team with exceptional balance"
                        score >= 60 -> "💪 Strong foundation with minor improvements needed"
                        score >= 40 -> "⚡ Solid core but requires optimization"
                        else -> "🛠️ Team needs significant restructuring"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}