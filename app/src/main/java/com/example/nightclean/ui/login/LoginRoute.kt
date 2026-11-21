package com.example.nightclean.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nightclean.domain.model.UserRole
import com.example.nightclean.ui.theme.NightCleanTheme

@Composable
fun LoginRoute(
    onSignedIn: (UserRole) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        (uiState as? LoginUiState.SignedIn)?.let { onSignedIn(it.role) }
    }
    var email by rememberSaveable { mutableStateOf("") }
    // 비밀번호는 저장 상태(Bundle)에 남기지 않음
    var password by remember { mutableStateOf("") }
    LoginScreen(
        uiState = uiState,
        email = email,
        password = password,
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onSubmit = { viewModel.signIn(email, password) },
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold { padding ->
        if (uiState !is LoginUiState.Input) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("야간 클린 워커", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("운영자가 발급한 계정으로 로그인하세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("이메일") },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("비밀번호") },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = onSubmit,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("로그인", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    NightCleanTheme {
        LoginScreen(
            uiState = LoginUiState.Input(error = "로그인하지 못했어요. 이메일, 비밀번호, 네트워크를 확인해 주세요."),
            email = "worker@example.com",
            password = "secret",
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {},
        )
    }
}
