package com.example.nightclean.navigation

import kotlinx.serialization.Serializable

/** 화면 목적지. 컴포저블 `XxxRoute`와의 이름 충돌 방지를 위해 `AppRoute`로 묶음 */
sealed interface AppRoute {
    /** signOut: 세션을 지우고 로그인 화면으로 */
    @Serializable data class Login(val signOut: Boolean = false) : AppRoute
    @Serializable data object ZoneMap : AppRoute
    @Serializable data class Shift(val zoneId: String) : AppRoute
    @Serializable data class Checkin(val shiftId: String, val zoneId: String, val startedAt: Long) : AppRoute
    /** beforePhotos: 근무 화면에서 찍은 청소 전 사진의 기기 내 경로 */
    @Serializable data class Collection(
        val shiftId: String,
        val zoneId: String,
        val startedAt: Long,
        val beforePhotos: List<String> = emptyList(),
    ) : AppRoute
    @Serializable data object ShopReport : AppRoute
}
