package com.example.nightclean.navigation

import kotlinx.serialization.Serializable

/** 화면 목적지. 컴포저블 `XxxRoute`와의 이름 충돌 방지를 위해 `AppRoute`로 묶음 */
sealed interface AppRoute {
    @Serializable data object ZoneMap : AppRoute
    @Serializable data class Shift(val zoneId: String) : AppRoute
    @Serializable data object Checkin : AppRoute
    @Serializable data object Collection : AppRoute
    @Serializable data object ShopReport : AppRoute
}
