package io.github.koolunkle.alleyglow.ui.shift

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.domain.model.AdSlot
import io.github.koolunkle.alleyglow.domain.model.MAX_BEFORE_AFTER_PHOTOS
import io.github.koolunkle.alleyglow.domain.model.currentAt
import io.github.koolunkle.alleyglow.domain.model.nextAfter
import io.github.koolunkle.alleyglow.domain.model.nightHour
import io.github.koolunkle.alleyglow.ui.common.LoadFailedNotice
import io.github.koolunkle.alleyglow.ui.common.InlineNotice
import io.github.koolunkle.alleyglow.ui.common.PhotoRow
import io.github.koolunkle.alleyglow.ui.common.SectionCard
import io.github.koolunkle.alleyglow.ui.theme.AlleyGlowTheme
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import io.github.koolunkle.alleyglow.ui.theme.TouchHeight
import io.github.koolunkle.alleyglow.ui.theme.Warning
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ShiftRoute(
    zoneId: String,
    zoneName: String,
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
        zoneName = zoneName,
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
    zoneName: String,
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
        // 근무 종료는 마지막에 꼭 거쳐야 하는 단계라 연두로 아래에 고정
        bottomBar = {
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.l, vertical = Spacing.m),
            ) {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(TouchHeight)) {
                    Text(stringResource(R.string.shift_finish), style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                StatusChip(isTracking)
                // 위급할 때 스크롤 없이 바로 보이도록 맨 위에 둠
                EmergencyChip(onClick = { showEmergency = true })
            }
            ShiftTimer(startedAt, now, zoneName)
            if (!isTracking) TrackingConsent(permissionDenied, onStartTracking)
            // 숫자와 그 숫자를 바꾸는 버튼을 한 카드에. 두 카드 높이를 맞춤
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                CounterCard(stringResource(R.string.shift_checkins), stringResource(R.string.shift_checkins_value, checkinCount)) {
                    Button(
                        onClick = onCheckin,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                        ),
                        contentPadding = PaddingValues(horizontal = Spacing.s),
                        modifier = Modifier.fillMaxWidth().height(TouchHeight),
                    ) { Text(stringResource(R.string.shift_checkin), style = MaterialTheme.typography.labelLarge) }
                }
                CounterCard(stringResource(R.string.shift_bags), stringResource(R.string.shift_bags_value, bagCount)) {
                    BagButtons(bagCount, onAddBag, onUndoBag)
                }
            }
            when (uiState) {
                ShiftUiState.Loading -> Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator() }
                ShiftUiState.Error -> LoadFailedNotice(stringResource(R.string.shift_slots_error), onRetry)
                is ShiftUiState.Success -> {
                    val hour = nightHour(now.hourOfDay())
                    SlotCard(uiState.slots.currentAt(hour), uiState.slots.nextAfter(hour))
                }
            }
            SectionCard(
                stringResource(R.string.shift_before_photos),
                required = false,
                hint = stringResource(R.string.shift_before_photos_caption),
            ) {
                PhotoRow(beforePhotos, MAX_BEFORE_AFTER_PHOTOS, stringResource(R.string.shift_before_photos), enabled = true, onAddBeforePhoto, onRemoveBeforePhoto)
            }
        }
    }
}

/** 근무 상태(연두 점). GPS 기록 중이면 문구에 표시 */
@Composable
private fun StatusChip(isTracking: Boolean) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Text(
            stringResource(if (isTracking) R.string.shift_status_tracking else R.string.shift_status),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** 긴급 연락. 옅은 주황 칩(터치 영역은 48dp 보장) */
@Composable
private fun EmergencyChip(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Warning.copy(alpha = 0.14f), contentColor = Warning) {
        Row(
            Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_call), contentDescription = null, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.shift_emergency), style = MaterialTheme.typography.labelMedium)
        }
    }
}

