package io.github.koolunkle.alleyglow.data.repository

import io.github.koolunkle.alleyglow.data.remote.CheckinRemoteDataSource
import io.github.koolunkle.alleyglow.data.remote.dto.CheckinRequestDto
import io.github.koolunkle.alleyglow.data.remote.dto.toCheckedInShop
import io.github.koolunkle.alleyglow.data.remote.dto.toCheckinResult
import io.github.koolunkle.alleyglow.data.remote.dto.toIsoUtc
import io.github.koolunkle.alleyglow.domain.model.CheckedInShop
import io.github.koolunkle.alleyglow.domain.model.CheckinResult
import io.github.koolunkle.alleyglow.domain.model.GeoPoint
import io.github.koolunkle.alleyglow.domain.repository.CheckinRepository
import javax.inject.Inject

class CheckinRepositoryImpl @Inject constructor(
    private val remote: CheckinRemoteDataSource,
) : CheckinRepository {

    override suspend fun checkIn(
        shiftId: String,
        zoneId: String,
        startedAt: Long,
        shopCode: String,
        location: GeoPoint,
    ): Result<CheckinResult> = result {
        val request = CheckinRequestDto(shiftId, zoneId, startedAt.toIsoUtc(), shopCode, location.lat, location.lng)
        remote.checkIn(request).toCheckinResult(shopCode)
    }

    override suspend fun getCheckins(shiftId: String): Result<List<CheckedInShop>> = result {
        remote.fetchCheckins(shiftId).mapNotNull { it.toCheckedInShop() }
    }
}
