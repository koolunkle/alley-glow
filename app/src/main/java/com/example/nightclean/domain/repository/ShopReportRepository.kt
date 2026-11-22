package com.example.nightclean.domain.repository

import com.example.nightclean.domain.model.ShopReport

interface ShopReportRepository {
    /** 계정에 연결된 가게가 없으면 null */
    suspend fun getReport(): Result<ShopReport?>

    /** "클린 보고 왔어요" 손님 1명 기록 */
    suspend fun addGuest(shopId: Long): Result<Unit>
}
