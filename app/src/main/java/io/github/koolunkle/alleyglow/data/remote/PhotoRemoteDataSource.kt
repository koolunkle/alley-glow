package io.github.koolunkle.alleyglow.data.remote

import android.graphics.Bitmap
import io.github.koolunkle.alleyglow.BuildConfig
import io.github.koolunkle.alleyglow.image.decodeUpright
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/** Supabase Storage `collection-photos` 업로드. 경로는 {user_id}/{상대 경로} */
class PhotoRemoteDataSource @Inject constructor(
    private val client: HttpClient,
    private val supabase: SupabaseClient,
) {
    /** 줄여서 올리고 Storage 경로(버킷 제외)를 돌려줌. 같은 경로면 덮어씀(재시도 안전) */
    suspend fun upload(localPath: String, relativePath: String): String {
        val userId = checkNotNull(supabase.auth.currentUserOrNull()?.id) { "로그인 필요" }
        val path = "$userId/$relativePath"
        val bytes = withContext(Dispatchers.IO) { compressJpeg(File(localPath)) }
        client.post("${BuildConfig.SUPABASE_URL}/storage/v1/object/$BUCKET/$path") {
            header("x-upsert", "true")
            contentType(ContentType.Image.JPEG)
            setBody(bytes)
        }
        return path
    }

    /** 비공개 버킷에서 로그인 토큰으로 내려받음. 읽기 권한은 Storage 정책이 판정 */
    suspend fun download(path: String): ByteArray =
        client.get("${BuildConfig.SUPABASE_URL}/storage/v1/object/authenticated/$BUCKET/$path").body()

    private companion object {
        const val BUCKET = "collection-photos"
    }
}

/** 똑바로 세워 긴 변 maxSide 이하로 줄인 JPEG. 원본 12MP 사진을 수백 KB로 */
internal fun compressJpeg(file: File, maxSide: Int = 1600, quality: Int = 80): ByteArray {
    val bitmap = checkNotNull(decodeUpright(file.path, maxSide)) { "사진을 읽지 못함" }
    return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
}
