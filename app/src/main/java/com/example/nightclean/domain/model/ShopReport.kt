package com.example.nightclean.domain.model

/** 상인 가게의 이번 달 리포트(서울 시각, 자정 넘은 체크인은 전날 밤) */
data class ShopReport(
    val shopId: Long,
    val shopName: String,
    val month: Int,
    val nights: Int,
    /** 이 가게에 체크인한 근무들의 수거량 합계(일반 + 재활용 + 꽁초) */
    val collectedKg: Double,
    val guests: Int,
    /** 최근 밤부터 최대 5개 */
    val recent: List<NightRecord>,
)

/** night: yyyy-MM-dd, firstAt·lastAt: HH:mm. 사진은 Storage 경로(그 밤 청소 전·후 1장씩, 없으면 null) */
data class NightRecord(
    val night: String,
    val checkins: Int,
    val firstAt: String,
    val lastAt: String,
    val beforePhoto: String? = null,
    val afterPhoto: String? = null,
)
