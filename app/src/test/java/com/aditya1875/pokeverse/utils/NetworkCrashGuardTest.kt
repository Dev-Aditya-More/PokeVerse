package com.aditya1875.pokeverse.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkCrashGuardTest {

    @Test
    fun `background network failures are swallowed`() {
        assertTrue(NetworkCrashGuard.isSwallowable(false, SocketTimeoutException("timeout")))
        assertTrue(NetworkCrashGuard.isSwallowable(false, UnknownHostException("pokeapi.co")))
        assertTrue(NetworkCrashGuard.isSwallowable(false, IOException("reset")))
    }

    @Test
    fun `an IOException anywhere in the cause chain counts`() {
        assertTrue(NetworkCrashGuard.isSwallowable(false, RuntimeException("wrapped", IOException("dns"))))
    }

    @Test
    fun `main thread failures are never swallowed`() {
        assertFalse(NetworkCrashGuard.isSwallowable(true, SocketTimeoutException("timeout")))
    }

    @Test
    fun `real bugs still crash`() {
        assertFalse(NetworkCrashGuard.isSwallowable(false, NullPointerException()))
        assertFalse(NetworkCrashGuard.isSwallowable(false, IllegalStateException("bad state")))
        assertFalse(NetworkCrashGuard.isSwallowable(false, OutOfMemoryError()))
    }
}
