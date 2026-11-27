package com.example.alleyglow.ui.shift

import com.example.alleyglow.domain.model.AdSlot

sealed interface ShiftUiState {
    data object Loading : ShiftUiState
    data class Success(val slots: List<AdSlot>) : ShiftUiState
    data object Error : ShiftUiState
}
