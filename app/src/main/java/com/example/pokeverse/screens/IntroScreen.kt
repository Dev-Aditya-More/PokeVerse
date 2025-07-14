package com.example.pokeverse.screens

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.example.pokeverse.R
import com.example.pokeverse.utils.ScreenStateManager
import com.example.pokeverse.utils.ScreenStateManager.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IntroScreen(
    navController: NavController,
    context: Context = LocalContext.current
) {
    val imageList = listOf(
        painterResource(id = R.drawable.intro_screen),
        painterResource(id = R.drawable.intro_screen2),
        painterResource(id = R.drawable.intro_screen3)
    )

    val pageCount = imageList.size
    var currentPage by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Auto-slide every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentPage = (currentPage + 1) % pageCount
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // Crossfade image transition
        Crossfade(
            targetState = currentPage,
            animationSpec = tween(durationMillis = 600),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Image(
                painter = imageList[page],
                contentDescription = "Intro Image $page",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dot indicators
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        ) {
            repeat(pageCount) { index ->
                val isSelected = currentPage == index
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (isSelected) 10.dp else 6.dp)
                        .background(
                            color = if (isSelected) Color.White else Color.Gray,
                            shape = CircleShape
                        )
                )
            }
        }

        Button(
            onClick = {
                scope.launch {
                    // Save intro seen and route
                    context.dataStore.edit { prefs ->
                        prefs[ScreenStateManager.INTRO_SEEN] = true
                        prefs[ScreenStateManager.LAST_ROUTE] = "home"
                    }

                    navController.navigate("home") {
                        popUpTo("intro") { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 32.dp, end = 32.dp)
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F1F))
        ) {
            Text(
                text = "Continue",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
