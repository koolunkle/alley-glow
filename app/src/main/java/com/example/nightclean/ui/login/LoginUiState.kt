package com.example.nightclean.ui.login

import com.example.nightclean.domain.model.UserRole

sealed interface LoginUiState {
    /** 저장된 세션 확인 중 */
    data object Checking : LoginUiState
    data class Input(val isSubmitting: Boolean = false, val error: String? = null) : LoginUiState
    data class SignedIn(val role: UserRole) : LoginUiState
}
