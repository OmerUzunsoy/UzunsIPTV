package com.uzuns.uzunsiptv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiClientTest {

    @Test
    fun `sanitizeBaseUrl adds http by default`() {
        assertEquals("http://example.com/", ApiClient.sanitizeBaseUrl("example.com"))
    }

    @Test
    fun `sanitizeBaseUrl keeps explicit https urls`() {
        assertEquals("https://example.com/", ApiClient.sanitizeBaseUrl("https://example.com"))
    }

    @Test
    fun `sanitizeBaseUrl accepts explicit http urls`() {
        assertEquals("http://example.com/", ApiClient.sanitizeBaseUrl("http://example.com"))
    }

    @Test
    fun `sanitizeBaseUrl rejects blank input`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ApiClient.sanitizeBaseUrl("   ")
        }

        assertEquals("Sunucu adresi boş olamaz.", error.message)
    }
}
