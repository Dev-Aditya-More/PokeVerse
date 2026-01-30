package com.aditya1875.pokeverse.presentation.screens.splash.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntSize

@Composable
fun CardShader(
    shaderStr: RuntimeShader,
    speed: Float = 0.01f
) {
    val shader = remember { shaderStr }

    var size by remember { mutableStateOf(IntSize(0, 0)) }
    var gesturePoint by remember { mutableStateOf(Offset(0f, 0f)) }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        do {
            withFrameMillis { time += speed }
        } while (true)
    }

    val shaderBrush = remember(shader) { ShaderBrush(shader) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    gesturePoint = change.position
                }
            }
    ) {
        if (size != this.size.toIntSize()) {
            size = this.size.toIntSize()
        }

        // Update shader uniforms
        shader.setFloatUniform("resolution", size.width.toFloat(), size.height.toFloat())
        shader.setFloatUniform("pointer", gesturePoint.x / size.width, gesturePoint.y / size.height)
        shader.setFloatUniform("time", time)

        drawRect(brush = shaderBrush)
    }
}