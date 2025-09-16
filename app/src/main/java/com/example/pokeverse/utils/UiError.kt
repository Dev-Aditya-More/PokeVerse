package com.example.pokeverse.utils

sealed class UiError {
    object NoInternet : UiError()
    data class Unexpected(val message: String? = null) : UiError()
}
