package io.github.koolunkle.alleyglow.data.remote.dto

import io.github.koolunkle.alleyglow.domain.model.CheckedInShop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckinDtoTest {

    @Test
    fun `체크인 시각은 소수 초와 시간대 표기가 있어도 epoch ms로 읽는다`() {
        // 2026-11-21 10:31:00 UTC
        val expected = 1_795_257_060_000L
        assertEquals(CheckedInShop("[가게명 1]", expected), CheckinRecordDto("2026-11-21T10:31:00.123456+00:00", ShopNameDto("[가게명 1]")).toCheckedInShop())
        assertEquals(expected, "2026-11-21T19:31:00+09:00".parseIsoUtc())
    }

    @Test
    fun `형식이 다른 시각은 버린다`() {
        assertNull(CheckinRecordDto("어제", ShopNameDto("가게")).toCheckedInShop())
    }
}
