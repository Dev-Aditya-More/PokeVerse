package com.aditya1875.pokeverse.utils

sealed class UiError {
    data class Network(val message: String?, val kind: LoadErrorKind = LoadErrorKind.OFFLINE) : UiError()
    data class Server(val code: Int) : UiError()
    data class Unexpected(val message: String?) : UiError()
    data class NotFound(val name: String) : UiError()
}
