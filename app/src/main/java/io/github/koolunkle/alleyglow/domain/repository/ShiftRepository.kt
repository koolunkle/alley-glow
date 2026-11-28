package io.github.koolunkle.alleyglow.domain.repository

import io.github.koolunkle.alleyglow.domain.model.ShiftRecord
import io.github.koolunkle.alleyglow.domain.model.ShiftTrack
import io.github.koolunkle.alleyglow.domain.model.ZoneShare

interface ShiftRepository {
    /** 같은 shiftId로 다시 호출해도 중복 저장되지 않음. 체크인으로 먼저 생긴 근무 행은 종료 시각을 채움 */
    suspend fun finishShift(record: ShiftRecord): Result<Unit>

    /** 배정 구역 안 GPS 기록 비율. 기록이 없으면 null */
    suspend fun getZoneShare(shiftId: String): Result<ZoneShare?>

    /** 근무 GPS 경로와 배정 구역 칸 중심. 기록이 없으면 null */
    suspend fun getTrack(shiftId: String, zoneId: String): Result<ShiftTrack?>
}
