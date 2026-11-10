package com.aditya1875.pokeverse.utils

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class LoadErrorTest {

    private fun http(code: Int) =
        HttpException(Response.error<Any>(code, "".toResponseBody("text/plain".toMediaType())))

    @Test
    fun `no connection reads as offline`() {
        assertEquals(LoadErrorKind.OFFLINE, UnknownHostException("pokeapi.co").toLoadErrorKind())
        assertEquals(LoadErrorKind.OFFLINE, ConnectException().toLoadErrorKind())
        assertEquals(LoadErrorKind.OFFLINE, IOException("boom").toLoadErrorKind())
        assertEquals(LoadErrorKind.OFFLINE, SSLHandshakeException("captive portal").toLoadErrorKind())
    }

    @Test
    fun `a timeout is a timeout, not offline, even though it is also an IOException`() {
        assertEquals(LoadErrorKind.TIMEOUT, SocketTimeoutException().toLoadErrorKind())
    }

    @Test
    fun `server errors are the server's fault`() {
        assertEquals(LoadErrorKind.SERVER, http(500).toLoadErrorKind())
        assertEquals(LoadErrorKind.SERVER, http(503).toLoadErrorKind())
    }

    @Test
    fun `a missing page is not a server outage`() {
        assertEquals(LoadErrorKind.UNKNOWN, http(404).toLoadErrorKind())
    }

    @Test
    fun `anything else is unknown, and a wrapped cause is looked through`() {
        assertEquals(LoadErrorKind.UNKNOWN, IllegalStateException("bug").toLoadErrorKind())
        assertEquals(LoadErrorKind.OFFLINE, RuntimeException("wrapped", UnknownHostException()).toLoadErrorKind())
    }

    @Test
    fun `screen errors keep the reason`() {
        assertEquals(LoadErrorKind.TIMEOUT, SocketTimeoutException().toUiError().toLoadErrorKind())
        assertEquals(LoadErrorKind.SERVER, http(502).toUiError().toLoadErrorKind())
        assertTrue(IllegalStateException().toUiError() is UiError.Unexpected)
    }
}
