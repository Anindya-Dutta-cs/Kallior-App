package com.app.kallior

import com.app.kallior.ui.AuthFormValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthFormValidatorTest {
    @Test
    fun `login rejects missing and malformed input`() {
        assertEquals("Email and password are required.", AuthFormValidator.loginError("", ""))
        assertEquals("Enter a valid email address.", AuthFormValidator.loginError("not-an-email", "password"))
    }

    @Test
    fun `registration validates password confirmation and strength`() {
        assertEquals(
            "Use a password with at least 8 characters.",
            AuthFormValidator.registrationError("a@example.com", "short", "short")
        )
        assertEquals(
            "Passwords do not match.",
            AuthFormValidator.registrationError("a@example.com", "password1", "password2")
        )
        assertNull(AuthFormValidator.registrationError("a@example.com", "password1", "password1"))
    }
}
