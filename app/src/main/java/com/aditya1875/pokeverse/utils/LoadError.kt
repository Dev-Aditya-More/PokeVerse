package com.aditya1875.pokeverse.utils

import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Why a load failed, in the terms a player cares about. Each kind gets its own message and its own
 * recovery (see LoadErrorState): being offline waits for the connection and retries by itself,
 * while a slow connection or a server hiccup just offers a retry.
 */
enum class LoadErrorKind { OFFLINE, TIMEOUT, SERVER, UNKNOWN }

fun Throwable.toLoadErrorKind(): LoadErrorKind = when (this) {
    // Timeouts first: SocketTimeoutException is also an IOException.
    is SocketTimeoutException, is TimeoutCancellationException -> LoadErrorKind.TIMEOUT
    is UnknownHostException, is ConnectException, is NoRouteToHostException -> LoadErrorKind.OFFLINE
    // A TLS failure on mobile is nearly always a captive portal or a dropping connection.
    is SSLException -> LoadErrorKind.OFFLINE
    is HttpException -> if (code() >= 500) LoadErrorKind.SERVER else LoadErrorKind.UNKNOWN
    is IOException -> LoadErrorKind.OFFLINE
    else -> cause?.takeIf { it !== this }?.toLoadErrorKind() ?: LoadErrorKind.UNKNOWN
}

/** The screen-state error for a failed load; keeps the kind so the right message and recovery show. */
fun Throwable.toUiError(): UiError = when (val kind = toLoadErrorKind()) {
    LoadErrorKind.OFFLINE, LoadErrorKind.TIMEOUT -> UiError.Network(message, kind)
    LoadErrorKind.SERVER -> UiError.Server((this as? HttpException)?.code() ?: 500)
    LoadErrorKind.UNKNOWN -> UiError.Unexpected(message)
}

fun UiError.toLoadErrorKind(): LoadErrorKind = when (this) {
    is UiError.Network -> kind
    is UiError.Server -> LoadErrorKind.SERVER
    is UiError.Unexpected, is UiError.NotFound -> LoadErrorKind.UNKNOWN
}
