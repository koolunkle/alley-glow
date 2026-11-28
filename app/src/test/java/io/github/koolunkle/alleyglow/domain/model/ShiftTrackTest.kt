package io.github.koolunkle.alleyglow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftTrackTest {

    @Test
    fun `배정 칸 안 기록 비율은 중심 125m 안을 세고 반올림한다`() {
        val center = GeoPoint(37.5, 127.0)
        val north = { m: Double -> GeoPoint(37.5 + m / 111_320.0, 127.0) }
        // 120m는 안, 130m는 밖: 40/44 = 90.9% → 91
        val track = ShiftTrack(center, List(40) { north(120.0) } + List(4) { north(130.0) })
        assertEquals(ZoneShare(total = 44, inside = 40), track.zoneShare())
        assertEquals(91, track.zoneShare().percent)
    }

    @Test
    fun `중심에서 동쪽과 북쪽으로 떨어진 거리를 미터로 구한다`() {
        val center = GeoPoint(37.5, 127.0)
        val (nx, ny) = GeoPoint(37.501, 127.0).metersFrom(center)
        assertEquals(0.0, nx, 0.01)
        assertEquals(111.32, ny, 0.01)
        // 위도 37.5도에서 경도 0.001도는 약 88.3m
        val (ex, ey) = GeoPoint(37.5, 127.001).metersFrom(center)
        assertEquals(88.32, ex, 0.01)
        assertEquals(0.0, ey, 0.01)
    }
}
