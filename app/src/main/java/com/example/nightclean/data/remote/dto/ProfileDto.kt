package com.example.nightclean.data.remote.dto

import com.example.nightclean.domain.model.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Supabase `profiles` 테이블 행 */
@Serializable
data class ProfileDto(
    val role: String,
    @SerialName("display_name") val displayName: String? = null,
)

fun ProfileDto.toUserRole(): UserRole? = when (role) {
    "worker" -> UserRole.WORKER
    "shop" -> UserRole.SHOP
    else -> null
}
