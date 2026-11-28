package io.github.koolunkle.alleyglow.ui.zonemap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.domain.model.NIGHT_SLOT_HOURS
import io.github.koolunkle.alleyglow.domain.model.Zone
import io.github.koolunkle.alleyglow.ui.checkin.formatDistance
import io.github.koolunkle.alleyglow.ui.common.LoadFailedNotice
import io.github.koolunkle.alleyglow.ui.common.AppTopBar
import io.github.koolunkle.alleyglow.ui.theme.AlleyGlowTheme
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import io.github.koolunkle.alleyglow.ui.theme.TouchHeight
import java.util.Locale
import kotlin.math.roundToInt
import io.github.koolunkle.alleyglow.ui.theme.cardSurface

// shortcut: 이태원역이 있는 칸 고정(analysis/zones.py의 STATION 칸), 분석 범위가 바뀌면 zones에 표식 열 추가
private const val STATION_ZONE_ID = "G20"
private const val COLUMNS = 6

/** 상태 보유 진입점. ViewModel 연결 후 상태와 이벤트를 Screen에 전달 */
@Composable
fun ZoneMapRoute(
    onStartShift: (zoneId: String, zoneName: String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: ZoneMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val workerName by viewModel.workerName.collectAsStateWithLifecycle()
    ZoneMapScreen(
        uiState = uiState,
        workerName = workerName,
        onSelectZone = viewModel::selectZone,
        onSelectSlot = viewModel::selectSlot,
        onRetry = viewModel::load,
        onStartShift = onStartShift,
        onSignOut = onSignOut,
    )
}

/** 상태 없는 화면. Preview·UI 테스트 대상 */
@Composable
fun ZoneMapScreen(
    uiState: ZoneMapUiState,
    workerName: String?,
    onSelectZone: (String) -> Unit,
    onSelectSlot: (Int?) -> Unit,
    onRetry: () -> Unit,
    onStartShift: (zoneId: String, zoneName: String) -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold(topBar = { AppTopBar(stringResource(R.string.role_worker), workerName, onSignOut) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.l, end = Spacing.l, top = Spacing.s, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Text(stringResource(R.string.zonemap_title), style = MaterialTheme.typography.headlineSmall)
            when (uiState) {
                ZoneMapUiState.Loading -> Box(Modifier.fillMaxWidth().aspectRatio(1f), Alignment.Center) {
                    CircularProgressIndicator()
                }
                ZoneMapUiState.Error -> LoadFailedNotice(stringResource(R.string.zonemap_load_error), onRetry)
                is ZoneMapUiState.Success -> {
                    val slot = uiState.slot
                    val ranks = remember(uiState.zones, slot) {
                        uiState.zones.sortedByDescending { it.scoreAt(slot) }.mapIndexed { i, z -> z.id to i + 1 }.toMap()
                    }
                    // 시간대 값이 없는 데이터(열 추가 전)면 칩 없이 평균만
                    if (uiState.zones.all { it.scoreByHour.size == NIGHT_SLOT_HOURS.size }) SlotChips(slot, onSelectSlot)
                    MapCard(uiState.zones, ranks, slot, uiState.selectedZoneId, onSelectZone)
                    uiState.selectedZone?.let { zone ->
                        ZoneCard(zone, ranks.getValue(zone.id), uiState.zones.size, slot, onStart = { onStartShift(zone.id, zone.name) })
                    } ?: Text(stringResource(R.string.zonemap_select_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun cellColor(accent: Color, score: Float) = accent.copy(alpha = 0.08f + score * 0.82f)

/** 평균과 한 시간 단위 시간대. 고른 칩은 흰 바탕(연두는 '지금 누를 것'에만) */
@Composable
private fun SlotChips(slot: Int?, onSelect: (Int?) -> Unit) {
    val colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.onSurface,
        selectedLabelColor = MaterialTheme.colorScheme.surface,
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        (listOf(null) + NIGHT_SLOT_HOURS.indices).forEach { s ->
            val label = s?.let { stringResource(R.string.slot_hours, NIGHT_SLOT_HOURS[it], NIGHT_SLOT_HOURS[it] + 1) }
                ?: stringResource(R.string.zonemap_slot_all)
            FilterChip(
                selected = s == slot,
                onClick = { onSelect(s) },
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                shape = CircleShape,
                colors = colors,
                border = null,
            )
        }
    }
}

/** 격자와 범례를 한 카드에. 북→남, 서→동 순(grid_id 정렬), 상위 3칸 순위와 이태원역 표시 */
@Composable
private fun MapCard(zones: List<Zone>, ranks: Map<String, Int>, slot: Int?, selectedZoneId: String?, onSelectZone: (String) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    // 실제 점수 폭(0.07~0.70 등)에 맞춰 색을 펼침. 범례도 '상대 비교'
    val lo = zones.minOf { it.scoreAt(slot) }
    val span = (zones.maxOf { it.scoreAt(slot) } - lo).takeIf { it > 0f } ?: 1f
    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface()
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
                                .background(cellColor(accent, (zone.scoreAt(slot) - lo) / span), shape)
                                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, shape) else Modifier)
                                .semantics { this.selected = selected }
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = stringResource(R.string.zonemap_select_action, zone.name),
                                ) { onSelectZone(zone.id) },
                        ) {
                            // 역은 칸 정중앙(위치), 순위는 오른쪽 위 배지. 자리가 달라 한 칸에 둘이어도 안 겹침
                            if (zone.id == STATION_ZONE_ID) StationPin(Modifier.align(Alignment.Center))
                            if (rank <= 3) Badge(rank.toString(), Modifier.align(Alignment.TopEnd).padding(Spacing.xs))
                        }
                    }
                }
            }
        }
        Legend()
        Source()
    }
}

