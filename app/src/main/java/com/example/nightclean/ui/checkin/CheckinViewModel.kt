package com.example.nightclean.ui.checkin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightclean.domain.model.shopCodeOf
import com.example.nightclean.domain.repository.CheckinRepository
import com.example.nightclean.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckinViewModel @Inject constructor(
    private val checkins: CheckinRepository,
    private val location: LocationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // AppRoute.Checkin 인자
    private val shiftId: String = checkNotNull(savedStateHandle["shiftId"])
    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    private val startedAt: Long = checkNotNull(savedStateHandle["startedAt"])

    private val _uiState = MutableStateFlow<CheckinUiState>(CheckinUiState.Idle)
    val uiState: StateFlow<CheckinUiState> = _uiState.asStateFlow()

    /** 스캔한 QR 값으로 체크인. 스캔 직후의 위치로 판정 */
    fun checkIn(qr: String) {
        if (_uiState.value == CheckinUiState.Working) return
        _uiState.value = CheckinUiState.Working
        viewModelScope.launch {
            val here = location.currentLocation()
            if (here == null) {
                _uiState.value = CheckinUiState.Error("현재 위치를 확인하지 못했어요. 위치 서비스를 켜고 다시 시도해 주세요.")
                return@launch
            }
            _uiState.value = checkins.checkIn(shiftId, zoneId, startedAt, shopCodeOf(qr), here).fold(
                onSuccess = { CheckinUiState.Done(it, System.currentTimeMillis()) },
                onFailure = { CheckinUiState.Error("체크인하지 못했어요. 네트워크를 확인하고 다시 시도해 주세요.") },
            )
        }
    }

    fun onPermissionDenied() {
        _uiState.value = CheckinUiState.Error("정확한 위치 권한이 있어야 체크인할 수 있어요. 가게 앞인지 확인하는 데만 써요.")
    }

    fun onScannerFailed() {
        _uiState.value = CheckinUiState.Error("QR 스캐너를 열지 못했어요. 잠시 뒤 다시 시도해 주세요.")
    }
}
