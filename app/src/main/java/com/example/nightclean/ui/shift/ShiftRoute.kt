package com.example.nightclean.ui.shift

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
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
    onFinish: (shiftId: String, startedAt: Long, beforePhotos: List<String>, bagsByHour: List<Int>) -> Unit,
    viewModel: ShiftViewModel = hiltViewModel(),
) {
    // 체크인 화면에서 돌아올 때마다 체크인 수를 새로 셈
    LifecycleResumeEffect(Unit) {
        viewModel.refreshCheckins()
        onPauseOrDispose {}
    }
    val checkinCount by viewModel.checkinCount.collectAsStateWithLifecycle()
    val bagLog by viewModel.bagLog.collectAsStateWithLifecycle()
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
        checkinCount = checkinCount,
        bagCount = bagLog.size,
        onAddBag = { viewModel.addBag(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) },
        onUndoBag = viewModel::undoBag,
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
        onFinish = { onFinish(viewModel.shiftId, viewModel.startedAt, beforePhotos, viewModel.bagsByHour()) },
        // 전화 앱에 번호만 채워 열기(바로 걸지 않아 오작동 통화 방지, 전화 권한 불필요)
        onDial = { number -> context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) },
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
    checkinCount: Int,
    bagCount: Int,
    onAddBag: () -> Unit,
    onUndoBag: () -> Unit,
    onRetry: () -> Unit,
    onStartTracking: () -> Unit,
    onAddBeforePhoto: (String) -> Unit,
    onRemoveBeforePhoto: (Int) -> Unit,
    onCheckin: () -> Unit,
    onFinish: () -> Unit,
    onDial: (number: String) -> Unit,
) {
    var showEmergency by rememberSaveable { mutableStateOf(false) }
    if (showEmergency) {
        EmergencyDialog(
            onDial = { number ->
                showEmergency = false
                onDial(number)
            },
            onDismiss = { showEmergency = false },
        )
    }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("구역 $zoneId", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // 위급할 때 스크롤 없이 바로 보이도록 맨 위에 둠
                    OutlinedButton(
                        onClick = { showEmergency = true },
                        border = BorderStroke(1.dp, Warning),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Warning),
                        modifier = Modifier.height(48.dp),
                    ) { Text("긴급 연락", fontWeight = FontWeight.Bold) }
                }
            }
            ShiftTimer(startedAt, now)
            if (!isTracking) TrackingConsent(permissionDenied, onStartTracking)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountCard("가게 체크인", "${checkinCount}곳", Modifier.weight(1f))
                CountCard("채운 봉투", "${bagCount}장", Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onUndoBag,
                            enabled = bagCount > 0,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(56.dp).semantics { contentDescription = "봉투 하나 되돌리기" },
                        ) { Text("−") }
                        Button(
                            onClick = onAddBag,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(56.dp).semantics { contentDescription = "봉투 하나 채움" },
                        ) { Text("+1", fontWeight = FontWeight.Black) }
                    }
                }
            }
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

// shortcut: 112·119만 둠, 운영팀(크루장) 연락처가 정해지면 목록에 추가
@Composable
private fun EmergencyDialog(onDial: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("긴급 연락") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("번호를 고르면 전화 앱이 열려요. 통화 버튼을 눌러야 걸려요.")
                Button(onClick = { onDial("112") }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("112 경찰 신고", fontWeight = FontWeight.Bold)
                }
                Button(onClick = { onDial("119") }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("119 화재·구급", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

/** 시안의 체크인 수·봉투 수 카드. 봉투는 채울 때마다 지금 시간대 칸에 1장씩 */
@Composable
private fun CountCard(label: String, value: String, modifier: Modifier, actions: (@Composable () -> Unit)? = null) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        actions?.invoke()
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
            checkinCount = 2,
            bagCount = 3,
            onAddBag = {}, onUndoBag = {},
            onRetry = {}, onStartTracking = {}, onAddBeforePhoto = {}, onRemoveBeforePhoto = {}, onCheckin = {}, onFinish = {},
            onDial = {},
        )
    }
}
