package com.example.nightclean.data.remote

import com.example.nightclean.data.remote.dto.CheckinRequestDto
import com.example.nightclean.data.remote.dto.CheckinResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
}
