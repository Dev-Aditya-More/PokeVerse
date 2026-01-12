package com.aditya1875.pokeverse.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.aditya1875.pokeverse.screens.home.components.BottomNavigationBar

@Composable
fun WithBottomBar(navController: NavHostController, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        containerColor = Color.Black,
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .consumeWindowInsets(padding)
        ) {
            content(padding)
        }
    }
}