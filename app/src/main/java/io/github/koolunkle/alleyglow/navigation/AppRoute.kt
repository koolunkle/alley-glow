package io.github.koolunkle.alleyglow.navigation

import kotlinx.serialization.Serializable

/** 화면 목적지. 컴포저블 `XxxRoute`와의 이름 충돌 방지를 위해 `AppRoute`로 묶음 */
sealed interface AppRoute {
    /** signOut: 세션을 지우고 로그인 화면으로 */
    @Serializable data class Login(val signOut: Boolean = false) : AppRoute
    @Serializable data object ZoneMap : AppRoute
    /** zoneName: 화면 표시용 구역 이름 */
    @Serializable data class Shift(val zoneId: String, val zoneName: String) : AppRoute
    @Serializable data class Checkin(val shiftId: String, val zoneId: String, val startedAt: Long) : AppRoute
    /** beforePhotos: 근무 화면에서 찍은 청소 전 사진의 기기 내 경로 */
    @Serializable data class Collection(
        val shiftId: String,
        val zoneId: String,
        val startedAt: Long,
        val beforePhotos: List<String> = emptyList(),
        /** 근무 중에 +1로 센 시간대별 봉투 수(근무 종료 화면의 초기값) */
        val bagsByHour: List<Int> = emptyList(),
    ) : AppRoute
    @Serializable data object ShopReport : AppRoute
}
