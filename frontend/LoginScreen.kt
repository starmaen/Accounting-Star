package frontend

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    
    val userManager = UserManager()
    
    Column {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (loginError != null) {
            Text(loginError!!, color = Color.Red, modifier = Modifier.padding(8.dp))
        }
        Button(
            onClick = {
                if (userManager.authenticate(username, password)) {
                    onLoginSuccess()
                } else {
                    loginError = "Invalid username or password"
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) { Text("Login") }
    }
}

@Preview
@Composable
fun PreviewLoginScreen() {
    LoginScreen {}
}
