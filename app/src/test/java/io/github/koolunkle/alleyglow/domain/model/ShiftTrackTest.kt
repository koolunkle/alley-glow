package io.github.koolunkle.alleyglow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftTrackTest {

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
