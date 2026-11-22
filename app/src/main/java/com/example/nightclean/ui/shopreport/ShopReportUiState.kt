package com.example.nightclean.ui.shopreport

import com.example.nightclean.domain.model.ShopReport

sealed interface ShopReportUiState {
    data object Loading : ShopReportUiState
    data class Success(
        val report: ShopReport,
        val isAddingGuest: Boolean = false,
        val message: String? = null,
    ) : ShopReportUiState

    /** 계정에 연결된 가게가 없음 */
    data object NoShop : ShopReportUiState
    data class Error(val message: String) : ShopReportUiState
}
