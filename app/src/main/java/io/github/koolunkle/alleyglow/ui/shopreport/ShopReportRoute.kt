package io.github.koolunkle.alleyglow.ui.shopreport

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.domain.model.NightRecord
import io.github.koolunkle.alleyglow.domain.model.ShopReport
import io.github.koolunkle.alleyglow.ui.common.LoadFailedNotice
import io.github.koolunkle.alleyglow.ui.common.AppTopBar
import io.github.koolunkle.alleyglow.ui.common.InlineNotice
import io.github.koolunkle.alleyglow.ui.theme.AlleyGlowTheme
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import io.github.koolunkle.alleyglow.ui.theme.TouchHeight
import java.text.SimpleDateFormat
import java.util.Locale
import io.github.koolunkle.alleyglow.ui.theme.cardSurface

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
    val shopName = (uiState as? ShopReportUiState.Success)?.report?.shopName
    Scaffold(topBar = { AppTopBar(stringResource(R.string.role_shop), shopName, onSignOut) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.l, end = Spacing.l, top = Spacing.s, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            when (uiState) {
                ShopReportUiState.Loading -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                ShopReportUiState.NoShop -> Text(stringResource(R.string.report_no_shop))
                ShopReportUiState.Error -> LoadFailedNotice(stringResource(R.string.report_load_error), onRetry)
                is ShopReportUiState.Success -> Report(uiState, onAddGuest, loadPhoto)
            }
        }
    }
}

@Composable
private fun Report(uiState: ShopReportUiState.Success, onAddGuest: () -> Unit, loadPhoto: suspend (String) -> ByteArray?) {
    val report = uiState.report
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            stringResource(R.string.report_owner, report.shopName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.report_title, report.month), style = MaterialTheme.typography.headlineSmall)
    }
    // 2×2 지표. 줄마다 높이를 맞춤
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Metric(stringResource(R.string.report_nights), stringResource(R.string.report_nights_value, report.nights), stringResource(R.string.report_nights_caption))
        Metric(stringResource(R.string.report_bags), stringResource(R.string.report_bags_value, report.collectedBags), stringResource(R.string.report_bags_caption))
    }
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Metric(stringResource(R.string.report_scans), stringResource(R.string.report_nights_value, report.qrScans), stringResource(R.string.report_scans_caption))
        Metric(
            stringResource(R.string.report_clean),
            report.cleanScore?.let { stringResource(R.string.report_clean_value, "%.1f".format(Locale.US, it)) } ?: "-",
            stringResource(R.string.report_clean_caption),
        )
    }
    GuestCard(report.guests, uiState.isAddingGuest, uiState.guestFailed, onAddGuest)
    RecentNights(report.recent, loadPhoto)
}

/** 지표 한 칸: 이름, 큰 값, 짧은 기준 설명 */
@Composable
private fun RowScope.Metric(label: String, value: String, caption: String) {
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .cardSurface()
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 손님 수와 +1(연두는 누를 버튼에만) */
@Composable
private fun GuestCard(guests: Int, isAdding: Boolean, failed: Boolean, onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface()
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.report_guests), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.report_guests_value, guests), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.report_guests_caption), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onAdd, enabled = !isAdding, modifier = Modifier.height(TouchHeight)) {
                if (isAdding) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.report_guests_add), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (failed) InlineNotice(stringResource(R.string.report_guests_failed), MaterialTheme.colorScheme.background)
    }
}

/** 최근 청소한 밤. 한 카드 안에서 구분선으로 나눔 */
@Composable
private fun RecentNights(nights: List<NightRecord>, loadPhoto: suspend (String) -> ByteArray?) {
    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface()
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Text(stringResource(R.string.report_recent), style = MaterialTheme.typography.titleMedium)
        if (nights.isEmpty()) {
            Text(stringResource(R.string.report_recent_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        nights.forEachIndexed { i, night ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            NightRow(night, loadPhoto)
        }
    }
}

@Composable
private fun NightRow(night: NightRecord, loadPhoto: suspend (String) -> ByteArray?) {
    val time = if (night.firstAt == night.lastAt) night.firstAt else "${night.firstAt}–${night.lastAt}"
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(formatNight(night.night), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.report_night_detail, time, night.checkins),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // 전·후 썸네일. 누르면 크게 보기, 사진이 없는 밤은 생략
        if (night.beforePhoto != null || night.afterPhoto != null) {
            var viewing by rememberSaveable { mutableStateOf(false) }
            if (viewing) NightPhotoViewer(night, loadPhoto, onDismiss = { viewing = false })
            Row(
                Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.report_photo_open)) { viewing = true },
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                NightPhoto(night.beforePhoto, stringResource(R.string.report_photo_before), loadPhoto)
                NightPhoto(night.afterPhoto, stringResource(R.string.report_photo_after), loadPhoto)
            }
        }
    }
}

/** 56dp 썸네일. 원본(긴 변 1600px, 업로드 때 방향 보정됨)을 작게 디코딩 */
@Composable
private fun NightPhoto(path: String?, label: String, loadPhoto: suspend (String) -> ByteArray?) {
    val image by rememberPhoto(path, sampleSize = 8, loadPhoto)
    Box(
        Modifier.size(56.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.BottomCenter,
    ) {
        image?.let { Image(it, contentDescription = stringResource(R.string.report_photo_desc, label), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)).padding(vertical = 1.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** 2026-11-29 → 11/29 (일). 형식이 다르면 원문 */
internal fun formatNight(night: String): String =
    runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(night) }.getOrNull()
        ?.let { SimpleDateFormat("M/d (E)", Locale.KOREA).format(it) }
        ?: night

@Preview
@Composable
private fun ShopReportScreenPreview() {
    AlleyGlowTheme {
        ShopReportScreen(
            uiState = ShopReportUiState.Success(
                ShopReport(
                    shopId = 1, shopName = "[가게명]", month = 11, nights = 4, collectedBags = 24, guests = 7,
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
