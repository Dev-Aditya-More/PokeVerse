package com.aditya1875.pokeverse.utils

sealed class UiError {

    data class Network(val message: String? = null) : UiError()
    data class Unexpected(val message: String? = null) : UiError()
}
