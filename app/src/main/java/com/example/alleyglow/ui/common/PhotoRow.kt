package com.example.alleyglow.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.alleyglow.R
import com.example.alleyglow.image.decodeUpright
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 찍은 사진 썸네일(삭제 가능) + 촬영 버튼. 시스템 카메라로만 찍음(갤러리 선택 불가).
 * 사진은 캐시 photos/ 에 저장되고, 경로를 onAdd로 넘김. 업로드는 호출하는 쪽에서
 */
@Composable
fun PhotoRow(
    photos: List<String>,
    max: Int,
    label: String,
    enabled: Boolean,
    onAdd: (path: String) -> Unit,
    onRemove: (index: Int) -> Unit,
) {
    // IDE 미리보기에는 카메라를 띄울 액티비티가 없어 썸네일 자리만 그림
    if (LocalInspectionMode.current) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(photos.size) { Box(Modifier.size(88.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.background)) }
        }
        return
    }
    val context = LocalContext.current
    val addLabel = stringResource(R.string.photo_add_desc, label, photos.size + 1)
    // 촬영 중 화면이 재생성돼도 저장할 파일 경로를 잃지 않게
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pending ?: return@rememberLauncherForActivityResult
        pending = null
        if (saved) onAdd(path) else File(path).delete()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        photos.forEachIndexed { index, path ->
            PhotoThumb(path, "$label ${index + 1}", enabled) { onRemove(index) }
        }
        if (photos.size < max) {
            Surface(
                onClick = {
                    val file = File(File(context.cacheDir, "photos").apply { mkdirs() }, "photo-${System.currentTimeMillis()}.jpg")
                    pending = file.path
                    takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                // 카드(surfaceVariant) 안에 놓이므로 한 단계 어두운 바탕
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.size(88.dp).semantics { contentDescription = addLabel },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null, modifier = Modifier.size(28.dp))
                    Text("${photos.size}/$max", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** EXIF 방향을 반영해 작게 디코딩(IO 스레드). 오른쪽 위 ×로 삭제 */
@Composable
private fun PhotoThumb(path: String, label: String, enabled: Boolean, onRemove: () -> Unit) {
    val image by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { decodeUpright(path, maxSide = 264)?.asImageBitmap() }
    }
    Box(Modifier.size(88.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.background)) {
        image?.let { Image(it, contentDescription = label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        if (enabled) {
            // 보이는 원은 24dp, 누르는 영역은 40dp
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.photo_remove_desc, label), onClick = onRemove),
                Alignment.Center,
            ) {
                Box(Modifier.size(24.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape), Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
