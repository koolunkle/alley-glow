package io.github.koolunkle.alleyglow.ui.zonemap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.koolunkle.alleyglow.domain.model.nightSlotOf
import io.github.koolunkle.alleyglow.domain.repository.AuthRepository
import io.github.koolunkle.alleyglow.domain.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ZoneMapViewModel @Inject constructor(
    private val repository: ZoneRepository,
    private val auth: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** 처음 보여 줄 시간대: 지금 시각의 칸(19~23시 밖이면 평균). 시각은 테스트에서 저장 상태로 넣을 수 있음 */
    private val initialSlot: Int? = nightSlotOf(
        savedStateHandle.get<Int>(KEY_HOUR) ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    )

    private val _uiState = MutableStateFlow<ZoneMapUiState>(ZoneMapUiState.Loading)
    val uiState: StateFlow<ZoneMapUiState> = _uiState.asStateFlow()

    /** 상단바 이름. 못 받으면 null(역할 이름으로 대신) */
    private val _workerName = MutableStateFlow<String?>(null)
    val workerName: StateFlow<String?> = _workerName.asStateFlow()

    init {
        load()
        viewModelScope.launch { _workerName.value = auth.displayName().getOrNull() }
    }

    fun load() {
        _uiState.value = ZoneMapUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.getZones().fold(
                // 그 시간대 1위 구역을 미리 골라 카드가 바로 보이게 함
                onSuccess = { zones ->
                    ZoneMapUiState.Success(zones, zones.maxByOrNull { it.scoreAt(initialSlot) }?.id, initialSlot)
                },
                onFailure = { ZoneMapUiState.Error },
            )
        }
    }

    /** 고른 구역은 그대로 두고 순위·색만 그 시간대로 */
    fun selectSlot(slot: Int?) {
        _uiState.update { state ->
            if (state is ZoneMapUiState.Success) state.copy(slot = slot) else state
        }
    }

    fun selectZone(zoneId: String) {
        _uiState.update { state ->
            if (state is ZoneMapUiState.Success) state.copy(selectedZoneId = zoneId) else state
        }
    }

    private companion object {
        const val KEY_HOUR = "hourOfDay"
    }
}
