package com.starmaen.accountingstar.util

import java.security.MessageDigest
import java.security.SecureRandom

object Security {
    private val random = SecureRandom()

    fun newSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(password: String, salt: String): String {
        var value = (salt + password).toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        repeat(12_000) {
            digest.reset()
            value = digest.digest(value + salt.toByteArray(Charsets.UTF_8))
        }
        return value.toHex()
    }

    fun verify(password: String, salt: String, expected: String): Boolean =
        hash(password, salt).equals(expected, ignoreCase = true)

    private fun ByteArray.toHex(): String {
        val chars = CharArray(size * 2)
        val digits = "0123456789abcdef"
        for (i in indices) {
            val v = this[i].toInt() and 0xFF
            chars[i * 2] = digits[v ushr 4]
            chars[i * 2 + 1] = digits[v and 0x0F]
        }
        return String(chars)
    }
}
