package com.example.nightclean.ui.collection

import com.example.nightclean.domain.model.BAG_SLOT_HOURS

/** 입력 중인 값. 무게는 입력 그대로의 문자열 */
data class CollectionForm(
    val bags: List<Int> = List(BAG_SLOT_HOURS.size) { 0 },
    /** 저울로 쟀는지. 아니면 무게 칸 없이 봉투 수로만 기록 */
    val weighed: Boolean = false,
    val generalKg: String = "",
    val recycleKg: String = "",
    val buttsG: String = "",
    val note: String = "",
    /** 근무 종료 사진(저울 눈금 또는 채운 봉투)의 기기 내 파일 경로 */
    val photos: List<String> = emptyList(),
    /** 청소 후 사진(근무 화면의 청소 전 사진과 같은 자리) */
    val afterPhotos: List<String> = emptyList(),
)

sealed interface CollectionUiState {
    data class Editing(
        val form: CollectionForm = CollectionForm(),
        val isSubmitting: Boolean = false,
        val error: String? = null,
    ) : CollectionUiState

    data object Submitted : CollectionUiState
}
