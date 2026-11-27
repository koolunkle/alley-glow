package io.github.koolunkle.alleyglow.ui.login

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.koolunkle.alleyglow.domain.model.UserRole
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.ui.common.InlineNotice
import io.github.koolunkle.alleyglow.ui.common.LoadFailedNotice
import io.github.koolunkle.alleyglow.ui.theme.AlleyGlowTheme
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import io.github.koolunkle.alleyglow.ui.theme.TouchHeight

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
        onRetryRestore = viewModel::restoreSession,
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
    onRetryRestore: () -> Unit,
) {
    Scaffold { padding ->
        if (uiState is LoginUiState.RestoreFailed) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                LoadFailedNotice(stringResource(R.string.login_restore_error), onRetryRestore)
            }
            return@Scaffold
        }
        if (uiState !is LoginUiState.Input) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.login_email)) },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.login_password)) },
                singleLine = true,
                enabled = !uiState.isSubmitting,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
            uiState.error?.let {
                val message = when (it) {
                    LoginError.EMPTY -> R.string.login_error_empty
                    LoginError.UNREGISTERED -> R.string.login_error_unregistered
                    LoginError.FAILED -> R.string.login_error_failed
                }
                InlineNotice(stringResource(message))
            }
            Button(
                onClick = onSubmit,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(TouchHeight),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.login_submit))
                }
            }
            // 회원가입이 없음을 알리는 보조 문구
            Text(
                stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    AlleyGlowTheme {
        LoginScreen(
            uiState = LoginUiState.Input(error = LoginError.FAILED),
            email = "worker@example.com",
            password = "secret",
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {}, onRetryRestore = {},
        )
    }
}
