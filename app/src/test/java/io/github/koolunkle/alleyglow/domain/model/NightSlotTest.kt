package io.github.koolunkle.alleyglow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NightSlotTest {

    @Test
    fun `정오 전 시각은 전날 밤의 연장이다`() {
        assertEquals(listOf(19, 23, 24, 25, 35, 12), listOf(19, 23, 0, 1, 11, 12).map(::nightHour))
    }

    @Test
    fun `봉투 시간대 칸은 19시 전은 첫 칸, 23시 이후와 새벽은 마지막 칸이다`() {
        assertEquals(listOf(0, 0, 3, 3, 3), listOf(15, 19, 22, 23, 1).map(::bagSlotOf))
    }

    @Test
    fun `시간대 칸은 19~22시만 해당하고 나머지는 평균이다`() {
        assertEquals(listOf(null, 0, 1, 2, 3, null, null), listOf(18, 19, 20, 21, 22, 23, 1).map(::nightSlotOf))
    }
}
