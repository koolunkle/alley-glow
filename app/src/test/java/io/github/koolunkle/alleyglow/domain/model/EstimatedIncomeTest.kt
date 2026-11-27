package io.github.koolunkle.alleyglow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EstimatedIncomeTest {

    @Test
    fun `4시간 근무는 시급 12000원 기준 48000원이다`() {
        assertEquals(48_000L, estimateWage(240))
    }

    @Test
    fun `분 단위로 계산한다`() {
        assertEquals(6_200L, estimateWage(31))
    }

    @Test
    fun `음수 입력은 0으로 본다`() {
        assertEquals(0L, estimateWage(-5))
    }
}
