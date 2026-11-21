package com.example.nightclean.ui.checkin

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nightclean.domain.model.CheckinResult
import com.example.nightclean.ui.theme.NightCleanTheme
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
    CheckinScreen(
        uiState = uiState,
        onScan = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) {
                scan()
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        },
        onDone = onDone,
    )
}

@Composable
fun CheckinScreen(uiState: CheckinUiState, onScan: () -> Unit, onDone: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("가게 체크인", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            when (uiState) {
                CheckinUiState.Idle -> Text(
                    "가게 앞에서 가게 QR을 스캔하세요. 체크인은 가게 50m 안에서만 인정돼요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CheckinUiState.Working -> Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("위치를 확인하고 있어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is CheckinUiState.Done -> CheckinResultView(uiState.result, uiState.checkedAt)
                is CheckinUiState.Error -> Notice(uiState.message)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onScan,
                enabled = uiState != CheckinUiState.Working,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(if (uiState is CheckinUiState.Idle) "가게 QR 스캔" else "다시 스캔", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("근무 화면으로", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CheckinResultView(result: CheckinResult, checkedAt: Long) {
    when (result) {
        is CheckinResult.CheckedIn -> Column(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(112.dp).background(MaterialTheme.colorScheme.primary, CircleShape).clearAndSetSemantics {},
                Alignment.Center,
            ) {
                Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 56.sp, fontWeight = FontWeight.Black)
            }
            Text(
                "${result.shopName}\n체크인 완료",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                "가게에서 ${formatDistance(result.distanceM)} · ${SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(checkedAt))}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        is CheckinResult.TooFar ->
            Notice("${result.shopName}에서 ${formatDistance(result.distanceM)} 떨어져 있어요. 가게 앞에서 다시 스캔해 주세요.")
        is CheckinResult.UnknownShop -> Notice(
            "등록된 가게 QR이 아니에요. 가게에 붙은 클린 워커 QR을 스캔해 주세요.\n읽은 값: ${result.shopCode.take(40)}",
        )
    }
}

/** 1km 미만은 m, 이상은 소수 한 자리 km(18476 → 18.5km) */
internal fun formatDistance(meters: Int): String =
    if (meters < 1000) "${meters}m" else "%.1fkm".format(Locale.US, meters / 1000.0)

/** 주의 안내(거리 초과, 오류) */
@Composable
private fun Notice(message: String, color: Color = Warning) {
    Text(
        message,
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        color = color,
    )
}

@Preview
@Composable
private fun CheckinScreenPreview() {
    NightCleanTheme {
        CheckinScreen(
            uiState = CheckinUiState.Done(CheckinResult.CheckedIn("[가게명]", 32), System.currentTimeMillis()),
            onScan = {}, onDone = {},
        )
    }
}
