package com.example.nightclean.data.remote.dto

import com.example.nightclean.domain.model.Zone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase `zones` 테이블 행 */
@Serializable
data class ZoneDto(
    @SerialName("grid_id") val gridId: String,
    val name: String,
    val score: Float,
    @SerialName("night_pop") val nightPop: Int? = null,
    @SerialName("weekday_pop") val weekdayPop: Int? = null,
    @SerialName("bin_dist_m") val binDistM: Int? = null,
    @SerialName("night_sales") val nightSales: Long? = null,
)

fun ZoneDto.toZone() = Zone(gridId, name, score, nightPop, weekdayPop, binDistM, nightSales)
