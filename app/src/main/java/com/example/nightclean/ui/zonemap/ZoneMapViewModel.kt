package com.example.nightclean.ui.zonemap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightclean.domain.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ZoneMapViewModel @Inject constructor(
    private val repository: ZoneRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ZoneMapUiState>(ZoneMapUiState.Loading)
    val uiState: StateFlow<ZoneMapUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = ZoneMapUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.getZones().fold(
                onSuccess = { ZoneMapUiState.Success(zones = it) },
                // 예외 원문 대신 고정 안내 문구 노출
                onFailure = { ZoneMapUiState.Error("구역 정보를 불러오지 못했어요. 네트워크를 확인하고 다시 시도해 주세요.") },
            )
        }
    }

    fun selectZone(zoneId: String) {
        _uiState.update { state ->
            if (state is ZoneMapUiState.Success) state.copy(selectedZoneId = zoneId) else state
        }
    }
}
