package com.example.ptmanageremployee.data

import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseUrlPolicyTest {
    @Test
    fun `release requires https`() {
        requireSecureBaseUrl("https://api.example.com/", isDebug = false)
        assertThrows(IllegalArgumentException::class.java) {
            requireSecureBaseUrl("http://api.example.com/", isDebug = false)
        }
    }

    @Test
    fun `debug permits emulator http`() {
        requireSecureBaseUrl("http://10.0.2.2:8080/", isDebug = true)
    }

    @Test
    fun `only explicit invalid refresh response expires session`() {
        assertTrue(isDefinitiveRefreshFailure(401, "invalid token"))
        assertFalse(isDefinitiveRefreshFailure(401, ""))
        assertFalse(isDefinitiveRefreshFailure(500, "server error"))
    }
}
