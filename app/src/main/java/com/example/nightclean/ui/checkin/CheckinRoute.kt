package com.example.nightclean.ui.checkin

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nightclean.R
import com.example.nightclean.domain.model.CheckinResult
import com.example.nightclean.ui.common.SubTopBar
import com.example.nightclean.ui.theme.NightCleanTheme
import com.example.nightclean.ui.theme.Spacing
import com.example.nightclean.ui.theme.TouchHeight
import com.example.nightclean.ui.theme.Warning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CheckinRoute(
    onDone: () -> Unit,
    viewModel: CheckinViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Google Code Scanner: 카메라 권한·스캔 화면을 Play 서비스가 맡음
    val scanner = remember(context) {
        GmsBarcodeScanning.getClient(
            context,
            GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
    }
    val scan = {
        scanner.startScan()
            .addOnSuccessListener { barcode -> barcode.rawValue?.let(viewModel::checkIn) }
            .addOnFailureListener { viewModel.onScannerFailed() }
        Unit
    }
    // 대략적 위치만 허용하면 50m 판정이 안 되므로 정확한 위치를 요구
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) scan() else viewModel.onPermissionDenied()
    }
    val startScan = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            scan()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    // 근무 화면에서 'QR 체크인'을 눌러 들어왔으니 스캐너를 바로 엶(화면 재생성 때는 다시 열지 않음)
    var autoScanned by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoScanned) {
            autoScanned = true
            startScan()
        }
    }
    CheckinScreen(uiState = uiState, onScan = startScan, onDone = onDone)
}

@Composable
fun CheckinScreen(uiState: CheckinUiState, onScan: () -> Unit, onDone: () -> Unit) {
    val succeeded = uiState is CheckinUiState.Done && uiState.result is CheckinResult.CheckedIn
    Scaffold(
        topBar = { SubTopBar(stringResource(R.string.checkin_title), onDone) },
        bottomBar = {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.l, vertical = Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                // 성공하면 근무로 돌아가는 게 다음 동작, 그 밖에는 다시 스캔
                if (succeeded) {
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(TouchHeight)) {
                        Text(stringResource(R.string.checkin_back_to_shift), style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(onClick = onScan, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text(stringResource(R.string.checkin_scan_another), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Button(
                        onClick = onScan,
                        enabled = uiState != CheckinUiState.Working,
                        modifier = Modifier.fillMaxWidth().height(TouchHeight),
                    ) {
                        Text(
                            stringResource(if (uiState is CheckinUiState.Idle) R.string.checkin_scan else R.string.checkin_rescan),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text(stringResource(R.string.checkin_back_to_shift), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
    ) { padding ->
        // 정중앙보다 조금 위(아래 버튼과 거리를 둠)
        Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.l), BiasAlignment(0f, -0.3f)) {
            when (uiState) {
                CheckinUiState.Idle -> StatusHero(
                    icon = R.drawable.ic_qr_scan,
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurface,
                    title = stringResource(R.string.checkin_idle_title),
                    caption = stringResource(R.string.checkin_idle_caption),
                )
                CheckinUiState.Working -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.l),
                ) {
                    CircularProgressIndicator(Modifier.size(48.dp))
                    Text(stringResource(R.string.checkin_working), style = MaterialTheme.typography.titleMedium)
                }
                is CheckinUiState.Done -> ResultHero(uiState.result, uiState.checkedAt, uiState.count)
                is CheckinUiState.Error -> StatusHero(
                    icon = R.drawable.ic_warning,
                    container = Warning.copy(alpha = 0.16f),
                    content = Warning,
                    title = stringResource(R.string.checkin_error_title),
                    caption = stringResource(
                        when (uiState.reason) {
                            CheckinError.LOCATION -> R.string.checkin_error_location
                            CheckinError.NETWORK -> R.string.checkin_error_network
                            CheckinError.PERMISSION -> R.string.checkin_error_permission
                            CheckinError.SCANNER -> R.string.checkin_error_scanner
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun ResultHero(result: CheckinResult, checkedAt: Long, count: Int?) {
    when (result) {
        is CheckinResult.CheckedIn -> StatusHero(
            icon = R.drawable.ic_check,
            container = MaterialTheme.colorScheme.primary,
            content = MaterialTheme.colorScheme.onPrimary,
            label = stringResource(R.string.checkin_done_title),
            title = result.shopName,
            caption = stringResource(
                R.string.checkin_done_caption,
                formatDistance(result.distanceM),
                SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(checkedAt)),
            ),
            detail = count?.let { stringResource(R.string.checkin_done_count, it) },
        )
        is CheckinResult.TooFar -> StatusHero(
            icon = R.drawable.ic_place,
            container = Warning.copy(alpha = 0.16f),
            content = Warning,
            overline = result.shopName,
            title = stringResource(R.string.checkin_too_far_title, formatDistance(result.distanceM)),
            caption = stringResource(R.string.checkin_too_far_caption),
        )
        is CheckinResult.UnknownShop -> StatusHero(
            icon = R.drawable.ic_warning,
            container = Warning.copy(alpha = 0.16f),
            content = Warning,
            title = stringResource(R.string.checkin_unknown_title),
            caption = stringResource(R.string.checkin_unknown_caption),
            detail = stringResource(R.string.checkin_unknown_value, result.shopCode.take(40)),
        )
    }
}

/** 상태 하나를 큰 원 아이콘 + (가게명) + 제목 + 한 줄 설명으로. 모든 결과가 같은 틀 */
@Composable
private fun StatusHero(
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    title: String,
    caption: String,
    overline: String? = null,
    /** 제목 위 작은 연두 라벨(완료 표시) */
    label: String? = null,
    detail: String? = null,
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Box(Modifier.size(112.dp).background(container, CircleShape), Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(56.dp))
        }
        Box(Modifier.height(Spacing.s))
        overline?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        label?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).padding(horizontal = Spacing.m, vertical = 2.dp),
            )
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        detail?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        }
    }
}

/** 1km 미만은 m, 이상은 소수 한 자리 km(18476 → 18.5km) */
internal fun formatDistance(meters: Int): String =
    if (meters < 1000) "${meters}m" else "%.1fkm".format(Locale.US, meters / 1000.0)

@Preview
@Composable
private fun CheckinScreenPreview() {
    NightCleanTheme {
        CheckinScreen(
            uiState = CheckinUiState.Done(CheckinResult.CheckedIn("[가게명]", 32), System.currentTimeMillis(), count = 3),
            onScan = {}, onDone = {},
        )
    }
}
