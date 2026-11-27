package com.example.alleyglow.ui.zonemap

import com.example.alleyglow.domain.model.Zone

sealed interface ZoneMapUiState {
    data object Loading : ZoneMapUiState
    data class Success(
        val zones: List<Zone>,
        val selectedZoneId: String? = null,
    ) : ZoneMapUiState {
        val selectedZone: Zone? get() = zones.find { it.id == selectedZoneId }
    }
    /** 문구는 화면이 리소스로 보임 */
    data object Error : ZoneMapUiState
}
