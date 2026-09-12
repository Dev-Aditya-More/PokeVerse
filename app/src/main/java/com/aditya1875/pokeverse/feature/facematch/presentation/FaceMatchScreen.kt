package com.aditya1875.pokeverse.feature.facematch.presentation

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.utils.SoundManager
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private val FaceMatchGold = Color(0xFFFFC107)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceMatchScreen(
    onBack: () -> Unit,
    viewModel: FaceMatchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val soundManager: SoundManager = koinInject()
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) soundManager.play(SoundManager.Sound.BUTTON_CLICK)
        viewModel.analyze(bitmap)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What Pokémon Do I Look Like?") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.95f)).togetherWith(fadeOut())
                },
                label = "face_match_state"
            ) { s ->
                when (s) {
                    is FaceMatchState.Idle -> IdleContent(
                        hasCamera = hasCamera,
                        onTakePhoto = { cameraLauncher.launch(null) }
                    )
                    is FaceMatchState.Analyzing -> AnalyzingContent(photo = s.photo)
                    is FaceMatchState.Error -> ErrorContent(
                        message = s.message,
                        onRetry = { cameraLauncher.launch(null) },
                        onCancel = { viewModel.reset() }
                    )
                    is FaceMatchState.Result -> ResultContent(
                        result = s,
                        onTryAgain = { cameraLauncher.launch(null) },
                        onDone = { viewModel.reset() }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(hasCamera: Boolean, onTakePhoto: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "idle_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            // breathing glow behind the viewfinder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(FaceMatchGold.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
            )

            // viewfinder corner brackets — hints "this is a scanner", not just an icon
            Canvas(modifier = Modifier.size(150.dp)) {
                val len = 22.dp.toPx()
                val stroke = 3.dp.toPx()
                val inset = 4.dp.toPx()
                val w = size.width
                val h = size.height
                val corners = listOf(
                    Offset(inset, inset) to listOf(Offset(inset, inset + len), Offset(inset, inset), Offset(inset + len, inset)),
                    Offset(w - inset, inset) to listOf(Offset(w - inset - len, inset), Offset(w - inset, inset), Offset(w - inset, inset + len)),
                    Offset(inset, h - inset) to listOf(Offset(inset, h - inset - len), Offset(inset, h - inset), Offset(inset + len, h - inset)),
                    Offset(w - inset, h - inset) to listOf(Offset(w - inset - len, h - inset), Offset(w - inset, h - inset), Offset(w - inset, h - inset - len))
                )
                corners.forEach { (_, pts) ->
                    drawPoints(
                        points = pts,
                        pointMode = androidx.compose.ui.graphics.PointMode.Polygon,
                        color = FaceMatchGold.copy(alpha = 0.8f),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            Icon(
                Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = FaceMatchGold
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Which Pokémon do you look like?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Take a selfie and find out! Just for fun — your photo never leaves your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        if (hasCamera) {
            Button(
                onClick = onTakePhoto,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FaceMatchGold, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Take a Selfie", fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                "No camera found on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val analyzingPhrases = listOf(
    "Reading your features...",
    "Cross-referencing the Pokédex...",
    "Almost there..."
)

@Composable
private fun AnalyzingContent(photo: Bitmap) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_y"
    )

    var phraseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(470)
            phraseIndex = (phraseIndex + 1) % analyzingPhrases.size
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, FaceMatchGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
        ) {
            androidx.compose.foundation.Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // scan-line sweep
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .graphicsLayer { translationY = scanY * 200.dp.toPx() }
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, FaceMatchGold, Color.Transparent)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                        )
                    )
            )
        }

        Spacer(Modifier.height(24.dp))

        AnimatedContent(
            targetState = phraseIndex,
            transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(200))) },
            label = "analyzing_phrase"
        ) { idx ->
            Text(
                analyzingPhrases[idx],
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("😕", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Try Again", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Cancel")
        }
    }
}

@Composable
private fun ResultContent(
    result: FaceMatchState.Result,
    onTryAgain: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val soundManager: SoundManager = koinInject()
    val haptic = LocalHapticFeedback.current
    val match = result.match

    var showConfetti by remember { mutableStateOf(false) }
    val revealAlpha = remember { Animatable(0f) }
    val revealScale = remember { Animatable(0.7f) }
    val selfieAlpha = remember { Animatable(0f) }

    LaunchedEffect(match) {
        selfieAlpha.animateTo(1f, tween(250))
        delay(150)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        soundManager.play(SoundManager.Sound.MATCH_FOUND)
        showConfetti = true
        revealAlpha.animateTo(1f, tween(320))
        revealScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = showConfetti) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever
            )
            LottieAnimation(
                composition = composition,
                progress = progress,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // your selfie, small — grounds the reveal in "you", not just a random Pokémon
            androidx.compose.foundation.Image(
                bitmap = result.photo.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer { alpha = selfieAlpha.value }
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "You look like...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        alpha = revealAlpha.value
                        scaleX = revealScale.value
                        scaleY = revealScale.value
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(FaceMatchGold.copy(alpha = 0.25f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = match.spriteUrl,
                    contentDescription = match.displayName,
                    modifier = Modifier.size(160.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                match.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.graphicsLayer { alpha = revealAlpha.value }
            )

            Spacer(Modifier.height(6.dp))

            Text(
                match.blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = revealAlpha.value }
            )

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "I look like ${match.displayName} according to Dexverse! 🎉\n" +
                                    "https://play.google.com/store/apps/details?id=${context.packageName}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, null))
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
                Button(
                    onClick = onTryAgain,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FaceMatchGold, contentColor = Color.Black)
                ) {
                    Text("Try Again", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
