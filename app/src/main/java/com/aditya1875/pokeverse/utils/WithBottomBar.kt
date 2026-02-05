package com.aditya1875.pokeverse.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aditya1875.pokeverse.presentation.screens.home.components.BottomNavigationBar
import com.aditya1875.pokeverse.presentation.screens.home.components.Route

@Composable
fun WithBottomBar(
    navController: NavHostController,
    selectedRoute: Route = Route.BottomBar.Home,
    onRouteChange: (Route) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedRoute = selectedRoute,
                onRouteChange = onRouteChange
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            content(padding)
        }
    }
}