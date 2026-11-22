package com.example.nightclean.ui.shopreport

import com.example.nightclean.domain.model.ShopReport

sealed interface ShopReportUiState {
    data object Loading : ShopReportUiState
    data class Success(
        val report: ShopReport,
        val isAddingGuest: Boolean = false,
        /** 손님 +1 저장 실패(문구는 화면이 리소스로) */
        val guestFailed: Boolean = false,
    ) : ShopReportUiState

    /** 계정에 연결된 가게가 없음 */
    data object NoShop : ShopReportUiState
    data object Error : ShopReportUiState
}
