package com.aditya1875.pokeverse.presentation.screens.item

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
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.data.remote.model.itemModels.ItemUiModel
import com.aditya1875.pokeverse.presentation.ui.viewmodel.ItemListState
import com.aditya1875.pokeverse.presentation.ui.viewmodel.ItemViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ItemListScreen(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemViewModel = koinViewModel()
) {
    val listState by viewModel.listState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= filteredItems.size - 8 &&
                    listState is ItemListState.Success &&
                    (listState as ItemListState.Success).canLoadMore &&
                    searchQuery.isBlank()
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar — same padding/style as Pokémon search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchChange(it) },
            label = { Text("Search items..") },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchChange("") }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant)
        )

        when (val state = listState) {
            is ItemListState.Loading -> ItemGridSkeleton()
            is ItemListState.Error   -> ItemListError(state.message) { viewModel.loadItems() }
            is ItemListState.Success -> {
                val displayList = if (searchQuery.isNotEmpty()) filteredItems else state.items
                if (displayList.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("No items found", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayList, key = { it.id }) { item ->
                            ItemGridCard(item = item, onClick = { onItemClick(item.name) })
                        }
                        if (state.isLoadingMore) {
                            item(span = { GridItemSpan(3) }) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemGridCard(item: ItemUiModel, onClick: () -> Unit) {
    val accentColor = Color(item.categoryColor)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center) {
                if (item.spriteUrl.isNotEmpty()) {
                    AsyncImage(model = item.spriteUrl, contentDescription = item.displayName,
                        modifier = Modifier.size(40.dp), contentScale = ContentScale.Fit)
                } else {
                    Text("?", fontSize = 20.sp, color = accentColor)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(item.displayName, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, lineHeight = 15.sp)
            Spacer(Modifier.height(4.dp))
            Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.15f)) {
                Text(item.categoryDisplay.split(" ").firstOrNull() ?: item.categoryDisplay,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall, color = accentColor, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ItemGridSkeleton() {
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
private fun ItemListError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("⚠️", fontSize = 40.sp); Spacer(Modifier.height(8.dp))
        Text("Failed to load items", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}