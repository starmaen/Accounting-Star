package tests

import frontend.LoginScreen
import frontend.UserManager
import org.junit.Test
import kotlin.test.assertEquals

class LoginScreenTest {
    @Test
    fun `login with valid credentials succeeds`() {
        val userManager = UserManager()
        userManager.addUser("admin", "password123", "ADMIN")
        var loginSuccess = false
        LoginScreen(onLoginSuccess = { loginSuccess = true }).apply {
            // Simulate user input
            // This would require more advanced testing setup
        }
        assertEquals(true, loginSuccess)
    }

    @Test
    fun `login with invalid credentials fails`() {
        val userManager = UserManager()
        userManager.addUser("admin", "password123", "ADMIN")
        var loginError: String? = null
        LoginScreen(onLoginSuccess = { }).apply {
            // Simulate user input
            // This would require more advanced testing setup
        }
        assertEquals("Invalid username or password", loginError)
    }
}
