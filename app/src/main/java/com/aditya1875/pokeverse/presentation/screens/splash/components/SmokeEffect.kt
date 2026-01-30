package com.aditya1875.pokeverse.presentation.screens.splash.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import com.aditya1875.pokeverse.utils.IShaderScreen

class SmokeShader @RequiresApi(Build.VERSION_CODES.TIRAMISU) constructor(
    override val name: String = "Smoke",
    override val speed: Float = 0.01f,
    override val shader: RuntimeShader =
        RuntimeShader(
        """
            uniform shader composable;
            uniform float2 resolution;
            uniform float2 pointer;
            uniform float time;

            float3 saturateColor(float3 color, float factor) {
                float luminance = dot(color, float3(0.299, 0.587, 0.114));
                return mix(float3(luminance), color, factor);
            }
            
            float rand(float2 n) {
                return fract(sin(dot(n, float2(12.9898, 4.1414))) * 43758.5453);
            }

            float noise(float2 p) {
                float2 i = floor(p);
                float2 f = fract(p);
                f = f * f * (3.0 - 2.0 * f);

                float a = rand(i);
                float b = rand(i + float2(1.0, 0.0));
                float c = rand(i + float2(0.0, 1.0));
                float d = rand(i + float2(1.0, 1.0));

                return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
            }

            float fbm(float2 p) {
                float value = 0.0;
                float amplitude = 0.5;
                float frequency = 2.0;

                for(int i = 0; i < 6; i++) {
                    value += amplitude * noise(p);
                    p *= 2.0;
                    amplitude *= 0.5;
                }

                return value;
            }

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / resolution.xy;
                float2 aspect = float2(resolution.x/resolution.y, 1.0);
                uv = uv * aspect;

                float2 pointerInfluence = pointer * aspect;
                float dist = length(uv - pointerInfluence);
                float pointerFactor = smoothstep(0.5, 0.0, dist);

                float2 movement = float2(time * 0.01, time * 0.05);
                float turbulence = fbm(uv * 3.0 + movement);
                turbulence += fbm((uv + float2(turbulence)) * 2.0 - movement);

                float smokeMask = fbm(uv * 1.5 + turbulence + movement);
                smokeMask = smoothstep(0.2, 0.8, smokeMask + pointerFactor * 0.3);

                float3 smokeColor = float3(1.0, 1.0, 1.0);
                // Replace bluish background with the requested red Color(0xFF802525)
                // 0x80 / 0x25 / 0x25 -> normalized: 128/255 ≈ 0.502, 37/255 ≈ 0.145
                float3 backgroundColor = saturateColor(float3(0.84, 0.16, 0.16), 1.4);
                float3 finalColor = mix(backgroundColor, smokeColor, smokeMask);
                finalColor += pointerFactor * 0.2;

                return half4(finalColor, 2.0);
            }
        """
    )
) : IShaderScreen
