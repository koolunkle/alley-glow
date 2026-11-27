package io.github.koolunkle.alleyglow.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** 근무 중 GPS 기록(동의 후 근무 중에만) */
interface TrackingRepository {
    val isTracking: StateFlow<Boolean>

    /** 위치 권한이 있을 때만 부름. 이미 기록 중이면 무시 */
    fun start(shiftId: String, zoneId: String, startedAt: Long)

    /** 남은 위치를 올리고 멈춤 */
    fun stop()
}
