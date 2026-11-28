package io.github.koolunkle.alleyglow.domain.repository

import io.github.koolunkle.alleyglow.domain.model.CheckedInShop
import io.github.koolunkle.alleyglow.domain.model.CheckinResult
import io.github.koolunkle.alleyglow.domain.model.GeoPoint

interface CheckinRepository {
    /** 근무 행이 없으면 함께 만듦. 같은 가게를 다시 찍어도 기록은 1건 */
    suspend fun checkIn(
        shiftId: String,
        zoneId: String,
        startedAt: Long,
        shopCode: String,
        location: GeoPoint,
    ): Result<CheckinResult>

    /** 이 근무에서 체크인한 가게(시각 순). 근무 행이 아직 없으면 빈 목록 */
    suspend fun getCheckins(shiftId: String): Result<List<CheckedInShop>>
}
