package io.github.koolunkle.alleyglow.data.repository

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun `값은 success로 감싼다`() {
        assertEquals(Result.success(3), result { 3 })
    }

    @Test
    fun `예외는 failure로 감싼다`() {
        assertTrue(result { error("x") }.exceptionOrNull() is IllegalStateException)
    }

    @Test(expected = CancellationException::class)
    fun `취소는 다시 던진다`() {
        result { throw CancellationException() }
    }
}
