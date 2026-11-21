package com.example.nightclean.di

import com.example.nightclean.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** 인증(세션 저장·토큰 갱신) 전용 */
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient =
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY) {
            install(Auth)
        }

    /** Supabase REST(PostgREST) 클라이언트 */
    @Provides
    @Singleton
    fun provideHttpClient(supabase: SupabaseClient): HttpClient = HttpClient(OkHttp) {
        expectSuccess = true
        install(HttpTimeout) { requestTimeoutMillis = 10_000 }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        // 로그인 중이면 사용자 토큰을 실어 RLS가 사용자를 구분. 없으면 anon
        install(createClientPlugin("SupabaseUserToken") {
            onRequest { request, _ -> supabase.auth.currentAccessTokenOrNull()?.let { request.bearerAuth(it) } }
        })
        defaultRequest {
            url("${BuildConfig.SUPABASE_URL}/rest/v1/")
            headers.append("apikey", BuildConfig.SUPABASE_KEY)
        }
    }
}
