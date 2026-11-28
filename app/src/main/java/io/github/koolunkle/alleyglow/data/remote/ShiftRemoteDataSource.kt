package io.github.koolunkle.alleyglow.data.remote

import io.github.koolunkle.alleyglow.data.remote.dto.CollectionDto
import io.github.koolunkle.alleyglow.data.remote.dto.ShiftDto
import io.github.koolunkle.alleyglow.data.remote.dto.ShiftStartDto
import io.github.koolunkle.alleyglow.data.remote.dto.TrackPointDto
import io.github.koolunkle.alleyglow.data.remote.dto.ZoneShareDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class ShiftRemoteDataSource @Inject constructor(
    private val client: HttpClient,
) {
    /** 체크인 때 종료 시각 없이 만들어진 근무 행이 있으면 갱신(upsert) */
    suspend fun upsertShift(shift: ShiftDto) = insert("shifts", shift, onConflict = "merge-duplicates")

    /** 근무 행이 없을 때만 만듦(있으면 그대로). 체크인 전에 GPS를 올릴 때 */
    suspend fun insertShiftIfAbsent(shift: ShiftStartDto) = insert("shifts", shift, onConflict = "ignore-duplicates")

    suspend fun insertTrackPoints(points: List<TrackPointDto>) = insert("track_points", points, onConflict = "ignore-duplicates")

    /** 이미 있으면 건너뜀. 재시도해도 안전 */
    suspend fun insertCollection(collection: CollectionDto) = insert("collections", collection, onConflict = "ignore-duplicates")

    /** 근무의 GPS 기록 중 배정 구역 칸 안에 든 수(서버 함수, RLS로 본인 근무만) */
    suspend fun fetchZoneShare(shiftId: String): ZoneShareDto? = client.get("rpc/shift_zone_share") {
        parameter("p_shift_id", shiftId)
    }.body<List<ZoneShareDto>>().firstOrNull()

    private suspend inline fun <reified T : Any> insert(table: String, row: T, onConflict: String) {
        client.post(table) {
            contentType(ContentType.Application.Json)
            header("Prefer", "resolution=$onConflict,return=minimal")
            setBody(row)
        }
    }
}
