package com.example.pokeverse.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.pokeverse.components.CustomProgressIndicator
import com.example.pokeverse.components.ResponsiveMetaballSwitch
import com.example.pokeverse.ui.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val pokeballGradient = remember {
        Brush.verticalGradient(
            listOf(Color(0xFF2E2E2E), Color(0xFF1A1A1A))
        )
    }

    var isAboutExpanded by remember { mutableStateOf(false) }
    var isSpecialEffectsExpanded by remember { mutableStateOf(false) }
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val specialEffectsEnabled by settingsViewModel.specialEffectsEnabled.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings ",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pokeballGradient)
                .padding(padding)
        ) {
            // Pokéball watermark
            CustomProgressIndicator(
                Modifier
                    .size(300.dp)
                    .align(Alignment.Center)
                    .size(280.dp)
                    .alpha(0.08f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // About Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAboutExpanded = !isAboutExpanded }
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("About", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                            Icon(
                                imageVector = if (isAboutExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                tint = Color.White
                            )
                        }

                        AnimatedVisibility(visible = isAboutExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text("Made with ❤️ by Aditya More", color = Color.Gray)
                                Text("Version: 1.0.2", color = Color.Gray)
                                Text("Built with Jetpack Compose", color = Color.Gray)
                            }
                        }
                    }
                }

                // Special Effects Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSpecialEffectsExpanded = !isSpecialEffectsExpanded }
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD54F))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Special Effects",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                            ResponsiveMetaballSwitch(
                                checked = specialEffectsEnabled,
                                onCheckedChange = { settingsViewModel.toggleSpecialEffects(it) },
                                enabled = true
                            )
                            Icon(
                                imageVector = if (isSpecialEffectsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                tint = Color.White
                            )
                        }

                        AnimatedVisibility(visible = isSpecialEffectsExpanded) {
                            Text(
                                "You'll see the particle effect \n(Try pressing the Pokémon sprite)",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
