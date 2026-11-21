package com.example.nightclean.ui.shift

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.nightclean.ui.common.PlaceholderScreen

// TODO: ShiftViewModel 추가 (근무 시간, 광고 슬롯, GPS 기록)
@Composable
fun ShiftRoute(
    zoneId: String,
    onCheckin: () -> Unit,
    onFinish: () -> Unit,
) {
    ShiftScreen(zoneId = zoneId, onCheckin = onCheckin, onFinish = onFinish)
}

@Composable
fun ShiftScreen(zoneId: String, onCheckin: () -> Unit, onFinish: () -> Unit) {
    PlaceholderScreen(title = "근무 중", description = "구역 $zoneId · 근무 시간, 광고 슬롯, GPS 기록이 들어갈 자리") {
        Button(onClick = onCheckin, modifier = Modifier.fillMaxWidth()) { Text("가게 체크인 (QR)") }
        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("근무 종료 · 수거량 입력") }
    }
}
