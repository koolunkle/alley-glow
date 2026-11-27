package io.github.koolunkle.alleyglow.data.repository

import io.github.koolunkle.alleyglow.data.remote.AdSlotRemoteDataSource
import io.github.koolunkle.alleyglow.data.remote.dto.toAdSlot
import io.github.koolunkle.alleyglow.domain.model.AdSlot
import io.github.koolunkle.alleyglow.domain.repository.AdSlotRepository
import javax.inject.Inject

class AdSlotRepositoryImpl @Inject constructor(
    private val remote: AdSlotRemoteDataSource,
) : AdSlotRepository {

    override suspend fun getAdSlots(): Result<List<AdSlot>> = result {
        remote.fetchAdSlots().map { it.toAdSlot() }
    }
}
