package io.github.koolunkle.alleyglow.data.remote.dto

import io.github.koolunkle.alleyglow.domain.model.Zone
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
    @SerialName("score_by_hour") val scoreByHour: List<Float>? = null,
    @SerialName("night_pop_by_hour") val nightPopByHour: List<Int>? = null,
    @SerialName("weekday_pop_by_hour") val weekdayPopByHour: List<Int>? = null,
)

fun ZoneDto.toZone() = Zone(
    gridId, name, score, nightPop, weekdayPop, binDistM, nightSales,
    scoreByHour.orEmpty(), nightPopByHour.orEmpty(), weekdayPopByHour.orEmpty(),
)
