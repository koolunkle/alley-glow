package com.example.nightclean.data.repository

import com.example.nightclean.data.remote.PhotoRemoteDataSource
import com.example.nightclean.data.remote.ShiftRemoteDataSource
import com.example.nightclean.data.remote.dto.toCollectionDto
import com.example.nightclean.data.remote.dto.toShiftDto
import com.example.nightclean.domain.model.ShiftRecord
import com.example.nightclean.domain.repository.ShiftRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ShiftRepositoryImpl @Inject constructor(
    private val remote: ShiftRemoteDataSource,
    private val photos: PhotoRemoteDataSource,
) : ShiftRepository {

    // 근무 → 사진 → 수거량 순서(collections가 shifts를 참조하고 사진 경로를 담음)
    override suspend fun finishShift(record: ShiftRecord): Result<Unit> = try {
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
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
