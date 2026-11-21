package com.example.nightclean.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EstimatedIncomeTest {

    @Test
    fun `4시간 근무는 시급 12000원 기준 48000원이다`() {
        assertEquals(48_000L, estimateIncome(workedMinutes = 240, buttsGram = 0.0).wage)
    }

    @Test
    fun `분 단위로 계산한다`() {
        assertEquals(6_200L, estimateIncome(workedMinutes = 31, buttsGram = 0.0).wage)
    }

    @Test
    fun `꽁초 보상은 1g당 30원이고 합계에 더해진다`() {
        val income = estimateIncome(workedMinutes = 60, buttsGram = 250.5)
        assertEquals(7_515L, income.buttReward)
        assertEquals(19_515L, income.total)
    }

    @Test
    fun `음수 입력은 0으로 본다`() {
        assertEquals(EstimatedIncome(0, 0), estimateIncome(workedMinutes = -5, buttsGram = -1.0))
    }
}
