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
            Text("Welcome, $username ($role)", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            when (role) {
                "ADMIN" -> AdminDashboard()
                "MANAGER" -> ManagerDashboard()
                else -> UserDashboard()
            }
        }
    }
}

@Composable
private fun AdminDashboard() {
    Column {
        Text("Admin Dashboard", style = MaterialTheme.typography.headlineSmall)
        // Add admin-specific UI components
    }
}

@Composable
private fun ManagerDashboard() {
    Column {
        Text("Manager Dashboard", style = MaterialTheme.typography.headlineSmall)
        // Add manager-specific UI components
    }
}

@Composable
private fun UserDashboard() {
    Column {
        Text("User Dashboard", style = MaterialTheme.typography.headlineSmall)
        // Add user-specific UI components
    }
}

@Preview
@Composable
fun PreviewMainScreen() {
    MainScreen("admin")
}
