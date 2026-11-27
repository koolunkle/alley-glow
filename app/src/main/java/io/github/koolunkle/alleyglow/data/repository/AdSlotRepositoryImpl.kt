package io.github.koolunkle.alleyglow.data.repository

import io.github.koolunkle.alleyglow.data.remote.AdSlotRemoteDataSource
import io.github.koolunkle.alleyglow.data.remote.dto.toAdSlot
import io.github.koolunkle.alleyglow.domain.model.AdSlot
import io.github.koolunkle.alleyglow.domain.repository.AdSlotRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class AdSlotRepositoryImpl @Inject constructor(
    private val remote: AdSlotRemoteDataSource,
) : AdSlotRepository {

    override suspend fun getAdSlots(): Result<List<AdSlot>> = try {
        Result.success(remote.fetchAdSlots().map { it.toAdSlot() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
