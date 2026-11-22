package com.example.nightclean.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.example.nightclean.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
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

    private companion object {
        const val BUCKET = "collection-photos"
    }
}

/** 긴 변 maxSide 이하로 줄이고 EXIF 회전을 반영한 JPEG. 원본 12MP 사진을 수백 KB로 */
internal fun compressJpeg(file: File, maxSide: Int = 1600, quality: Int = 80): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val decoded = checkNotNull(BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })) {
        "사진을 읽지 못함"
    }
    val degrees = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val longSide = maxOf(decoded.width, decoded.height)
    val scale = if (longSide > maxSide) maxSide.toFloat() / longSide else 1f
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(degrees)
    }
    val bitmap = if (scale == 1f && degrees == 0f) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
}
