package com.example.alleyglow.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface

/**
 * 긴 변을 maxSide 이하로 줄이고 EXIF 방향을 반영해 똑바로 세운 비트맵.
 * 카메라 앱은 세로 사진을 가로 픽셀 + 회전 태그로 저장하는 경우가 많아, 태그를 무시하면 옆으로 눕는다.
 * 업로드용 압축과 화면 썸네일이 같이 쓴다. 읽지 못하면 null
 */
fun decodeUpright(path: String, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null

    val degrees = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    val longSide = maxOf(decoded.width, decoded.height)
    val scale = if (longSide > maxSide) maxSide.toFloat() / longSide else 1f
    if (scale == 1f && degrees == 0f) return decoded
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(degrees)
    }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}
