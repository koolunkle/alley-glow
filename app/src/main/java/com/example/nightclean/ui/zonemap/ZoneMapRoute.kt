package com.example.nightclean.ui.zonemap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nightclean.R
import com.example.nightclean.domain.model.Zone
import com.example.nightclean.ui.checkin.formatDistance
import com.example.nightclean.ui.common.AppTopBar
import com.example.nightclean.ui.theme.NightCleanTheme
import com.example.nightclean.ui.theme.Spacing
import com.example.nightclean.ui.theme.TouchHeight
import java.util.Locale
import kotlin.math.roundToInt

// shortcut: 이태원역이 있는 칸 고정(analysis/zones.py의 STATION 칸), 분석 범위가 바뀌면 zones에 표식 열 추가
private const val STATION_ZONE_ID = "G20"
private const val COLUMNS = 6

/** 상태 보유 진입점. ViewModel 연결 후 상태와 이벤트를 Screen에 전달 */
@Composable
fun ZoneMapRoute(
    onStartShift: (zoneId: String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: ZoneMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val workerName by viewModel.workerName.collectAsStateWithLifecycle()
    ZoneMapScreen(
        uiState = uiState,
        account = workerName ?: stringResource(R.string.role_worker),
        onSelectZone = viewModel::selectZone,
        onRetry = viewModel::load,
        onStartShift = onStartShift,
        onSignOut = onSignOut,
    )
}

/** 상태 없는 화면. Preview·UI 테스트 대상 */
@Composable
fun ZoneMapScreen(
    uiState: ZoneMapUiState,
    account: String,
    onSelectZone: (String) -> Unit,
    onRetry: () -> Unit,
    onStartShift: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold(topBar = { AppTopBar(account, onSignOut) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.l, end = Spacing.l, top = Spacing.s, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(stringResource(R.string.zonemap_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.zonemap_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (uiState) {
                ZoneMapUiState.Loading -> Box(Modifier.fillMaxWidth().aspectRatio(1f), Alignment.Center) {
                    CircularProgressIndicator()
                }
                ZoneMapUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(stringResource(R.string.zonemap_load_error))
                    Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
                is ZoneMapUiState.Success -> {
                    val ranks = remember(uiState.zones) {
                        uiState.zones.sortedByDescending { it.score }.mapIndexed { i, z -> z.id to i + 1 }.toMap()
                    }
                    MapCard(uiState.zones, ranks, uiState.selectedZoneId, onSelectZone)
                    uiState.selectedZone?.let { zone ->
                        ZoneCard(zone, ranks.getValue(zone.id), uiState.zones.size, onStart = { onStartShift(zone.id) })
                    } ?: Text(stringResource(R.string.zonemap_select_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                stringResource(R.string.zonemap_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun cellColor(accent: Color, score: Float) = accent.copy(alpha = 0.08f + score * 0.82f)

/** 격자와 범례를 한 카드에. 북→남, 서→동 순(grid_id 정렬), 상위 3칸 순위와 이태원역 표시 */
@Composable
private fun MapCard(zones: List<Zone>, ranks: Map<String, Int>, selectedZoneId: String?, onSelectZone: (String) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    // 실제 점수 폭(0.07~0.70 등)에 맞춰 색을 펼침. 범례도 '상대 비교'
    val lo = zones.minOf { it.score }
    val span = (zones.maxOf { it.score } - lo).takeIf { it > 0f } ?: 1f
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            zones.chunked(COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    row.forEach { zone ->
                        val shape = RoundedCornerShape(10.dp)
                        val rank = ranks.getValue(zone.id)
                        val selected = zone.id == selectedZoneId
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(cellColor(accent, (zone.score - lo) / span), shape)
                                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, shape) else Modifier)
                                .semantics { this.selected = selected }
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = stringResource(R.string.zonemap_select_action, zone.name),
                                ) { onSelectZone(zone.id) },
                        ) {
                            if (rank <= 3) Badge(rank.toString(), Modifier.align(Alignment.TopEnd))
                            if (zone.id == STATION_ZONE_ID) {
                                Badge(
                                    stringResource(R.string.zonemap_station_badge),
                                    Modifier.align(Alignment.TopStart),
                                    MaterialTheme.colorScheme.onSurface,
                                    MaterialTheme.colorScheme.surface,
                                )
                            }
                        }
                    }
                }
            }
        }
        Legend()
    }
}

@Composable
private fun Badge(label: String, modifier: Modifier, container: Color = MaterialTheme.colorScheme.surface, content: Color = MaterialTheme.colorScheme.onSurface) {
    Box(modifier.padding(Spacing.xs).size(20.dp).background(container, CircleShape), Alignment.Center) {
        // 한글 글꼴의 위아래 여백을 잘라 원 정중앙에 맞춤
        Text(
            label,
            color = content,
            style = MaterialTheme.typography.labelSmall.copy(
                lineHeight = 11.sp,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        )
    }
}

@Composable
private fun Legend() {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val small = MaterialTheme.typography.bodySmall
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.zonemap_legend_low), style = small, color = muted)
        Box(
            Modifier
                .width(56.dp)
                .height(8.dp)
                .background(Brush.horizontalGradient(listOf(cellColor(accent, 0f), cellColor(accent, 1f))), CircleShape),
        )
        Text(stringResource(R.string.zonemap_legend_high), style = small, color = muted)
        Box(Modifier.weight(1f))
        Badge("1", Modifier, MaterialTheme.colorScheme.background)
        Text(stringResource(R.string.zonemap_legend_rank), style = small, color = muted)
        Badge(stringResource(R.string.zonemap_station_badge), Modifier, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface)
        Text(stringResource(R.string.zonemap_station), style = small, color = muted)
    }
}

/** 고른 구역의 추천 근거(공공데이터 지표 3개)와 근무 시작 */
@Composable
private fun ZoneCard(zone: Zone, rank: Int, total: Int, onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(zone.name, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.zonemap_rank, rank, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(horizontal = Spacing.s, vertical = 2.dp),
                )
                Text(
                    stringResource(R.string.zonemap_score, "%.2f".format(Locale.US, zone.score)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            zone.nightPop?.let { pop ->
                Fact(
                    stringResource(R.string.zonemap_fact_pop),
                    stringResource(R.string.zonemap_fact_pop_value, "%,d".format(Locale.US, pop)),
                    zone.weekdayPop?.let { formatChange(pop, it) }?.let { stringResource(R.string.zonemap_fact_pop_vs_weekday, it) },
                )
            }
            zone.binDistM?.let {
                Fact(stringResource(R.string.zonemap_fact_bin), formatDistance(it), stringResource(R.string.zonemap_fact_bin_caption))
            }
            zone.nightSales?.let {
                Fact(stringResource(R.string.zonemap_fact_sales), formatWon(it), stringResource(R.string.zonemap_fact_sales_caption))
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(TouchHeight)) {
            Text(stringResource(R.string.zonemap_start), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** 지표 한 칸: 이름, 값, 보조 설명. 세 칸 높이를 맞춤 */
@Composable
private fun RowScope.Fact(label: String, value: String, caption: String?) {
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
        caption?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** 1억 이상은 억(10억 미만은 소수 한 자리), 그 아래는 만 원 단위 */
internal fun formatWon(won: Long): String = when {
    won >= 1_000_000_000 -> "%,d억 원".format(Locale.US, Math.round(won / 100_000_000.0))
    won >= 100_000_000 -> "%.1f억 원".format(Locale.US, won / 100_000_000.0)
    else -> "%,d만 원".format(Locale.US, Math.round(won / 10_000.0))
}

/** 평일 대비 증감률(+60%). 평일 값이 0이면 null */
internal fun formatChange(value: Int, base: Int): String? =
    if (base <= 0) null else "%+d%%".format(Locale.US, ((value - base) * 100.0 / base).roundToInt())

@Preview
@Composable
private fun ZoneMapScreenPreview() {
    NightCleanTheme {
        ZoneMapScreen(
            uiState = ZoneMapUiState.Success(
                zones = List(36) { Zone(id = "G%02d".format(it), name = "격자 ${it + 1}", score = (it % 5) / 4f, 5523, 3446, 31, 4_493_365_069) },
                selectedZoneId = "G19",
            ),
            account = "데모 워커",
            onSelectZone = {}, onRetry = {}, onStartShift = {}, onSignOut = {},
        )
    }
}
