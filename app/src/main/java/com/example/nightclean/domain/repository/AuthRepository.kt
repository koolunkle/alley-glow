package com.example.nightclean.domain.repository

import com.example.nightclean.domain.model.UserRole

interface AuthRepository {
    /** 저장된 세션의 역할. 세션이 없으면 null */
    suspend fun restoreSession(): Result<UserRole?>
    suspend fun signIn(email: String, password: String): Result<UserRole>
    suspend fun signOut()
}

/** 로그인은 됐지만 profiles에 역할이 없는 계정 */
class UnregisteredAccountException : Exception()