// shortcut: 112·119만 둠, 운영팀(크루장) 연락처가 정해지면 목록에 추가
@Composable
private fun EmergencyDialog(onDial: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shift_emergency)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(stringResource(R.string.emergency_body))
                Button(onClick = { onDial("112") }, modifier = Modifier.fillMaxWidth().height(TouchHeight)) {
                    Text(stringResource(R.string.emergency_police), style = MaterialTheme.typography.labelLarge)
                }
                Button(onClick = { onDial("119") }, modifier = Modifier.fillMaxWidth().height(TouchHeight)) {
                    Text(stringResource(R.string.emergency_fire), style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** 이름, 큰 숫자, 아래쪽 버튼 줄 */
@Composable
private fun RowScope.CounterCard(label: String, value: String, actions: @Composable () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(Spacing.l),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.weight(1f).height(Spacing.l))
        actions()
    }
}

/** 봉투를 채울 때마다 +1(누른 시각의 시간대 칸에 1장). 두 버튼 같은 높이·모양 */
@Composable
private fun BagButtons(bagCount: Int, onAdd: () -> Unit, onUndo: () -> Unit) {
    val undoDesc = stringResource(R.string.shift_bag_undo_desc)
    val addDesc = stringResource(R.string.shift_bag_add_desc)
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        FilledTonalButton(
            onClick = onUndo,
            enabled = bagCount > 0,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.weight(1f).height(TouchHeight).semantics { contentDescription = undoDesc },
        ) { Text("−1", style = MaterialTheme.typography.labelLarge) }
        Button(
            onClick = onAdd,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.weight(1f).height(TouchHeight).semantics { contentDescription = addDesc },
        ) { Text("+1", style = MaterialTheme.typography.labelLarge) }
    }
}

/** 위치 수집 동의(메모: 근무 중에만 수집, 동의 화면 1개). 근무마다 눌러야 기록이 시작됨 */
@Composable
private fun TrackingConsent(permissionDenied: Boolean, onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(start = Spacing.l, end = Spacing.m, top = Spacing.m, bottom = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.shift_consent_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.shift_consent_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onStart, contentPadding = PaddingValues(horizontal = Spacing.l), modifier = Modifier.height(48.dp)) {
                Text(stringResource(R.string.shift_consent_start), style = MaterialTheme.typography.labelMedium)
            }
        }
        if (permissionDenied) {
            InlineNotice(stringResource(R.string.shift_consent_denied), MaterialTheme.colorScheme.background)
        }
    }
}

@Composable
private fun ShiftTimer(startedAt: Long, now: Long, zoneName: String) {
    val minutes = ((now - startedAt) / 60_000).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.s), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.shift_elapsed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("%02d:%02d".format(minutes / 60, minutes % 60), style = MaterialTheme.typography.displayMedium)
        Text(
            stringResource(R.string.shift_started, SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(startedAt)), zoneName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 지금 광고(작은 연두 라벨 + 가게명)와 다음 광고 한 줄을 한 카드에 */
@Composable
private fun SlotCard(current: AdSlot?, next: AdSlot?) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        if (current == null && next == null) {
            Text(stringResource(R.string.shift_slots_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        current?.let { slot ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    stringResource(R.string.shift_slot_now, slot.startHour % 24, slot.endHour % 24),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(horizontal = Spacing.s, vertical = 2.dp),
                )
                Text(slot.shopName, style = MaterialTheme.typography.headlineSmall)
                Text(slot.placement, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        next?.let { slot ->
            if (current != null) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.shift_slot_next, slot.startHour % 24, slot.endHour % 24),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(96.dp),
                )
                Text(slot.shopName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun Long.hourOfDay(): Int =
    Calendar.getInstance().apply { timeInMillis = this@hourOfDay }.get(Calendar.HOUR_OF_DAY)

@Preview
@Composable
private fun ShiftScreenPreview() {
    val now = System.currentTimeMillis()
    val hour = nightHour(now.hourOfDay())
    AlleyGlowTheme {
        ShiftScreen(
            zoneName = "이태원 관광특구 4",
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
