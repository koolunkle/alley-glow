package com.example.nightclean.ui.shift

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nightclean.domain.model.AdSlot
import com.example.nightclean.domain.model.MAX_BEFORE_AFTER_PHOTOS
import com.example.nightclean.domain.model.currentAt
import com.example.nightclean.domain.model.nextAfter
import com.example.nightclean.domain.model.nightHour
import com.example.nightclean.ui.common.PhotoRow
import com.example.nightclean.ui.theme.NightCleanTheme
import com.example.nightclean.ui.theme.Warning
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ShiftRoute(
    zoneId: String,
    onCheckin: (shiftId: String, startedAt: Long) -> Unit,
    onFinish: (shiftId: String, startedAt: Long, beforePhotos: List<String>) -> Unit,
    viewModel: ShiftViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val beforePhotos by viewModel.beforePhotos.collectAsStateWithLifecycle()
    val isTracking by viewModel.isTracking.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    val context = LocalContext.current
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    // 정확한 위치가 있어야 기록. 알림 권한은 없어도 기록은 되며 알림만 안 보임
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        permissionDenied = grants[Manifest.permission.ACCESS_FINE_LOCATION] != true
        if (!permissionDenied) viewModel.startTracking(zoneId)
    }
    ShiftScreen(
        zoneId = zoneId,
        uiState = uiState,
        startedAt = viewModel.startedAt,
        now = now,
        beforePhotos = beforePhotos,
        isTracking = isTracking,
        permissionDenied = permissionDenied,
        onRetry = viewModel::load,
        onStartTracking = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) {
                permissionDenied = false
                viewModel.startTracking(zoneId)
            } else {
                permissionLauncher.launch(
                    buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                    }.toTypedArray(),
                )
            }
        },
        onAddBeforePhoto = viewModel::addBeforePhoto,
        onRemoveBeforePhoto = viewModel::removeBeforePhoto,
        onCheckin = { onCheckin(viewModel.shiftId, viewModel.startedAt) },
        onFinish = { onFinish(viewModel.shiftId, viewModel.startedAt, beforePhotos) },
    )
}

@Composable
fun ShiftScreen(
    zoneId: String,
    uiState: ShiftUiState,
    startedAt: Long,
    now: Long,
    beforePhotos: List<String>,
    isTracking: Boolean,
    permissionDenied: Boolean,
    onRetry: () -> Unit,
    onStartTracking: () -> Unit,
    onAddBeforePhoto: (String) -> Unit,
    onRemoveBeforePhoto: (Int) -> Unit,
    onCheckin: () -> Unit,
    onFinish: () -> Unit,
) {
    Scaffold(
        // 내용이 길어져도 근무 버튼은 항상 보이게 아래에 고정
        bottomBar = {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCheckin, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("가게 체크인 (QR)", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("근무 종료 · 수거량 입력", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    if (isTracking) "근무 중 · GPS 기록 중" else "근무 중",
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text("구역 $zoneId", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ShiftTimer(startedAt, now)
            if (!isTracking) TrackingConsent(permissionDenied, onStartTracking)
            when (uiState) {
                ShiftUiState.Loading -> Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator() }
                is ShiftUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(uiState.message)
                    Button(onClick = onRetry) { Text("다시 시도") }
                }
                is ShiftUiState.Success -> {
                    val hour = nightHour(now.hourOfDay())
                    val current = uiState.slots.currentAt(hour)
                    val next = uiState.slots.nextAfter(hour)
                    if (current == null && next == null) {
                        Text("오늘 남은 광고가 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    current?.let { SlotCard("지금 광고 중", it, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary) }
                    next?.let { SlotCard("다음 슬롯", it, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("청소 전 사진 (선택, 최대 ${MAX_BEFORE_AFTER_PHOTOS}장)", fontWeight = FontWeight.Bold)
                Text(
                    "치우기 전에 찍어 두면 근무 종료 때 같은 자리에서 청소 후 사진을 찍어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PhotoRow(beforePhotos, MAX_BEFORE_AFTER_PHOTOS, "청소 전 사진", enabled = true, onAddBeforePhoto, onRemoveBeforePhoto)
            }
        }
    }
}

/** 위치 수집 동의(메모: 근무 중에만 수집, 동의 화면 1개). 근무마다 눌러야 기록이 시작됨 */
@Composable
private fun TrackingConsent(permissionDenied: Boolean, onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("위치 기록 동의", fontWeight = FontWeight.Bold)
        Text(
            "근무 중에만 30초마다 위치를 기록하고, 근무를 마치면 멈춰요. 담당 구역에서 일했는지 확인하는 데만 써요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (permissionDenied) {
            Text("정확한 위치 권한이 있어야 기록할 수 있어요.", style = MaterialTheme.typography.bodySmall, color = Warning)
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("동의하고 GPS 기록 시작", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ShiftTimer(startedAt: Long, now: Long) {
    val minutes = ((now - startedAt) / 60_000).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("근무 시간", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("%02d:%02d".format(minutes / 60, minutes % 60), fontSize = 56.sp, fontWeight = FontWeight.Black)
        Text(
            "${SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(startedAt))} 시작",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SlotCard(label: String, slot: AdSlot, container: Color, content: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "$label · ${slot.startHour % 24}–${slot.endHour % 24}시",
            color = content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(slot.shopName, color = content, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(slot.placement, color = content, style = MaterialTheme.typography.bodySmall)
    }
}

private fun Long.hourOfDay(): Int =
    Calendar.getInstance().apply { timeInMillis = this@hourOfDay }.get(Calendar.HOUR_OF_DAY)

@Preview
@Composable
private fun ShiftScreenPreview() {
    val now = System.currentTimeMillis()
    val hour = nightHour(now.hourOfDay())
    NightCleanTheme {
        ShiftScreen(
            zoneId = "G14",
            uiState = ShiftUiState.Success(
                listOf(
                    AdSlot("[가게명 1]", hour, hour + 1, "등판 조끼 · 수거통 옆면"),
                    AdSlot("[가게명 2]", hour + 1, hour + 2, "등판 조끼 · 수거통 옆면"),
                ),
            ),
            startedAt = now - 84 * 60_000,
            now = now,
            beforePhotos = emptyList(),
            isTracking = false,
            permissionDenied = false,
            onRetry = {}, onStartTracking = {}, onAddBeforePhoto = {}, onRemoveBeforePhoto = {}, onCheckin = {}, onFinish = {},
        )
    }
}
