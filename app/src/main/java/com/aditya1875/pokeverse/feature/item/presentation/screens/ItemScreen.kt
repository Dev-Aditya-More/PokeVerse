package com.aditya1875.pokeverse.feature.item.presentation.screens

import com.aditya1875.pokeverse.feature.core.ui.components.LoadErrorState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.core.ui.components.CatalogTile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.FilterQuality
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
    CatalogTile(
        spriteUrl = item.spriteUrl,
        name = item.displayName,
        caption = item.categoryDisplay,
        accent = Color(item.categoryColor),
        onClick = onClick,
        discModifier = Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = "item-${item.id}"),
            animatedVisibilityScope = animatedVisibilityScope,
        ),
    )
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
            Box(Modifier.fillMaxWidth().aspectRatio(0.82f).clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmer)))
        }
    }
}

@Composable
fun ItemListError(message: String, onRetry: () -> Unit) {
    LoadErrorState(onRetry = onRetry)
}