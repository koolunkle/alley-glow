package io.github.koolunkle.alleyglow.data.repository

import io.github.koolunkle.alleyglow.data.remote.PhotoRemoteDataSource
import io.github.koolunkle.alleyglow.data.remote.ShiftRemoteDataSource
import io.github.koolunkle.alleyglow.data.remote.dto.toCollectionDto
import io.github.koolunkle.alleyglow.data.remote.dto.toShiftDto
import io.github.koolunkle.alleyglow.domain.model.ShiftRecord
import io.github.koolunkle.alleyglow.domain.model.ZoneShare
import io.github.koolunkle.alleyglow.domain.repository.ShiftRepository
import javax.inject.Inject

class ShiftRepositoryImpl @Inject constructor(
    private val remote: ShiftRemoteDataSource,
    private val photos: PhotoRemoteDataSource,
) : ShiftRepository {

    // 근무 → 사진 → 수거량 순서(collections가 shifts를 참조하고 사진 경로를 담음)
    override suspend fun finishShift(record: ShiftRecord): Result<Unit> = result {
        remote.upsertShift(record.toShiftDto())
        // 파일 이름 고정(photo-1, before-1 …)이라 재시도해도 같은 파일을 덮어씀
        suspend fun upload(locals: List<String>, prefix: String) = locals.mapIndexed { i, local ->
            photos.upload(local, "${record.shiftId}/$prefix-${i + 1}.jpg")
        }
        val collection = record.toCollectionDto(
            photoPaths = upload(record.photos, "photo"),
            beforePhotoPaths = upload(record.beforePhotos, "before"),
            afterPhotoPaths = upload(record.afterPhotos, "after"),
        )
        remote.insertCollection(collection)
    }

    override suspend fun getZoneShare(shiftId: String): Result<ZoneShare?> = result {
        remote.fetchZoneShare(shiftId)?.takeIf { it.total > 0 }?.let { ZoneShare(it.total, it.inside) }
    }
}
