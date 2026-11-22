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
    /** false면 무게 세 칸은 null(DB check 제약) */
    val weighed: Boolean,
    @SerialName("general_kg") val generalKg: Double?,
    @SerialName("recycle_kg") val recycleKg: Double?,
    @SerialName("butts_g") val buttsG: Double?,
    val note: String?,
    /** Storage 경로(버킷 제외) */
    @SerialName("photo_paths") val photoPaths: List<String>,
    @SerialName("before_photo_paths") val beforePhotoPaths: List<String>,
    @SerialName("after_photo_paths") val afterPhotoPaths: List<String>,
)

fun ShiftRecord.toShiftDto() = ShiftDto(
    id = shiftId,
    zoneId = zoneId,
    startedAt = startedAt.toIsoUtc(),
    endedAt = endedAt.toIsoUtc(),
)

fun ShiftRecord.toCollectionDto(photoPaths: List<String>, beforePhotoPaths: List<String>, afterPhotoPaths: List<String>) = CollectionDto(
    shiftId = shiftId,
    bagsByHour = bagsByHour,
    weighed = weight != null,
    generalKg = weight?.generalKg,
    recycleKg = weight?.recycleKg,
    buttsG = weight?.buttsG,
    note = note,
    photoPaths = photoPaths,
    beforePhotoPaths = beforePhotoPaths,
    afterPhotoPaths = afterPhotoPaths,
)

/** minSdk 24라 java.time 대신 SimpleDateFormat */
internal fun Long.toIsoUtc(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(this))
