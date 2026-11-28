package io.github.koolunkle.alleyglow.ui.zonemap

import io.github.koolunkle.alleyglow.domain.model.Zone

sealed interface ZoneMapUiState {
    data object Loading : ZoneMapUiState
    data class Success(
        val zones: List<Zone>,
        val selectedZoneId: String? = null,
        /** 시간대 칸(0~3). null이면 19~23시 평균 */
        val slot: Int? = null,
    ) : ZoneMapUiState {
        val selectedZone: Zone? get() = zones.find { it.id == selectedZoneId }
    }
    /** 문구는 화면이 리소스로 보임 */
    data object Error : ZoneMapUiState
}
