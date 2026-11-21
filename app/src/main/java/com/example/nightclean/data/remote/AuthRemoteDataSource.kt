package com.example.nightclean.data.remote

import com.example.nightclean.data.remote.dto.ProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
    private val client: HttpClient,
) {
    /** 저장소에서 세션을 다 불러올 때까지 기다린 뒤 로그인 여부 반환 */
    suspend fun hasSession(): Boolean =
        supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing } is SessionStatus.Authenticated

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 서버 로그아웃이 실패해도 기기의 세션은 지움
            supabase.auth.clearSession()
        }
    }

    /** RLS로 본인 행만 돌아옴 */
    suspend fun fetchProfile(): ProfileDto? = client.get("profiles") {
        parameter("select", "role")
    }.body<List<ProfileDto>>().firstOrNull()
}
