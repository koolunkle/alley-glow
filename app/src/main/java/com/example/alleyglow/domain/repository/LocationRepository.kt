package com.example.alleyglow.domain.repository

import com.example.alleyglow.domain.model.GeoPoint

interface LocationRepository {
    /** 현재 위치. 위치 서비스가 꺼졌거나 권한이 없으면 null */
    suspend fun currentLocation(): GeoPoint?
}
