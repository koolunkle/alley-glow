package com.example.nightclean.domain.model

data class GeoPoint(val lat: Double, val lng: Double)

/** 체크인 판정(가게 50m 안만 인정, 판정은 서버 check_in 함수) */
sealed interface CheckinResult {
    data class CheckedIn(val shopName: String, val distanceM: Int) : CheckinResult
    data class TooFar(val shopName: String, val distanceM: Int) : CheckinResult
    /** shopCode: QR에서 읽어 보낸 값(잘못 만든 QR을 찾는 데 씀) */
    data class UnknownShop(val shopCode: String) : CheckinResult
}

/** QR 값에서 가게 코드 추출. URL이면 마지막 경로 */
fun shopCodeOf(qr: String): String = qr.trim().trimEnd('/').substringAfterLast('/')
