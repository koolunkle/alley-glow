package io.github.koolunkle.alleyglow.ui.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.koolunkle.alleyglow.domain.repository.AuthRepository
import io.github.koolunkle.alleyglow.domain.repository.UnregisteredAccountException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Checking)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // AppRoute.Login(signOut = true)로 들어오면 로그아웃부터
        val signOut = savedStateHandle.get<Boolean>("signOut") == true
        if (signOut) {
            viewModelScope.launch {
                repository.signOut()
                _uiState.value = LoginUiState.Input()
            }
        } else {
            restoreSession()
        }
    }

    fun restoreSession() {
        _uiState.value = LoginUiState.Checking
        viewModelScope.launch {
            _uiState.value = repository.restoreSession().fold(
                onSuccess = { role -> role?.let { LoginUiState.SignedIn(it) } ?: LoginUiState.Input() },
                // 저장된 세션은 남아 있으므로 입력 폼(로그아웃처럼 보임) 대신 다시 시도 안내
                onFailure = { e -> if (e is UnregisteredAccountException) LoginUiState.Input() else LoginUiState.RestoreFailed },
            )
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Input(error = LoginError.EMPTY)
            return
        }
        _uiState.value = LoginUiState.Input(isSubmitting = true)
        viewModelScope.launch {
            _uiState.value = repository.signIn(email.trim(), password).fold(
                onSuccess = { LoginUiState.SignedIn(it) },
                onFailure = { e ->
                    LoginUiState.Input(error = if (e is UnregisteredAccountException) LoginError.UNREGISTERED else LoginError.FAILED)
                },
            )
        }
    }
}
