package com.example.nightclean.domain.model

// shortcut: 원가표 가정 단가를 상수로 둠, 실제 광고 단가·보상 제도가 정해지면 DB 값으로
const val HOURLY_WAGE_WON = 12_000L

/** 성동구 꽁초 수거보상제 단가. 서울시 보상은 아직 없는 제도라 '예상'으로만 표시 */
const val BUTT_REWARD_WON_PER_GRAM = 30

data class EstimatedIncome(val wage: Long, val buttReward: Long) {
    val total: Long get() = wage + buttReward
}

fun estimateIncome(workedMinutes: Long, buttsGram: Double): EstimatedIncome = EstimatedIncome(
    wage = workedMinutes.coerceAtLeast(0) * HOURLY_WAGE_WON / 60,
    buttReward = (buttsGram.coerceAtLeast(0.0) * BUTT_REWARD_WON_PER_GRAM).toLong(),
)
