package com.example.nightclean.data.remote

import com.example.nightclean.data.remote.dto.CollectionDto
import com.example.nightclean.data.remote.dto.ShiftDto
import com.example.nightclean.data.remote.dto.ShiftStartDto
import com.example.nightclean.data.remote.dto.TrackPointDto
import io.ktor.client.HttpClient
import io.ktor.client.request.header
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

    private suspend inline fun <reified T : Any> insert(table: String, row: T, onConflict: String) {
        client.post(table) {
            contentType(ContentType.Application.Json)
            header("Prefer", "resolution=$onConflict,return=minimal")
            setBody(row)
        }
    }
}
