package com.starmaen.accountingstar

import com.starmaen.accountingstar.util.Security
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityTest {
    @Test
    fun `same password and salt produce same hash`() {
        val salt = Security.newSalt()
        val hash = Security.hash("admin123", salt)
        assertTrue(Security.verify("admin123", salt, hash))
    }

    @Test
    fun `wrong password is rejected`() {
        val salt = Security.newSalt()
        val hash = Security.hash("admin123", salt)
        assertFalse(Security.verify("wrong", salt, hash))
    }

    @Test
    fun `different salts produce different hashes`() {
        val first = Security.hash("secret", Security.newSalt())
        val second = Security.hash("secret", Security.newSalt())
        assertFalse(first == second)
    }
}
