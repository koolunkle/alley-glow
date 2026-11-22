package com.example.nightclean.ui.shift

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightclean.domain.model.MAX_BEFORE_AFTER_PHOTOS
import com.example.nightclean.domain.repository.AdSlotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val repository: AdSlotRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** 근무 시작 시각(epoch ms). 프로세스 재생성 후에도 유지 */
    val startedAt: Long = savedStateHandle.get<Long>(KEY_STARTED_AT)
        ?: System.currentTimeMillis().also { savedStateHandle[KEY_STARTED_AT] = it }

    /** 근무 id. 근무 종료(와 이후 체크인) 기록을 묶고, 재시도 시 중복 저장을 막음 */
    val shiftId: String = savedStateHandle.get<String>(KEY_SHIFT_ID)
        ?: UUID.randomUUID().toString().also { savedStateHandle[KEY_SHIFT_ID] = it }

    /** 청소 전 사진(기기 내 경로). 카메라를 띄운 사이 프로세스가 재생성돼도 남게 저장 상태에 둠 */
    val beforePhotos: StateFlow<List<String>> = savedStateHandle.getStateFlow(KEY_BEFORE_PHOTOS, emptyList())

    fun addBeforePhoto(path: String) {
        val photos = beforePhotos.value
        if (photos.size < MAX_BEFORE_AFTER_PHOTOS) savedStateHandle[KEY_BEFORE_PHOTOS] = ArrayList(photos + path)
    }

    fun removeBeforePhoto(index: Int) {
        savedStateHandle[KEY_BEFORE_PHOTOS] = ArrayList(beforePhotos.value.filterIndexed { i, _ -> i != index })
    }

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
        const val KEY_SHIFT_ID = "shiftId"
        const val KEY_BEFORE_PHOTOS = "beforePhotos"
    }
}
