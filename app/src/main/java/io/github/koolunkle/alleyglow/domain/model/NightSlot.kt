package io.github.koolunkle.alleyglow.domain.model

/** 밤 시간대 칸의 시작 시각(19~20 … 22~23시). 봉투 기록, 근무 종료 폼, 구역 지도 시간대 칩이 같은 칸을 씀 */
val NIGHT_SLOT_HOURS = listOf(19, 20, 21, 22)

/** 정오 전 시각을 전날 밤의 연장(24~35)으로 환산 */
fun nightHour(hourOfDay: Int): Int = if (hourOfDay < 12) hourOfDay + 24 else hourOfDay

/** 봉투를 기록할 칸. 19시 전은 첫 칸, 23시 이후(자정 넘김 포함)는 마지막 칸 */
fun bagSlotOf(hourOfDay: Int): Int = (nightHour(hourOfDay) - NIGHT_SLOT_HOURS.first()).coerceIn(0, NIGHT_SLOT_HOURS.lastIndex)

/** 지금 시각이 들어 있는 칸. 19~23시 밖이면 null(구역 지도는 평균) */
fun nightSlotOf(hourOfDay: Int): Int? = (hourOfDay - NIGHT_SLOT_HOURS.first()).takeIf { it in NIGHT_SLOT_HOURS.indices }
