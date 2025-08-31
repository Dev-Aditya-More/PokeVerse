package com.example.pokeverse.screens

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import org.intellij.lang.annotations.Language

@Language("AGSL")
const val ShaderSource = """
    uniform shader composable;
    
    uniform float cutoff;
    uniform float3 rgb;
    
    half4 main(float2 fragCoord) {
        half4 color = composable.eval(fragCoord);
        color.a = step(cutoff, color.a);
        color.a = step(cutoff, color.a);
        color.rgb = mix(color.rgb, rgb, color.a);
        return color;
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MetaContainer(
    modifier: Modifier = Modifier,
    cutoff: Float = .5f,
    color: Color = Color.Black,
    content: @Composable BoxScope.() -> Unit,
) {
    val metaShader = remember {
        RuntimeShader(ShaderSource)
    }
    Box(
        modifier
            .graphicsLayer {
                metaShader.setFloatUniform("cutoff", cutoff)
                metaShader.setFloatUniform("rgb", color.red, color.green, color.blue)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(
                        metaShader, "composable"
                    )
                    .asComposeRenderEffect()
            },
        content = content,
    )
}