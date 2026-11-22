package com.example.nightclean.ui.zonemap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nightclean.domain.model.Zone
import com.example.nightclean.ui.checkin.formatDistance
import com.example.nightclean.ui.theme.NightCleanTheme
import java.util.Locale

// shortcut: 이태원역이 있는 칸 고정(analysis/zones.py의 STATION 칸), 분석 범위가 바뀌면 zones에 표식 열 추가
private const val STATION_ZONE_ID = "G20"
private const val SOURCE = "출처: 서울 열린데이터광장(서울 생활인구 2026.11, 가로쓰레기통 설치정보 2025.11, 상권분석서비스 2025), 서울특별시·KT"

/** 상태 보유 진입점. ViewModel 연결 후 상태와 이벤트를 Screen에 전달 */
@Composable
fun ZoneMapRoute(
    onStartShift: (zoneId: String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: ZoneMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ZoneMapScreen(
        uiState = uiState,
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
    onSelectZone: (String) -> Unit,
    onRetry: () -> Unit,
    onStartShift: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("오늘 밤, 어느 골목으로 갈까요?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            when (uiState) {
                ZoneMapUiState.Loading -> Box(Modifier.fillMaxWidth().aspectRatio(1f), Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ZoneMapUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(uiState.message)
                    Button(onClick = onRetry) { Text("다시 시도") }
                }
                is ZoneMapUiState.Success -> {
                    val ranks = remember(uiState.zones) {
                        uiState.zones.sortedByDescending { it.score }.mapIndexed { i, z -> z.id to i + 1 }.toMap()
                    }
                    ZoneGrid(uiState.zones, ranks, uiState.selectedZoneId, onSelectZone)
                    Legend()
                    uiState.selectedZone?.let { zone ->
                        ZoneCard(zone, ranks.getValue(zone.id), uiState.zones.size)
                        Button(
                            onClick = { onStartShift(zone.id) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text("${zone.name}에서 근무 시작", fontWeight = FontWeight.Bold) }
                    } ?: Text("격자를 눌러 근무할 구역을 고르세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onSignOut) { Text("로그아웃") }
            Text(SOURCE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun cellColor(accent: Color, score: Float) = accent.copy(alpha = 0.08f + score * 0.82f)

/** 6열 격자. 북→남, 서→동 순(grid_id 정렬). 상위 3칸 순위와 이태원역 표시 */
@Composable
private fun ZoneGrid(zones: List<Zone>, ranks: Map<String, Int>, selectedZoneId: String?, onSelectZone: (String) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    // 실제 점수 폭(0.07~0.70 등)에 맞춰 색을 펼침. 범례도 '상대 비교'
    val lo = zones.minOf { it.score }
    val span = (zones.maxOf { it.score } - lo).takeIf { it > 0f } ?: 1f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        zones.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { zone ->
                    val shape = RoundedCornerShape(10.dp)
                    val rank = ranks.getValue(zone.id)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(cellColor(accent, (zone.score - lo) / span), shape)
                            .then(if (zone.id == selectedZoneId) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, shape) else Modifier)
                            .clickable(role = Role.Button, onClickLabel = "${zone.name} 선택") { onSelectZone(zone.id) },
                    ) {
                        if (rank <= 3) Badge(rank.toString(), Modifier.align(Alignment.TopEnd))
                        if (zone.id == STATION_ZONE_ID) {
                            Badge("역", Modifier.align(Alignment.TopStart), MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(label: String, modifier: Modifier, container: Color = MaterialTheme.colorScheme.surface, content: Color = MaterialTheme.colorScheme.onSurface) {
    Box(modifier.padding(4.dp).size(20.dp).background(container, CircleShape), Alignment.Center) {
        Text(label, color = content, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Legend() {
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("추천도 낮음", style = MaterialTheme.typography.bodySmall, color = muted)
        Box(
            Modifier
                .width(96.dp)
                .height(12.dp)
                .background(Brush.horizontalGradient(listOf(cellColor(accent, 0f), cellColor(accent, 1f))), RoundedCornerShape(6.dp)),
        )
        Text("높음", style = MaterialTheme.typography.bodySmall, color = muted)
        Box(Modifier.weight(1f))
        Badge("1", Modifier, MaterialTheme.colorScheme.surfaceVariant)
        Text("순위", style = MaterialTheme.typography.bodySmall, color = muted)
        Badge("역", Modifier, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface)
        Text("이태원역", style = MaterialTheme.typography.bodySmall, color = muted)
    }
}

/** 고른 구역의 추천 근거(공공데이터 지표) */
@Composable
private fun ZoneCard(zone: Zone, rank: Int, total: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(zone.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "추천도 ${"%.2f".format(Locale.US, zone.score)} · ${total}칸 중 ${rank}위",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        zone.nightPop?.let { pop ->
            Fact("금·토 밤 인구", "%,d명".format(Locale.US, pop) + (zone.weekdayPop?.let { " (평일 %,d명)".format(Locale.US, it) } ?: ""))
        }
        zone.binDistM?.let { Fact("가장 가까운 휴지통", formatDistance(it)) }
        zone.nightSales?.let { Fact("21~24시 매출(분기)", formatWon(it)) }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

/** 1억 이상은 억(10억 미만은 소수 한 자리), 그 아래는 만 원 단위 */
internal fun formatWon(won: Long): String = when {
    won >= 1_000_000_000 -> "%,d억 원".format(Locale.US, Math.round(won / 100_000_000.0))
    won >= 100_000_000 -> "%.1f억 원".format(Locale.US, won / 100_000_000.0)
    else -> "%,d만 원".format(Locale.US, Math.round(won / 10_000.0))
}

@Preview
@Composable
private fun ZoneMapScreenPreview() {
    NightCleanTheme {
        ZoneMapScreen(
            uiState = ZoneMapUiState.Success(
                zones = List(36) { Zone(id = "G%02d".format(it), name = "격자 ${it + 1}", score = (it % 5) / 4f, 5523, 3446, 31, 4_493_365_069) },
                selectedZoneId = "G19",
            ),
            onSelectZone = {}, onRetry = {}, onStartShift = {}, onSignOut = {},
        )
    }
}
