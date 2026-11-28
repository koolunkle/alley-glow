package io.github.koolunkle.alleyglow.ui.collection

import io.github.koolunkle.alleyglow.domain.model.NIGHT_SLOT_HOURS

/** 입력 중인 값. 무게는 입력 그대로의 문자열 */
data class CollectionForm(
    val bags: List<Int> = List(NIGHT_SLOT_HOURS.size) { 0 },
    val generalKg: String = "",
    val recycleKg: String = "",
    val note: String = "",
    /** 근무 종료 사진(저울 눈금 또는 채운 봉투)의 기기 내 파일 경로 */
    val photos: List<String> = emptyList(),
    /** 청소 후 사진(근무 화면의 청소 전 사진과 같은 자리) */
    val afterPhotos: List<String> = emptyList(),
) {
    /** 무게 칸 중 하나라도 적었으면 저울을 쓴 근무(비운 칸은 0) */
    val weighed: Boolean get() = generalKg.isNotBlank() || recycleKg.isNotBlank()
}

sealed interface CollectionUiState {
    data class Editing(
        val form: CollectionForm = CollectionForm(),
        val isSubmitting: Boolean = false,
        /** 문구는 화면이 리소스로 보임 */
        val error: CollectionError? = null,
        /** 봉투 0장으로 저장하려 할 때 한 번 더 묻는 중 */
        val confirmZeroBags: Boolean = false,
    ) : CollectionUiState

    data object Submitted : CollectionUiState
}

enum class CollectionError { WEIGHT, PHOTO, SAVE }
