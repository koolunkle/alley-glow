package com.example.nightclean.ui.checkin

import com.example.nightclean.domain.model.CheckinResult

sealed interface CheckinUiState {
    data object Idle : CheckinUiState
    /** 위치 확인과 서버 판정 중 */
    data object Working : CheckinUiState
    data class Done(val result: CheckinResult, val checkedAt: Long) : CheckinUiState
    data class Error(val message: String) : CheckinUiState
}
