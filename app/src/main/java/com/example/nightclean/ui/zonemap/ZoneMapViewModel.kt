package com.example.nightclean.ui.zonemap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightclean.domain.repository.AuthRepository
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
    private val auth: AuthRepository,
) : ViewModel() {

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
                // 1위 구역을 미리 골라 카드가 바로 보이게 함
                onSuccess = { zones -> ZoneMapUiState.Success(zones, zones.maxByOrNull { it.score }?.id) },
                onFailure = { ZoneMapUiState.Error },
            )
        }
    }

    fun selectZone(zoneId: String) {
        _uiState.update { state ->
            if (state is ZoneMapUiState.Success) state.copy(selectedZoneId = zoneId) else state
        }
    }
}
