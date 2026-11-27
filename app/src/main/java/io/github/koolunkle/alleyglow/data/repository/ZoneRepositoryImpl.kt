package io.github.koolunkle.alleyglow.data.repository

import io.github.koolunkle.alleyglow.data.remote.ZoneRemoteDataSource
import io.github.koolunkle.alleyglow.data.remote.dto.toZone
import io.github.koolunkle.alleyglow.domain.model.Zone
import io.github.koolunkle.alleyglow.domain.repository.ZoneRepository
import javax.inject.Inject

class ZoneRepositoryImpl @Inject constructor(
    private val remote: ZoneRemoteDataSource,
) : ZoneRepository {

    override suspend fun getZones(): Result<List<Zone>> = result {
        remote.fetchZones().map { it.toZone() }
    }
}
