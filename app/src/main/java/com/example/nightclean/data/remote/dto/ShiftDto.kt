package com.example.nightclean.data.remote.dto

import com.example.nightclean.domain.model.ShiftRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Supabase `shifts` 테이블 행. worker_id는 DB 기본값(auth.uid()) */
@Serializable
data class ShiftDto(
    val id: String,
    @SerialName("zone_id") val zoneId: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String,
)

/** Supabase `collections` 테이블 행 */
@Serializable
data class CollectionDto(
    @SerialName("shift_id") val shiftId: String,
    @SerialName("bags_by_hour") val bagsByHour: List<Int>,
    @SerialName("general_kg") val generalKg: Double,
    @SerialName("recycle_kg") val recycleKg: Double,
    @SerialName("butts_g") val buttsG: Double,
    val note: String?,
)

fun ShiftRecord.toShiftDto() = ShiftDto(
    id = shiftId,
    zoneId = zoneId,
    startedAt = startedAt.toIsoUtc(),
    endedAt = endedAt.toIsoUtc(),
)

fun ShiftRecord.toCollectionDto() = CollectionDto(
    shiftId = shiftId,
    bagsByHour = bagsByHour,
    generalKg = generalKg,
    recycleKg = recycleKg,
    buttsG = buttsG,
    note = note,
)

/** minSdk 24라 java.time 대신 SimpleDateFormat */
private fun Long.toIsoUtc(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(this))
