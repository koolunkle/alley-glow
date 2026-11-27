package com.example.alleyglow.data.remote

import com.example.alleyglow.data.remote.dto.AdSlotDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject

// shortcut: 구역 구분 없이 전 구역 공통 슬롯, 상권 확대 시 zone_id 컬럼 추가
class AdSlotRemoteDataSource @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun fetchAdSlots(): List<AdSlotDto> = client.get("ad_slots") {
        parameter("select", "shop_name,start_hour,end_hour,placement")
        parameter("order", "start_hour")
    }.body()
}
