package io.github.koolunkle.alleyglow.domain.model

/**
 * 250m 격자. score: 배치 추천도(0~1).
 * 지표(공공데이터 분석): 금·토·평일 19~23시 평균 인구, 가장 가까운 가로휴지통 거리, 21~24시 추정매출(분기 평균, 원)
 * ByHour: 시간대 칸(BAG_SLOT_HOURS 순, 19~20 … 22~23시)별 추천도와 인구. 없으면 빈 목록
 */
data class Zone(
    val id: String,
    val name: String,
    val score: Float,
    val nightPop: Int? = null,
    val weekdayPop: Int? = null,
    val binDistM: Int? = null,
    val nightSales: Long? = null,
    val scoreByHour: List<Float> = emptyList(),
    val nightPopByHour: List<Int> = emptyList(),
    val weekdayPopByHour: List<Int> = emptyList(),
) {
    /** slot: 시간대 칸(0~3). null이거나 시간대 값이 없으면 19~23시 평균 */
    fun scoreAt(slot: Int?): Float = slot?.let { scoreByHour.getOrNull(it) } ?: score
    fun nightPopAt(slot: Int?): Int? = slot?.let { nightPopByHour.getOrNull(it) } ?: nightPop
    fun weekdayPopAt(slot: Int?): Int? = slot?.let { weekdayPopByHour.getOrNull(it) } ?: weekdayPop
}
