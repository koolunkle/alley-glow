package com.example.nightclean.data.repository

import com.example.nightclean.data.remote.CheckinRemoteDataSource
import com.example.nightclean.data.remote.dto.CheckinRequestDto
import com.example.nightclean.data.remote.dto.toCheckinResult
import com.example.nightclean.data.remote.dto.toIsoUtc
import com.example.nightclean.domain.model.CheckinResult
import com.example.nightclean.domain.model.GeoPoint
import com.example.nightclean.domain.repository.CheckinRepository
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
}