/** 이태원역 표시(칸 안과 범례 공통) */
@Composable
private fun StationPin(modifier: Modifier = Modifier) {
    Box(modifier.size(22.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape), Alignment.Center) {
        Icon(painterResource(R.drawable.ic_place), contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(15.dp))
    }
}

/** 출처 한 줄(필수 표기). 누르면 데이터셋 전체 */
@Composable
private fun Source() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Text(
        stringResource(if (expanded) R.string.zonemap_source else R.string.zonemap_source_short),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.zonemap_source_toggle)) { expanded = !expanded },
    )
}

@Composable
private fun Badge(label: String, modifier: Modifier, container: Color = MaterialTheme.colorScheme.surface, content: Color = MaterialTheme.colorScheme.onSurface) {
    Box(modifier.size(22.dp).background(container, CircleShape), Alignment.Center) {
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
    // 격자 칸과 같은 모양의 견본 5단계. 순위 숫자는 칸 안에서 바로 읽혀 범례에서 뺌
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(R.string.zonemap_legend_title), style = MaterialTheme.typography.labelMedium, color = muted)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.zonemap_legend_low), style = small, color = muted)
            repeat(5) { i ->
                Box(Modifier.size(14.dp).background(cellColor(accent, i / 4f), RoundedCornerShape(4.dp)))
            }
            Text(stringResource(R.string.zonemap_legend_high), style = small, color = muted)
            Box(Modifier.weight(1f))
            StationPin()
            Text(stringResource(R.string.zonemap_station), style = small, color = muted)
        }
    }
}

/** 고른 구역의 추천 근거(공공데이터 지표 3개)와 근무 시작 */
@Composable
private fun ZoneCard(zone: Zone, rank: Int, total: Int, slot: Int?, onStart: () -> Unit) {
    var showInfo by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface()
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
                    stringResource(R.string.zonemap_score, (zone.scoreAt(slot) * 100).roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    Modifier
                        .size(32.dp)
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.zonemap_score_info_desc)) { showInfo = !showInfo },
                    Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (showInfo) {
                Text(
                    stringResource(R.string.zonemap_score_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            zone.nightPopAt(slot)?.let { pop ->
                Fact(
                    stringResource(R.string.zonemap_fact_pop),
                    stringResource(R.string.zonemap_fact_pop_value, "%,d".format(Locale.US, pop)),
                    zone.weekdayPopAt(slot)?.let { formatChange(pop, it) }?.let { stringResource(R.string.zonemap_fact_pop_vs_weekday, it) },
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
    AlleyGlowTheme {
        ZoneMapScreen(
            uiState = ZoneMapUiState.Success(
                zones = List(36) {
                    Zone(
                        id = "G%02d".format(it), name = "격자 ${it + 1}", score = (it % 5) / 4f, 5523, 3446, 31, 4_493_365_069,
                        scoreByHour = List(4) { h -> ((it + h) % 5) / 4f }, nightPopByHour = List(4) { 5000 }, weekdayPopByHour = List(4) { 3500 },
                    )
                },
                selectedZoneId = "G19",
                slot = 1,
            ),
            workerName = "이태원 1조",
            onSelectZone = {}, onSelectSlot = {}, onRetry = {}, onStartShift = { _, _ -> }, onSignOut = {},
        )
    }
}
