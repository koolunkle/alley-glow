package com.example.alleyglow.data.remote.dto

import com.example.alleyglow.domain.model.AdSlot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase `ad_slots` 테이블 행 */
@Serializable
data class AdSlotDto(
    @SerialName("shop_name") val shopName: String,
    @SerialName("start_hour") val startHour: Int,
    @SerialName("end_hour") val endHour: Int,
    val placement: String,
)

fun AdSlotDto.toAdSlot() = AdSlot(shopName = shopName, startHour = startHour, endHour = endHour, placement = placement)
