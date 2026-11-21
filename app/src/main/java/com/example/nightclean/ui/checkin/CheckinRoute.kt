package com.example.nightclean.ui.checkin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.nightclean.ui.common.PlaceholderScreen

// TODO: CheckinViewModel 추가 (QR 스캔, 가게 반경 50m 확인)
@Composable
fun CheckinRoute(onDone: () -> Unit) {
    CheckinScreen(onDone = onDone)
}

@Composable
fun CheckinScreen(onDone: () -> Unit) {
    PlaceholderScreen(title = "가게 체크인", description = "QR 스캔 결과와 가게까지 거리가 들어갈 자리") {
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("근무 화면으로") }
    }
}
