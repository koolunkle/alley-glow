package io.github.koolunkle.alleyglow.domain.model

/** 근무 GPS 기록 중 배정 구역 칸(중심 ±125m) 안에 든 수 */
data class ZoneShare(val total: Int, val inside: Int) {
    val percent: Int get() = Math.round(inside * 100f / total)
}

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
