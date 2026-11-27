package io.github.koolunkle.alleyglow.data.repository

import io.github.koolunkle.alleyglow.data.remote.PhotoRemoteDataSource
import io.github.koolunkle.alleyglow.data.remote.ShopReportRemoteDataSource
import io.github.koolunkle.alleyglow.data.remote.dto.GuestVisitDto
import io.github.koolunkle.alleyglow.data.remote.dto.toShopReport
import io.github.koolunkle.alleyglow.domain.model.ShopReport
import io.github.koolunkle.alleyglow.domain.repository.ShopReportRepository
import javax.inject.Inject

class ShopReportRepositoryImpl @Inject constructor(
    private val remote: ShopReportRemoteDataSource,
    private val photos: PhotoRemoteDataSource,
) : ShopReportRepository {

    override suspend fun getReport(): Result<ShopReport?> = result { remote.fetchReport()?.toShopReport() }

    override suspend fun addGuest(shopId: Long): Result<Unit> = result { remote.insertGuestVisit(GuestVisitDto(shopId)) }

    override suspend fun getPhoto(path: String): Result<ByteArray> = result { photos.download(path) }
}
