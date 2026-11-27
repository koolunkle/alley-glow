package io.github.koolunkle.alleyglow.domain.repository

import io.github.koolunkle.alleyglow.domain.model.ShiftRecord

interface ShiftRepository {
    /** 같은 shiftId로 다시 호출해도 중복 저장되지 않음. 체크인으로 먼저 생긴 근무 행은 종료 시각을 채움 */
    suspend fun finishShift(record: ShiftRecord): Result<Unit>
}
