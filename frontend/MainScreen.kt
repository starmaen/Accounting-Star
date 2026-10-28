package frontend

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MainScreen(username: String) {
    val userManager = UserManager()
    val role = userManager.getUserRole(username) ?: "GUEST"

    Scaffold(
        topBar = { TopAppBar(title = { Text("Accounting System") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text("Welcome, $username ($role)")
            // Add more UI components based on user role
        }
    }
}

@Preview
@Composable
fun PreviewMainScreen() {
    MainScreen("admin")
}
