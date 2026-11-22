package com.example.nightclean.ui.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightclean.domain.model.BAG_SLOT_HOURS
import com.example.nightclean.domain.model.MAX_BEFORE_AFTER_PHOTOS
import com.example.nightclean.domain.model.MAX_PHOTOS
import com.example.nightclean.domain.model.ShiftRecord
import com.example.nightclean.domain.model.Weight
import com.example.nightclean.domain.repository.ShiftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: ShiftRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // AppRoute.Collection 인자
    private val shiftId: String = checkNotNull(savedStateHandle["shiftId"])
    private val zoneId: String = checkNotNull(savedStateHandle["zoneId"])
    val startedAt: Long = checkNotNull(savedStateHandle["startedAt"])

    /** 근무 화면에서 찍은 청소 전 사진(기기 내 경로) */
    val beforePhotos: List<String> = savedStateHandle.stringList("beforePhotos")

    /** 이 화면에 들어온 시각을 근무 종료 시각으로 고정 */
    val endedAt: Long = savedStateHandle.get<Long>(KEY_ENDED_AT)
        ?: System.currentTimeMillis().also { savedStateHandle[KEY_ENDED_AT] = it }

    // 근무 중에 센 봉투 수가 있으면 초기값으로(여기서 고칠 수 있음)
    private val _uiState = MutableStateFlow<CollectionUiState>(
        CollectionUiState.Editing(
            form = savedStateHandle.intList("bagsByHour").takeIf { it.size == BAG_SLOT_HOURS.size }
                ?.let { CollectionForm(bags = it) }
                ?: CollectionForm(),
        ),
    )
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    fun changeBags(slot: Int, delta: Int) = edit { form ->
        form.copy(bags = form.bags.mapIndexed { i, n -> if (i == slot) (n + delta).coerceIn(0, MAX_BAGS) else n })
    }

    fun changeGeneralKg(value: String) = edit { it.copy(generalKg = value) }
    fun changeRecycleKg(value: String) = edit { it.copy(recycleKg = value) }
    fun changeButtsG(value: String) = edit { it.copy(buttsG = value) }
    fun changeNote(value: String) = edit { it.copy(note = value.take(MAX_NOTE)) }

    fun setWeighed(weighed: Boolean) = edit { it.copy(weighed = weighed) }

    fun addPhoto(path: String) = edit { form ->
        if (form.photos.size < MAX_PHOTOS) form.copy(photos = form.photos + path) else form
    }

    fun removePhoto(index: Int) = edit { it.copy(photos = it.photos.filterIndexed { i, _ -> i != index }) }

    fun addAfterPhoto(path: String) = edit { form ->
        if (form.afterPhotos.size < MAX_BEFORE_AFTER_PHOTOS) form.copy(afterPhotos = form.afterPhotos + path) else form
    }

    fun removeAfterPhoto(index: Int) = edit { it.copy(afterPhotos = it.afterPhotos.filterIndexed { i, _ -> i != index }) }

    fun submit() {
        val state = _uiState.value as? CollectionUiState.Editing ?: return
        if (state.isSubmitting) return
        val form = state.form
        val weight = if (form.weighed) {
            val generalKg = form.generalKg.toWeight(MAX_KG)
            val recycleKg = form.recycleKg.toWeight(MAX_KG)
            val buttsG = form.buttsG.toWeight(MAX_G)
            if (generalKg == null || recycleKg == null || buttsG == null) {
                _uiState.value = state.copy(error = "무게를 숫자로 입력해 주세요. 없으면 0을 적어 주세요.")
                return
            }
            Weight(generalKg, recycleKg, buttsG)
        } else {
            null
        }
        if (form.photos.isEmpty()) {
            val what = if (form.weighed) "저울" else "채운 봉투"
            _uiState.value = state.copy(error = "$what 사진을 1장 이상 찍어 주세요.")
            return
        }
        val record = ShiftRecord(
            shiftId = shiftId,
            zoneId = zoneId,
            startedAt = startedAt,
            endedAt = endedAt,
            bagsByHour = form.bags,
            weight = weight,
            note = form.note.trim().ifEmpty { null },
            photos = form.photos,
            beforePhotos = beforePhotos,
            afterPhotos = form.afterPhotos,
        )
        _uiState.value = state.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            _uiState.value = repository.finishShift(record).fold(
                onSuccess = { CollectionUiState.Submitted },
                onFailure = { state.copy(error = "저장하지 못했어요. 네트워크를 확인하고 다시 시도해 주세요. 사진은 그대로 남아 있어요.") },
            )
        }
    }

    private fun edit(change: (CollectionForm) -> CollectionForm) {
        val state = _uiState.value as? CollectionUiState.Editing ?: return
        if (!state.isSubmitting) _uiState.value = state.copy(form = change(state.form), error = null)
    }

    private companion object {
        const val KEY_ENDED_AT = "endedAt"
        // DB check 제약과 같은 상한
        const val MAX_BAGS = 50
        const val MAX_KG = 500.0
        const val MAX_G = 100_000.0
        const val MAX_NOTE = 1000
    }
}

/** 내비게이션은 List<String> 인자를 배열로 넣기도 해 둘 다 받음 */
private fun SavedStateHandle.stringList(key: String): List<String> = when (val value = get<Any>(key)) {
    is Array<*> -> value.filterIsInstance<String>()
    is List<*> -> value.filterIsInstance<String>()
    else -> emptyList()
}

private fun SavedStateHandle.intList(key: String): List<Int> = when (val value = get<Any>(key)) {
    is IntArray -> value.toList()
    is Array<*> -> value.filterIsInstance<Int>()
    is List<*> -> value.filterIsInstance<Int>()
    else -> emptyList()
}

/** 0 이상 max 이하 숫자. 소수점 쉼표도 허용 */
internal fun String.toWeight(max: Double): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..max }
