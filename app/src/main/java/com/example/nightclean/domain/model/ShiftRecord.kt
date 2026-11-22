package com.example.nightclean.domain.model

/** 봉투 수를 나눠 적는 시간대의 시작 시각(19~20 … 22~23시) */
val BAG_SLOT_HOURS = listOf(19, 20, 21, 22)

/** 근무 1회와 근무 종료 때 입력한 수거량 */
data class ShiftRecord(
    val shiftId: String,
    val zoneId: String,
    val startedAt: Long,
    val endedAt: Long,
    val bagsByHour: List<Int>,
    val generalKg: Double,
    val recycleKg: Double,
    val buttsG: Double,
    val note: String?,
    /** 저울 사진의 기기 내 파일 경로(1~3장). 저장할 때 업로드 */
    val scalePhotos: List<String>,
)

const val MAX_SCALE_PHOTOS = 3
