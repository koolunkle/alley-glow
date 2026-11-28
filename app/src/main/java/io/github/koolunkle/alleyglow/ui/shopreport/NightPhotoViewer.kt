package io.github.koolunkle.alleyglow.ui.shopreport

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.domain.model.NightRecord
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 사진을 내려받아 디코딩. sampleSize 8은 썸네일(긴 변 200px), 1은 원본(긴 변 1600px) */
@Composable
internal fun rememberPhoto(path: String?, sampleSize: Int, loadPhoto: suspend (String) -> ByteArray?): State<ImageBitmap?> =
    produceState<ImageBitmap?>(null, path, sampleSize) {
        val bytes = path?.let { loadPhoto(it) } ?: return@produceState
        value = withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize })?.asImageBitmap()
        }
    }

/** 한 밤의 청소 전·후 사진을 화면 가득 위아래로 놓고 비교 */
@Composable
internal fun NightPhotoViewer(night: NightRecord, loadPhoto: suspend (String) -> ByteArray?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatNight(night.night), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.close))
                }
            }
            LargePhoto(night.beforePhoto, stringResource(R.string.report_photo_before), loadPhoto)
            LargePhoto(night.afterPhoto, stringResource(R.string.report_photo_after), loadPhoto)
        }
    }
}

@Composable
private fun LargePhoto(path: String?, label: String, loadPhoto: suspend (String) -> ByteArray?) {
    val image by rememberPhoto(path, sampleSize = 1, loadPhoto)
    val desc = stringResource(R.string.report_photo_desc, label)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(desc, style = MaterialTheme.typography.titleSmall)
        Box(
            Modifier
                .fillMaxWidth()
                // 받은 사진 비율대로(가로 사진이면 전·후 두 장이 한 화면에), 받기 전엔 세로 3:4 자리
                .aspectRatio(image?.let { it.width.toFloat() / it.height } ?: (3f / 4f))
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = image
            when {
                bitmap != null -> Image(bitmap, contentDescription = desc, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                path == null -> Text(stringResource(R.string.report_photo_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> CircularProgressIndicator()
            }
        }
    }
}
