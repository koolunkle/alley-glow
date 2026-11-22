package com.example.nightclean.data.remote.dto

import com.example.nightclean.domain.model.NightRecord
import com.example.nightclean.domain.model.ShopReport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `rpc/shop_report` 결과(JSON 객체, 상인이 아니면 null) */
@Serializable
data class ShopReportDto(
    @SerialName("shop_id") val shopId: Long,
    @SerialName("shop_name") val shopName: String,
    val month: Int,
    val nights: Int,
    @SerialName("collected_kg") val collectedKg: Double,
    val guests: Int,
    val recent: List<NightRecordDto>,
)

@Serializable
data class NightRecordDto(
    val night: String,
    val checkins: Int,
    @SerialName("first_at") val firstAt: String,
    @SerialName("last_at") val lastAt: String,
    @SerialName("before_photo") val beforePhoto: String? = null,
    @SerialName("after_photo") val afterPhoto: String? = null,
)

/** `guest_visits` 삽입 행 */
@Serializable
data class GuestVisitDto(@SerialName("shop_id") val shopId: Long)

fun ShopReportDto.toShopReport() = ShopReport(
    shopId = shopId,
    shopName = shopName,
    month = month,
    nights = nights,
    collectedKg = collectedKg,
    guests = guests,
    recent = recent.map { NightRecord(it.night, it.checkins, it.firstAt, it.lastAt, it.beforePhoto, it.afterPhoto) },
)
