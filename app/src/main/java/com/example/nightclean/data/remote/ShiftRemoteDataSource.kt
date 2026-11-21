package com.example.nightclean.data.remote

import com.example.nightclean.data.remote.dto.CollectionDto
import com.example.nightclean.data.remote.dto.ShiftDto
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
    suspend fun insertShift(shift: ShiftDto) = insertIgnoringDuplicate("shifts", shift)

    suspend fun insertCollection(collection: CollectionDto) = insertIgnoringDuplicate("collections", collection)

    /** 기본키가 이미 있으면 건너뜀(ON CONFLICT DO NOTHING). 재시도해도 안전 */
    private suspend inline fun <reified T : Any> insertIgnoringDuplicate(table: String, row: T) {
        client.post(table) {
            contentType(ContentType.Application.Json)
            header("Prefer", "resolution=ignore-duplicates,return=minimal")
            setBody(row)
        }
    }
}
