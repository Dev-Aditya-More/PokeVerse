package com.aditya1875.pokeverse.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(showBackground = true)
@Composable
fun PokemonNotFoundScreen(
    onRetryClick: () -> Unit = {}
) {
    val transition = rememberInfiniteTransition()
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        )
    )

    val animatedGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF1C1C1C), Color(0xFF262626), Color(0xFF1C1C1C)),
        start = Offset(offset, 0f),
        end = Offset(offset + 300f, 1000f)
    )

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedGradient)
                .padding(padding)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Icon with soft bounce
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFFBDBDBD),
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            shadowElevation = 12.dp.toPx()
                            alpha = 0.9f
                        }
                )

                Text(
                    text = "No Pokémon Found",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "We couldn’t find that Pokémon.\nPlease check the spelling or try again.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFB0B0B0),
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onRetryClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEE4B2B),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Try Again",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )
                    )
                }
            }
        }
    }
}
