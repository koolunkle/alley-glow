package io.github.koolunkle.alleyglow.domain.model

/** 봉투 수를 나눠 적는 시간대의 시작 시각(19~20 … 22~23시) */
val BAG_SLOT_HOURS = listOf(19, 20, 21, 22)

/** 지금 시각의 봉투 시간대 칸. 19시 전은 첫 칸, 23시 이후(자정 넘김 포함)는 마지막 칸 */
fun bagSlotOf(hourOfDay: Int): Int = (nightHour(hourOfDay) - BAG_SLOT_HOURS.first()).coerceIn(0, BAG_SLOT_HOURS.lastIndex)

/** 지금 시각이 들어 있는 시간대 칸. 19~23시 밖이면 null(평균) */
fun nightSlotOf(hourOfDay: Int): Int? = (hourOfDay - BAG_SLOT_HOURS.first()).takeIf { it in BAG_SLOT_HOURS.indices }

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
