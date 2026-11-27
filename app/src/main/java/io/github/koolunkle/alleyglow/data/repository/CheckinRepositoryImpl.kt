package io.github.koolunkle.alleyglow.data.repository

import io.github.koolunkle.alleyglow.data.remote.CheckinRemoteDataSource
import io.github.koolunkle.alleyglow.data.remote.dto.CheckinRequestDto
import io.github.koolunkle.alleyglow.data.remote.dto.toCheckinResult
import io.github.koolunkle.alleyglow.data.remote.dto.toIsoUtc
import io.github.koolunkle.alleyglow.domain.model.CheckinResult
import io.github.koolunkle.alleyglow.domain.model.GeoPoint
import io.github.koolunkle.alleyglow.domain.repository.CheckinRepository
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
