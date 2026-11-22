package com.example.nightclean.ui.checkin

import com.example.nightclean.domain.model.CheckinResult

sealed interface CheckinUiState {
    data object Idle : CheckinUiState
    /** 위치 확인과 서버 판정 중 */
    data object Working : CheckinUiState
    /** count: 성공했을 때 이번 근무 체크인 가게 수(못 받으면 null) */
    data class Done(val result: CheckinResult, val checkedAt: Long, val count: Int? = null) : CheckinUiState
    /** 문구는 화면이 리소스로 보임 */
    data class Error(val reason: CheckinError) : CheckinUiState
}

enum class CheckinError { LOCATION, NETWORK, PERMISSION, SCANNER }
