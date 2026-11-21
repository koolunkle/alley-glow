package com.example.nightclean.ui.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nightclean.domain.model.ShiftRecord
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

    /** 이 화면에 들어온 시각을 근무 종료 시각으로 고정 */
    val endedAt: Long = savedStateHandle.get<Long>(KEY_ENDED_AT)
        ?: System.currentTimeMillis().also { savedStateHandle[KEY_ENDED_AT] = it }

    private val _uiState = MutableStateFlow<CollectionUiState>(CollectionUiState.Editing())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    fun changeBags(slot: Int, delta: Int) = edit { form ->
        form.copy(bags = form.bags.mapIndexed { i, n -> if (i == slot) (n + delta).coerceIn(0, MAX_BAGS) else n })
    }

    fun changeGeneralKg(value: String) = edit { it.copy(generalKg = value) }
    fun changeRecycleKg(value: String) = edit { it.copy(recycleKg = value) }
    fun changeButtsG(value: String) = edit { it.copy(buttsG = value) }
    fun changeNote(value: String) = edit { it.copy(note = value.take(MAX_NOTE)) }

    fun submit() {
        val state = _uiState.value as? CollectionUiState.Editing ?: return
        if (state.isSubmitting) return
        val form = state.form
        val generalKg = form.generalKg.toWeight(MAX_KG)
        val recycleKg = form.recycleKg.toWeight(MAX_KG)
        val buttsG = form.buttsG.toWeight(MAX_G)
        if (generalKg == null || recycleKg == null || buttsG == null) {
            _uiState.value = state.copy(error = "무게를 숫자로 입력해 주세요. 없으면 0을 적어 주세요.")
            return
        }
        val record = ShiftRecord(
            shiftId = shiftId,
            zoneId = zoneId,
            startedAt = startedAt,
            endedAt = endedAt,
            bagsByHour = form.bags,
            generalKg = generalKg,
            recycleKg = recycleKg,
            buttsG = buttsG,
            note = form.note.trim().ifEmpty { null },
        )
        _uiState.value = state.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            _uiState.value = repository.finishShift(record).fold(
                onSuccess = { CollectionUiState.Submitted },
                onFailure = { state.copy(error = "저장하지 못했어요. 네트워크를 확인하고 다시 시도해 주세요.") },
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

/** 0 이상 max 이하 숫자. 소수점 쉼표도 허용 */
internal fun String.toWeight(max: Double): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..max }
