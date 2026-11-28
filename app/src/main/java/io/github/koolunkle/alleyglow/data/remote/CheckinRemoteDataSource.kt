package io.github.koolunkle.alleyglow.data.remote

import io.github.koolunkle.alleyglow.data.remote.dto.CheckinRecordDto
import io.github.koolunkle.alleyglow.data.remote.dto.CheckinRequestDto
import io.github.koolunkle.alleyglow.data.remote.dto.CheckinResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class CheckinRemoteDataSource @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun checkIn(request: CheckinRequestDto): CheckinResponseDto? = client.post("rpc/check_in") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body<List<CheckinResponseDto>>().firstOrNull()

    /** RLS로 본인 근무의 체크인만 보임. 체크인 시각 순 */
    suspend fun fetchCheckins(shiftId: String): List<CheckinRecordDto> = client.get("checkins") {
        parameter("select", "checked_at,shops(name)")
        parameter("shift_id", "eq.$shiftId")
        parameter("order", "checked_at")
    }.body()
}
