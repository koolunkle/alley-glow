package com.example.nightclean.domain.repository

import com.example.nightclean.domain.model.CheckinResult
import com.example.nightclean.domain.model.GeoPoint

interface CheckinRepository {
    /** 근무 행이 없으면 함께 만듦. 같은 가게를 다시 찍어도 기록은 1건 */
    suspend fun checkIn(
        shiftId: String,
        zoneId: String,
        startedAt: Long,
        shopCode: String,
        location: GeoPoint,
    ): Result<CheckinResult>

    /** 이 근무에서 체크인한 가게 수. 근무 행이 아직 없으면 0 */
    suspend fun countCheckins(shiftId: String): Result<Int>
}
