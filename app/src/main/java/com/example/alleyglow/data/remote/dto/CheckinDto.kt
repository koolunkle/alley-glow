package com.example.alleyglow.data.remote.dto

import com.example.alleyglow.domain.model.CheckinResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `rpc/check_in` 인자 */
@Serializable
data class CheckinRequestDto(
    @SerialName("p_shift_id") val shiftId: String,
    @SerialName("p_zone_id") val zoneId: String,
    @SerialName("p_started_at") val startedAt: String,
    @SerialName("p_qr_code") val qrCode: String,
    @SerialName("p_lat") val lat: Double,
    @SerialName("p_lng") val lng: Double,
)

/** `rpc/check_in` 결과 행. 모르는 QR이면 행 없음 */
@Serializable
data class CheckinResponseDto(
    @SerialName("shop_name") val shopName: String,
    @SerialName("distance_m") val distanceM: Int,
    @SerialName("checked_in") val checkedIn: Boolean,
)

fun CheckinResponseDto?.toCheckinResult(shopCode: String): CheckinResult = when {
    this == null -> CheckinResult.UnknownShop(shopCode)
    checkedIn -> CheckinResult.CheckedIn(shopName, distanceM)
    else -> CheckinResult.TooFar(shopName, distanceM)
}
