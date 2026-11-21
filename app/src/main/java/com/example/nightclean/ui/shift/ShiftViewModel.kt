package com.example.nightclean.ui.shift

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightclean.domain.repository.AdSlotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val repository: AdSlotRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** 근무 시작 시각(epoch ms). 프로세스 재생성 후에도 유지 */
    val startedAt: Long = savedStateHandle.get<Long>(KEY_STARTED_AT)
        ?: System.currentTimeMillis().also { savedStateHandle[KEY_STARTED_AT] = it }

    private val _uiState = MutableStateFlow<ShiftUiState>(ShiftUiState.Loading)
    val uiState: StateFlow<ShiftUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = ShiftUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.getAdSlots().fold(
                onSuccess = { ShiftUiState.Success(it) },
                onFailure = { ShiftUiState.Error("광고 일정을 불러오지 못했어요. 네트워크를 확인하고 다시 시도해 주세요.") },
            )
        }
    }

    private companion object {
        const val KEY_STARTED_AT = "startedAt"
    }
}
