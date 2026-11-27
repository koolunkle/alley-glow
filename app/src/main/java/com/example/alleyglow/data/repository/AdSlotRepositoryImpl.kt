package com.example.alleyglow.data.repository

import com.example.alleyglow.data.remote.AdSlotRemoteDataSource
import com.example.alleyglow.data.remote.dto.toAdSlot
import com.example.alleyglow.domain.model.AdSlot
import com.example.alleyglow.domain.repository.AdSlotRepository
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
