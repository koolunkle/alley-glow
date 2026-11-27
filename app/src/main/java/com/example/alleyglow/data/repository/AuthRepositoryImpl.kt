package com.example.alleyglow.data.repository

import com.example.alleyglow.data.remote.AuthRemoteDataSource
import com.example.alleyglow.data.remote.dto.toUserRole
import com.example.alleyglow.domain.model.UserRole
import com.example.alleyglow.domain.repository.AuthRepository
import com.example.alleyglow.domain.repository.UnregisteredAccountException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
) : AuthRepository {

    override suspend fun restoreSession(): Result<UserRole?> = result {
        if (remote.hasSession()) role() else null
    }

    override suspend fun signIn(email: String, password: String): Result<UserRole> = result {
        remote.signIn(email, password)
        role()
    }

    override suspend fun signOut() = remote.signOut()

    override suspend fun displayName(): Result<String?> = result { remote.fetchProfile()?.displayName }

    /** 역할이 없는 계정은 세션을 남기지 않음 */
    private suspend fun role(): UserRole =
        remote.fetchProfile()?.toUserRole() ?: run {
            remote.signOut()
            throw UnregisteredAccountException()
        }

    private inline fun <T> result(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
