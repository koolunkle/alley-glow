package com.example.nightclean.ui.shopreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightclean.domain.repository.ShopReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShopReportViewModel @Inject constructor(
    private val repository: ShopReportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShopReportUiState>(ShopReportUiState.Loading)
    val uiState: StateFlow<ShopReportUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = ShopReportUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.getReport().fold(
                onSuccess = { report -> report?.let { ShopReportUiState.Success(it) } ?: ShopReportUiState.NoShop },
                onFailure = { ShopReportUiState.Error("리포트를 불러오지 못했어요. 네트워크를 확인하고 다시 시도해 주세요.") },
            )
        }
    }

    // 화면을 다시 그릴 때마다 내려받지 않게 기억(리포트 사진은 최대 10장)
    private val photoCache = mutableMapOf<String, ByteArray>()

    /** 청소 전후 사진. 못 받으면 null(썸네일 자리만 비워 둠) */
    suspend fun loadPhoto(path: String): ByteArray? =
        photoCache[path] ?: repository.getPhoto(path).getOrNull()?.also { photoCache[path] = it }

    /** 저장에 성공했을 때만 숫자를 올림(중복 탭은 무시) */
    fun addGuest() {
        val state = _uiState.value as? ShopReportUiState.Success ?: return
        if (state.isAddingGuest) return
        _uiState.value = state.copy(isAddingGuest = true, message = null)
        viewModelScope.launch {
            _uiState.value = repository.addGuest(state.report.shopId).fold(
                onSuccess = { state.copy(report = state.report.copy(guests = state.report.guests + 1)) },
                onFailure = { state.copy(message = "손님을 기록하지 못했어요. 다시 눌러 주세요.") },
            )
        }
    }
}
