package io.github.koolunkle.alleyglow.ui.shift

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.koolunkle.alleyglow.domain.model.CheckedInShop
import io.github.koolunkle.alleyglow.domain.model.MAX_BEFORE_AFTER_PHOTOS
import io.github.koolunkle.alleyglow.domain.model.NIGHT_SLOT_HOURS
import io.github.koolunkle.alleyglow.domain.model.bagSlotOf
import io.github.koolunkle.alleyglow.domain.repository.AdSlotRepository
import io.github.koolunkle.alleyglow.domain.repository.CheckinRepository
import io.github.koolunkle.alleyglow.domain.repository.TrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.koolunkle.alleyglow.ui.common.plusUpTo
import io.github.koolunkle.alleyglow.ui.common.withoutIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val repository: AdSlotRepository,
    private val checkins: CheckinRepository,
    private val tracking: TrackingRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val isTracking: StateFlow<Boolean> = tracking.isTracking

    /** 화면에서 동의와 정확한 위치 권한을 받은 뒤 부름 */
    fun startTracking(zoneId: String) = tracking.start(shiftId, zoneId, startedAt)

    // 근무를 마치거나(근무 종료 저장 후 지도로) 근무 화면을 떠나면 이 ViewModel이 사라지므로 여기서 멈춤
    override fun onCleared() = tracking.stop()

    /** 근무 시작 시각(epoch ms). 프로세스 재생성 후에도 유지 */
    val startedAt: Long = savedStateHandle.get<Long>(KEY_STARTED_AT)
        ?: System.currentTimeMillis().also { savedStateHandle[KEY_STARTED_AT] = it }

    /** 근무 id. 근무 종료(와 이후 체크인) 기록을 묶고, 재시도 시 중복 저장을 막음 */
    val shiftId: String = savedStateHandle.get<String>(KEY_SHIFT_ID)
        ?: UUID.randomUUID().toString().also { savedStateHandle[KEY_SHIFT_ID] = it }

    /** 청소 전 사진(기기 내 경로). 카메라를 띄운 사이 프로세스가 재생성돼도 남게 저장 상태에 둠 */
    val beforePhotos: StateFlow<List<String>> = savedStateHandle.getStateFlow(KEY_BEFORE_PHOTOS, emptyList())

    fun addBeforePhoto(path: String) {
        savedStateHandle[KEY_BEFORE_PHOTOS] = ArrayList(beforePhotos.value.plusUpTo(path, MAX_BEFORE_AFTER_PHOTOS))
    }

    fun removeBeforePhoto(index: Int) {
        savedStateHandle[KEY_BEFORE_PHOTOS] = ArrayList(beforePhotos.value.withoutIndex(index))
    }

    private val _checkedIn = MutableStateFlow<List<CheckedInShop>>(emptyList())
    val checkedIn: StateFlow<List<CheckedInShop>> = _checkedIn.asStateFlow()

    /** 화면에 돌아올 때마다(체크인 후 등) 서버에서 다시 셈. 실패하면 이전 값 유지 */
    fun refreshCheckins() {
        viewModelScope.launch { checkins.getCheckins(shiftId).onSuccess { _checkedIn.value = it } }
    }

    /** 채운 봉투를 누른 순서대로의 시간대 칸(0~3). 되돌리기(-1)와 시간대별 집계에 씀 */
    val bagLog: StateFlow<List<Int>> = savedStateHandle.getStateFlow(KEY_BAG_LOG, emptyList())

    /** 지금 시각의 시간대 칸에 1장 */
    fun addBag(hourOfDay: Int) {
        savedStateHandle[KEY_BAG_LOG] = ArrayList(bagLog.value + bagSlotOf(hourOfDay))
    }

    /** 마지막으로 누른 1장을 되돌림 */
    fun undoBag() {
        savedStateHandle[KEY_BAG_LOG] = ArrayList(bagLog.value.dropLast(1))
    }

    /** 근무 종료 화면에 넘길 시간대별 봉투 수 */
    fun bagsByHour(): List<Int> = NIGHT_SLOT_HOURS.indices.map { slot -> bagLog.value.count { it == slot } }

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
                onFailure = { ShiftUiState.Error },
            )
        }
    }

    private companion object {
        const val KEY_STARTED_AT = "startedAt"
        const val KEY_SHIFT_ID = "shiftId"
        const val KEY_BEFORE_PHOTOS = "beforePhotos"
        const val KEY_BAG_LOG = "bagLog"
    }
}
