package com.example.nightclean.ui.shift

import com.example.nightclean.domain.model.AdSlot

sealed interface ShiftUiState {
    data object Loading : ShiftUiState
    data class Success(val slots: List<AdSlot>) : ShiftUiState
    data object Error : ShiftUiState
}
