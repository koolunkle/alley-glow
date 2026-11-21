package com.example.nightclean.data.repository

import com.example.nightclean.data.remote.AdSlotRemoteDataSource
import com.example.nightclean.data.remote.dto.toAdSlot
import com.example.nightclean.domain.model.AdSlot
import com.example.nightclean.domain.repository.AdSlotRepository
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
