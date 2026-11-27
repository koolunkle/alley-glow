package io.github.koolunkle.alleyglow.ui.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.koolunkle.alleyglow.domain.model.BAG_SLOT_HOURS
import io.github.koolunkle.alleyglow.domain.model.MAX_BEFORE_AFTER_PHOTOS
import io.github.koolunkle.alleyglow.domain.model.MAX_PHOTOS
import io.github.koolunkle.alleyglow.domain.model.ShiftRecord
import io.github.koolunkle.alleyglow.domain.model.Weight
import io.github.koolunkle.alleyglow.domain.repository.ShiftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.koolunkle.alleyglow.ui.common.plusUpTo
import io.github.koolunkle.alleyglow.ui.common.withoutIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: ShiftRepository,
    private val savedStateHandle: SavedStateHandle,
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

    // 카메라를 띄운 사이 프로세스가 재생성돼도 입력이 남도록 저장 상태에서 먼저 복원.
    // 처음 들어오면 근무 중에 센 봉투 수를 초기값으로(여기서 고칠 수 있음)
    private val _uiState = MutableStateFlow<CollectionUiState>(
        CollectionUiState.Editing(
            form = savedStateHandle.restoreForm()
                ?: savedStateHandle.intList("bagsByHour").takeIf { it.size == BAG_SLOT_HOURS.size }
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
    fun changeNote(value: String) = edit { it.copy(note = value.take(MAX_NOTE)) }

    fun addPhoto(path: String) = edit { it.copy(photos = it.photos.plusUpTo(path, MAX_PHOTOS)) }

    fun removePhoto(index: Int) = edit { it.copy(photos = it.photos.withoutIndex(index)) }

    fun addAfterPhoto(path: String) = edit { it.copy(afterPhotos = it.afterPhotos.plusUpTo(path, MAX_BEFORE_AFTER_PHOTOS)) }

    fun removeAfterPhoto(index: Int) = edit { it.copy(afterPhotos = it.afterPhotos.withoutIndex(index)) }

    fun submit() {
        val state = _uiState.value as? CollectionUiState.Editing ?: return
        if (state.isSubmitting) return
        val form = state.form
        val weight = if (form.weighed) {
            val generalKg = form.generalKg.ifBlank { "0" }.toWeight(MAX_KG)
            val recycleKg = form.recycleKg.ifBlank { "0" }.toWeight(MAX_KG)
            if (generalKg == null || recycleKg == null) {
                _uiState.value = state.copy(error = CollectionError.WEIGHT)
                return
            }
            // shortcut: 꽁초는 따로 재지 않고(일반에 포함) 0으로 저장, 꽁초 수거 지표가 필요해지면 칸을 되살림
            Weight(generalKg, recycleKg, 0.0)
        } else {
            null
        }
        if (form.photos.isEmpty()) {
            _uiState.value = state.copy(error = CollectionError.PHOTO)
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
                onFailure = { state.copy(error = CollectionError.SAVE) },
            )
        }
    }

    private fun edit(change: (CollectionForm) -> CollectionForm) {
        val state = _uiState.value as? CollectionUiState.Editing ?: return
        if (state.isSubmitting) return
        val form = change(state.form)
        savedStateHandle.saveForm(form)
        _uiState.value = state.copy(form = form, error = null)
    }

    private companion object {
        const val KEY_ENDED_AT = "endedAt"
        // DB check 제약과 같은 상한
        const val MAX_BAGS = 50
        const val MAX_KG = 500.0
        const val MAX_NOTE = 1000
    }
}

/** 내비게이션은 List<String> 인자를 배열로 넣기도 해 둘 다 받음 */
private fun SavedStateHandle.stringList(key: String): List<String> = when (val value = get<Any>(key)) {
    is Array<*> -> value.filterIsInstance<String>()
    is List<*> -> value.filterIsInstance<String>()
    else -> emptyList()
}

// 폼 저장 키는 내비게이션 인자(bagsByHour 등)와 겹치지 않게 접두
private fun SavedStateHandle.saveForm(form: CollectionForm) {
    this["form_bags"] = ArrayList(form.bags)
    this["form_general_kg"] = form.generalKg
    this["form_recycle_kg"] = form.recycleKg
    this["form_note"] = form.note
    this["form_photos"] = ArrayList(form.photos)
    this["form_after_photos"] = ArrayList(form.afterPhotos)
}

private fun SavedStateHandle.restoreForm(): CollectionForm? {
    val bags = intList("form_bags").takeIf { it.size == BAG_SLOT_HOURS.size } ?: return null
    return CollectionForm(
        bags = bags,
        generalKg = get<String>("form_general_kg").orEmpty(),
        recycleKg = get<String>("form_recycle_kg").orEmpty(),
        note = get<String>("form_note").orEmpty(),
        photos = stringList("form_photos"),
        afterPhotos = stringList("form_after_photos"),
    )
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
