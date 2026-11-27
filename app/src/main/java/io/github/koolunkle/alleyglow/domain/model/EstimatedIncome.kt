package io.github.koolunkle.alleyglow.domain.model

// shortcut: 원가표 가정 시급을 상수로 둠, 실제 시급이 정해지면 DB 값으로
const val HOURLY_WAGE_WON = 12_000L

/** 시급제 예상 수당(분 단위). 수거량과 무관 */
fun estimateWage(workedMinutes: Long): Long = workedMinutes.coerceAtLeast(0) * HOURLY_WAGE_WON / 60
