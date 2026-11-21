package com.example.nightclean.ui.zonemap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nightclean.domain.model.Zone
import com.example.nightclean.ui.theme.NightCleanTheme

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
                    ZoneGrid(uiState.zones, uiState.selectedZoneId, onSelectZone)
                    uiState.selectedZone?.let { zone ->
                        Button(
                            onClick = { onStartShift(zone.id) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text("${zone.name}에서 근무 시작", fontWeight = FontWeight.Bold) }
                    } ?: Text("격자를 눌러 근무할 구역을 고르세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onSignOut) { Text("로그아웃") }
        }
    }
}

@Composable
private fun ZoneGrid(zones: List<Zone>, selectedZoneId: String?, onSelectZone: (String) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
        userScrollEnabled = false,
    ) {
        items(zones, key = { it.id }) { zone ->
            val shape = RoundedCornerShape(10.dp)
            Box(
                Modifier
                    .aspectRatio(1f)
                    .background(accent.copy(alpha = 0.08f + zone.score * 0.82f), shape)
                    .then(if (zone.id == selectedZoneId) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape) else Modifier)
                    .clickable(role = Role.Button, onClickLabel = "${zone.name} 선택") { onSelectZone(zone.id) },
            )
        }
    }
}

@Preview
@Composable
private fun ZoneMapScreenPreview() {
    NightCleanTheme {
        ZoneMapScreen(
            uiState = ZoneMapUiState.Success(
                zones = List(36) { Zone(id = "G$it", name = "격자 ${it + 1}", score = (it % 5) / 4f) },
                selectedZoneId = "G14",
            ),
            onSelectZone = {}, onRetry = {}, onStartShift = {}, onSignOut = {},
        )
    }
}
