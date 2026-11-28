package io.github.koolunkle.alleyglow.data.remote.dto

import io.github.koolunkle.alleyglow.domain.model.NightRecord
import io.github.koolunkle.alleyglow.domain.model.ShopReport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `rpc/shop_report` 결과(JSON 객체, 상인이 아니면 null) */
@Serializable
data class ShopReportDto(
    @SerialName("shop_id") val shopId: Long,
    @SerialName("shop_name") val shopName: String,
    val month: Int,
    val nights: Int,
    @SerialName("collected_bags") val collectedBags: Int = 0,
    val guests: Int,
    @SerialName("qr_scans") val qrScans: Int = 0,
    @SerialName("clean_score") val cleanScore: Double? = null,
    @SerialName("ad_minutes") val adMinutes: Int = 0,
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
    collectedBags = collectedBags,
    guests = guests,
    qrScans = qrScans,
    cleanScore = cleanScore,
    adMinutes = adMinutes,
    recent = recent.map { NightRecord(it.night, it.checkins, it.firstAt, it.lastAt, it.beforePhoto, it.afterPhoto) },
)
