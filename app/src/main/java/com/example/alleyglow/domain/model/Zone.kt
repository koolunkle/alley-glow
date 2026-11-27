package com.example.alleyglow.domain.model

/**
 * 250m 격자. score: 배치 추천도(0~1).
 * 지표(공공데이터 분석): 금·토·평일 19~23시 평균 인구, 가장 가까운 가로휴지통 거리, 21~24시 추정매출(분기 평균, 원)
 */
data class Zone(
    val id: String,
    val name: String,
    val score: Float,
    val nightPop: Int? = null,
    val weekdayPop: Int? = null,
    val binDistM: Int? = null,
    val nightSales: Long? = null,
)
