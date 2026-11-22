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
    /** 저울로 잰 무게. 저울이 없으면 null(봉투 수로만 기록) */
    val weight: Weight?,
    val note: String?,
    /** 근무 종료 사진의 기기 내 파일 경로(1~3장). 저장할 때 업로드 */
    val photos: List<String>,
    /** 청소 전·후 사진(같은 자리, 같은 각도). 선택, 각 최대 2장 */
    val beforePhotos: List<String> = emptyList(),
    val afterPhotos: List<String> = emptyList(),
)

data class Weight(val generalKg: Double, val recycleKg: Double, val buttsG: Double)

const val MAX_PHOTOS = 3
const val MAX_BEFORE_AFTER_PHOTOS = 2

// shortcut: 100L 봉투 1장 평균 무게 가정값, 이태원 시범 실측(봉투 수와 kg)이 나오면 그 평균으로
const val KG_PER_BAG_ESTIMATE = 6.0

/** 저울이 없을 때 화면에 보여 줄 추정 무게. 저장하지 않음 */
fun estimateKgFromBags(bags: Int): Double = bags.coerceAtLeast(0) * KG_PER_BAG_ESTIMATE
