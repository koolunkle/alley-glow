package com.example.nightclean.domain.repository

import com.example.nightclean.domain.model.ShiftRecord

interface ShiftRepository {
    /** 같은 shiftId로 다시 호출해도 중복 저장되지 않음 */
    suspend fun finishShift(record: ShiftRecord): Result<Unit>
}
