package com.example.nightclean.domain.model

/** 250m 격자. score: 배치 추천도(0~1) */
data class Zone(
    val id: String,
    val name: String,
    val score: Float,
)
