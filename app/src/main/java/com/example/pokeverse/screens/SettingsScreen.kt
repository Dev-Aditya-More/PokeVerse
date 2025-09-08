package com.example.pokeverse.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.pokeverse.ui.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
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
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pokeballGradient)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Expandable About Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAboutExpanded = !isAboutExpanded }
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Icon(
                        imageVector = if (isAboutExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }

                AnimatedVisibility(visible = isAboutExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Made with ❤️ by Aditya More", color = Color.Gray)
                        Text("Version: 1.0.2", color = Color.Gray)
                        Text("Built with Jetpack Compose", color = Color.Gray)
                    }
                }
            }

            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isSpecialEffectsExpanded = !isSpecialEffectsExpanded }
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Special Effects (Experimental)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    MetaballSwitch(
                        checked = specialEffectsEnabled,
                        onCheckedChange = { settingsViewModel.toggleSpecialEffects(it) },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Icon(
                        imageVector = if (isSpecialEffectsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }

                AnimatedVisibility(visible = isSpecialEffectsExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("You'll see the particle effect \n(Try pressing the pokemon sprite)", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }


        }
    }
}
