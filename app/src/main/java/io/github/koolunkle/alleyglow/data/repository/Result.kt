package io.github.koolunkle.alleyglow.data.repository

import kotlinx.coroutines.CancellationException

/** 예외를 Result로 감싸되 코루틴 취소는 다시 던짐(runCatching은 취소까지 삼킴) */
internal inline fun <T> result(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
