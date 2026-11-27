package io.github.koolunkle.alleyglow.data.repository

import io.github.koolunkle.alleyglow.data.remote.ZoneRemoteDataSource
import io.github.koolunkle.alleyglow.data.remote.dto.toZone
import io.github.koolunkle.alleyglow.domain.model.Zone
import io.github.koolunkle.alleyglow.domain.repository.ZoneRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ZoneRepositoryImpl @Inject constructor(
    private val remote: ZoneRemoteDataSource,
) : ZoneRepository {

    override suspend fun getZones(): Result<List<Zone>> = try {
        Result.success(remote.fetchZones().map { it.toZone() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
