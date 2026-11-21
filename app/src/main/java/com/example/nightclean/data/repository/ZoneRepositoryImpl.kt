package com.example.nightclean.data.repository

import com.example.nightclean.data.remote.ZoneRemoteDataSource
import com.example.nightclean.data.remote.dto.toZone
import com.example.nightclean.domain.model.Zone
import com.example.nightclean.domain.repository.ZoneRepository
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
