package io.github.koolunkle.alleyglow.domain.model

/** 가게 광고 슬롯. 시각은 시 단위, 자정 이후는 24 이상 */
data class AdSlot(
    val shopName: String,
    val startHour: Int,
    val endHour: Int,
    val placement: String,
)

fun List<AdSlot>.currentAt(nightHour: Int): AdSlot? = find { nightHour in it.startHour until it.endHour }

fun List<AdSlot>.nextAfter(nightHour: Int): AdSlot? = filter { it.startHour > nightHour }.minByOrNull { it.startHour }
