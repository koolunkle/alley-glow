package com.example.nightclean.data.repository

import com.example.nightclean.data.remote.ShiftRemoteDataSource
import com.example.nightclean.data.remote.dto.toCollectionDto
import com.example.nightclean.data.remote.dto.toShiftDto
import com.example.nightclean.domain.model.ShiftRecord
import com.example.nightclean.domain.repository.ShiftRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ShiftRepositoryImpl @Inject constructor(
    private val remote: ShiftRemoteDataSource,
) : ShiftRepository {

    // collections가 shifts를 참조하므로 근무 먼저 저장
    override suspend fun finishShift(record: ShiftRecord): Result<Unit> = try {
        remote.insertShift(record.toShiftDto())
        remote.insertCollection(record.toCollectionDto())
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
