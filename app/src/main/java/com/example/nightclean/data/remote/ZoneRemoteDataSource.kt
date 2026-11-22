package com.example.nightclean.data.remote

import com.example.nightclean.data.remote.dto.ZoneDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject

// shortcut: 인증만 supabase-kt, PostgREST·Storage는 Ktor로 직접 호출. 실시간 구독 등 기능이 늘면 supabase-kt로 통일 검토
class ZoneRemoteDataSource @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun fetchZones(): List<ZoneDto> = client.get("zones") {
        parameter("select", "grid_id,name,score")
        parameter("order", "grid_id")
    }.body()
}
