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
)

fun ZoneDto.toZone() = Zone(id = gridId, name = name, score = score)
