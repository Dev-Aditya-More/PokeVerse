package com.aditya1875.pokeverse.utils

import android.graphics.RuntimeShader

interface IShaderScreen {
    val name: String
    val speed: Float
    val shader: RuntimeShader
}