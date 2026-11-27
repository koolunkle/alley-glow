package com.example.alleyglow.data.repository

import com.example.alleyglow.data.remote.CheckinRemoteDataSource
import com.example.alleyglow.data.remote.dto.CheckinRequestDto
import com.example.alleyglow.data.remote.dto.toCheckinResult
import com.example.alleyglow.data.remote.dto.toIsoUtc
import com.example.alleyglow.domain.model.CheckinResult
import com.example.alleyglow.domain.model.GeoPoint
import com.example.alleyglow.domain.repository.CheckinRepository
import kotlinx.coroutines.CancellationException
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
    ): Result<CheckinResult> = try {
        val request = CheckinRequestDto(shiftId, zoneId, startedAt.toIsoUtc(), shopCode, location.lat, location.lng)
        Result.success(remote.checkIn(request).toCheckinResult(shopCode))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun countCheckins(shiftId: String): Result<Int> = try {
        Result.success(remote.countCheckins(shiftId))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
