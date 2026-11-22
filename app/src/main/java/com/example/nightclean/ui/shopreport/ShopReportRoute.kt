package com.example.nightclean.ui.shopreport

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nightclean.R
import com.example.nightclean.domain.model.NightRecord
import com.example.nightclean.ui.common.AppTopBar
import com.example.nightclean.domain.model.ShopReport
import com.example.nightclean.ui.theme.NightCleanTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ShopReportRoute(
    onSignOut: () -> Unit,
    viewModel: ShopReportViewModel = hiltViewModel(),
) {
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShopReportScreen(
        uiState = uiState,
        onAddGuest = viewModel::addGuest,
        onRetry = viewModel::load,
        onSignOut = onSignOut,
        loadPhoto = viewModel::loadPhoto,
    )
}

@Composable
fun ShopReportScreen(
    uiState: ShopReportUiState,
    onAddGuest: () -> Unit,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    /** 청소 전후 사진(JPEG 바이트). 미리보기에서는 null을 돌려줌 */
    loadPhoto: suspend (path: String) -> ByteArray?,
) {
    Scaffold(topBar = { AppTopBar(stringResource(R.string.role_shop), onSignOut) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (uiState) {
                ShopReportUiState.Loading -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                ShopReportUiState.NoShop -> Text("이 계정에 연결된 가게가 없어요. 운영자에게 문의해 주세요.")
                is ShopReportUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(uiState.message)
                    Button(onClick = onRetry) { Text("다시 시도") }
                }
                is ShopReportUiState.Success -> Report(uiState, onAddGuest, loadPhoto)
            }
        }
    }
}

@Composable
private fun Report(uiState: ShopReportUiState.Success, onAddGuest: () -> Unit, loadPhoto: suspend (String) -> ByteArray?) {
    val report = uiState.report
    Column {
        Text("${report.shopName} 사장님", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "${report.month}월 우리 골목 리포트",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Metric("청소한 밤", "${report.nights}회", "가게 앞 체크인 기준", Modifier.weight(1f))
        Metric("치운 쓰레기", "${formatKg(report.collectedKg)}kg", "들른 근무 중 저울로 잰 양", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Metric("홍보물 QR 스캔", "${report.qrScans}회", "시민이 랜딩을 연 횟수", Modifier.weight(1f))
        Metric(
            "거리 청결도",
            report.cleanScore?.let { "%.1f점".format(Locale.US, it) } ?: "-",
            "QR 랜딩 평가 평균(5점 만점)",
            Modifier.weight(1f),
        )
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("“클린 보고 왔어요” 손님", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            Text(
                "${report.guests}명",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
        }
        Button(
            onClick = onAddGuest,
            enabled = !uiState.isAddingGuest,
            modifier = Modifier.height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            if (uiState.isAddingGuest) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("+1 기록", fontWeight = FontWeight.Black)
            }
        }
    }
    uiState.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }

    Text("최근 청소 기록", fontWeight = FontWeight.Bold)
    if (report.recent.isEmpty()) {
        Text("아직 청소 기록이 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    report.recent.forEach { NightRow(it, loadPhoto) }
}

@Composable
private fun Metric(label: String, value: String, caption: String, modifier: Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NightRow(night: NightRecord, loadPhoto: suspend (String) -> ByteArray?) {
    val time = if (night.firstAt == night.lastAt) night.firstAt else "${night.firstAt}–${night.lastAt}"
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 시안처럼 전·후 썸네일. 사진이 없는 밤은 생략
        if (night.beforePhoto != null || night.afterPhoto != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NightPhoto(night.beforePhoto, "전", loadPhoto)
                NightPhoto(night.afterPhoto, "후", loadPhoto)
            }
        }
        Column {
            Text("${formatNight(night.night)} $time", fontWeight = FontWeight.Bold)
            Text(
                "가게 앞 체크인 ${night.checkins}회",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 52dp 썸네일. 원본(긴 변 1600px, 업로드 때 방향 보정됨)을 작게 디코딩 */
@Composable
private fun NightPhoto(path: String?, label: String, loadPhoto: suspend (String) -> ByteArray?) {
    val image by produceState<ImageBitmap?>(null, path) {
        val bytes = path?.let { loadPhoto(it) } ?: return@produceState
        value = withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = 8 })?.asImageBitmap()
        }
    }
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.BottomCenter,
    ) {
        image?.let { Image(it, contentDescription = "청소 $label 사진", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)).padding(horizontal = 4.dp),
        )
    }
}

/** 2026-11-29 → 11/29 (일). 형식이 다르면 원문 */
internal fun formatNight(night: String): String =
    runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(night) }.getOrNull()
        ?.let { SimpleDateFormat("M/d (E)", Locale.KOREA).format(it) }
        ?: night

/** 소수점 아래가 0이면 정수로(16.0 → 16, 12.5 → 12.5) */
internal fun formatKg(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toLong().toString() else "%.1f".format(Locale.US, kg)

@Preview
@Composable
private fun ShopReportScreenPreview() {
    NightCleanTheme {
        ShopReportScreen(
            uiState = ShopReportUiState.Success(
                ShopReport(
                    shopId = 1, shopName = "[가게명]", month = 11, nights = 4, collectedKg = 38.5, guests = 7,
                    qrScans = 23, cleanScore = 4.2,
                    recent = listOf(
                        NightRecord("2026-11-29", 3, "19:12", "22:40", beforePhoto = "b", afterPhoto = "a"),
                        NightRecord("2026-11-28", 2, "20:05", "21:30"),
                    ),
                ),
            ),
            onAddGuest = {}, onRetry = {}, onSignOut = {}, loadPhoto = { null },
        )
    }
}
