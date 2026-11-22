package com.example.nightclean.ui.collection

import com.example.nightclean.domain.model.BAG_SLOT_HOURS

/** 입력 중인 값. 무게는 입력 그대로의 문자열 */
data class CollectionForm(
    val bags: List<Int> = List(BAG_SLOT_HOURS.size) { 0 },
    val generalKg: String = "",
    val recycleKg: String = "",
    val buttsG: String = "",
    val note: String = "",
    /** 저울 사진의 기기 내 파일 경로 */
    val scalePhotos: List<String> = emptyList(),
)

sealed interface CollectionUiState {
    data class Editing(
        val form: CollectionForm = CollectionForm(),
        val isSubmitting: Boolean = false,
        val error: String? = null,
    ) : CollectionUiState

    data object Submitted : CollectionUiState
}
