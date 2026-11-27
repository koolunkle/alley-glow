package com.example.alleyglow.data.remote

import com.example.alleyglow.data.remote.dto.GuestVisitDto
import com.example.alleyglow.data.remote.dto.ShopReportDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class ShopReportRemoteDataSource @Inject constructor(
    private val client: HttpClient,
) {
    /** 인자 없는 stable 함수라 GET으로 호출 */
    suspend fun fetchReport(): ShopReportDto? = client.get("rpc/shop_report").body()

    suspend fun insertGuestVisit(visit: GuestVisitDto) {
        client.post("guest_visits") {
            contentType(ContentType.Application.Json)
            header("Prefer", "return=minimal")
            setBody(visit)
        }
    }
}
