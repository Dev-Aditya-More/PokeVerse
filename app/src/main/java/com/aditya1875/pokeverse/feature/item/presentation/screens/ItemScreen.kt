package com.aditya1875.pokeverse.feature.item.presentation.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.feature.item.data.source.remote.model.itemModels.ItemUiModel
import com.aditya1875.pokeverse.feature.item.presentation.viewmodels.ItemListState
import com.aditya1875.pokeverse.feature.item.presentation.viewmodels.ItemViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SharedTransitionScope.ItemGridCard(
    item: ItemUiModel,
    onClick: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope
) {

    val accent = Color(item.categoryColor)

    val key = "item-${item.id}"

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .graphicsLayer {
                shadowElevation = 8f
                shape = RoundedCornerShape(20.dp)
                clip = true
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f))
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = key),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = item.spriteUrl,
                    contentDescription = item.displayName,
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    item.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    item.categoryDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ItemGridSkeleton() {
    val shimmer by rememberInfiniteTransition(label = "s").animateFloat(
        0.3f, 0.7f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(), userScrollEnabled = false) {
        items(18) {
            Box(Modifier.fillMaxWidth().aspectRatio(0.85f).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmer)))
        }
    }
}

@Composable
fun ItemListError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("⚠️", fontSize = 40.sp); Spacer(Modifier.height(8.dp))
        Text("Failed to load items", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}