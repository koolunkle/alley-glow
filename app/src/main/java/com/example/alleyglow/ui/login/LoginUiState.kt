package com.example.alleyglow.ui.login

import com.example.alleyglow.domain.model.UserRole

sealed interface LoginUiState {
    /** 저장된 세션 확인 중 */
    data object Checking : LoginUiState
    /** error 문구는 화면이 리소스로 보임 */
    data class Input(val isSubmitting: Boolean = false, val error: LoginError? = null) : LoginUiState
    data class SignedIn(val role: UserRole) : LoginUiState
}

enum class LoginError { EMPTY, UNREGISTERED, FAILED }
