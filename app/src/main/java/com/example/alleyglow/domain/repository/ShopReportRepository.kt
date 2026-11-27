package com.example.alleyglow.domain.repository

import com.example.alleyglow.domain.model.ShopReport

interface ShopReportRepository {
    /** 계정에 연결된 가게가 없으면 null */
    suspend fun getReport(): Result<ShopReport?>

    /** "클린 워커 보고 왔어요" 손님 1명 기록 */
    suspend fun addGuest(shopId: Long): Result<Unit>

    /** 리포트의 청소 전후 사진(JPEG). 이 가게에 체크인한 근무의 사진만 읽을 수 있음 */
    suspend fun getPhoto(path: String): Result<ByteArray>
}
