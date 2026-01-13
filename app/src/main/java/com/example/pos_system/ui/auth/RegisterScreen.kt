package com.example.pos_system.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var passConfirm by remember { mutableStateOf("") }

    val isRegistered by viewModel.isRegistered.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(isRegistered) { if (isRegistered) onRegisterSuccess() }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)
        .verticalScroll(rememberScrollState())) {
        Text("Create Account", fontSize = 28.sp)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = passConfirm, onValueChange = { passConfirm = it }, label = { Text("Confirm Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(onClick = { viewModel.register(email, name, pass, passConfirm, phone) }, modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)) {
            Text("Register")
        }
        TextButton(onClick = onBackToLogin) { Text("Back to Login") }
    }
}

