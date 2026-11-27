package com.example.alleyglow.data.remote

import com.example.alleyglow.data.remote.dto.CheckinRequestDto
import com.example.alleyglow.data.remote.dto.CheckinResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

class CheckinRemoteDataSource @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun checkIn(request: CheckinRequestDto): CheckinResponseDto? = client.post("rpc/check_in") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body<List<CheckinResponseDto>>().firstOrNull()

    /** RLS로 본인 근무의 체크인만 보임 */
    suspend fun countCheckins(shiftId: String): Int = client.get("checkins") {
        parameter("select", "id")
        parameter("shift_id", "eq.$shiftId")
    }.body<List<JsonObject>>().size
}
