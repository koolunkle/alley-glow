package io.github.koolunkle.alleyglow.ui.shift

import io.github.koolunkle.alleyglow.domain.model.AdSlot

sealed interface ShiftUiState {
    data object Loading : ShiftUiState
    data class Success(val slots: List<AdSlot>) : ShiftUiState
    data object Error : ShiftUiState
}
