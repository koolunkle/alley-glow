package com.example.nightclean.data.remote.dto

import com.example.nightclean.domain.model.UserRole
import kotlinx.serialization.Serializable

/** Supabase `profiles` 테이블 행 */
@Serializable
data class ProfileDto(
    val role: String,
)

fun ProfileDto.toUserRole(): UserRole? = when (role) {
    "worker" -> UserRole.WORKER
    "shop" -> UserRole.SHOP
    else -> null
}
