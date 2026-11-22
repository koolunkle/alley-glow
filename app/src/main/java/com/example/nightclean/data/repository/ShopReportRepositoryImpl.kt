package com.example.nightclean.data.repository

import com.example.nightclean.data.remote.PhotoRemoteDataSource
import com.example.nightclean.data.remote.ShopReportRemoteDataSource
import com.example.nightclean.data.remote.dto.GuestVisitDto
import com.example.nightclean.data.remote.dto.toShopReport
import com.example.nightclean.domain.model.ShopReport
import com.example.nightclean.domain.repository.ShopReportRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ShopReportRepositoryImpl @Inject constructor(
    private val remote: ShopReportRemoteDataSource,
    private val photos: PhotoRemoteDataSource,
) : ShopReportRepository {

    override suspend fun getReport(): Result<ShopReport?> = result { remote.fetchReport()?.toShopReport() }

    override suspend fun addGuest(shopId: Long): Result<Unit> = result { remote.insertGuestVisit(GuestVisitDto(shopId)) }

    override suspend fun getPhoto(path: String): Result<ByteArray> = result { photos.download(path) }

    private inline fun <T> result(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
