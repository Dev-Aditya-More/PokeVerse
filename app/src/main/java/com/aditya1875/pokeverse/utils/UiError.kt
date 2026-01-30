package com.aditya1875.pokeverse.utils

sealed class UiError {
    data class Network(val message: String?) : UiError()
    data class Unexpected(val message: String?) : UiError()
    data class NotFound(val name: String) : UiError()
}

