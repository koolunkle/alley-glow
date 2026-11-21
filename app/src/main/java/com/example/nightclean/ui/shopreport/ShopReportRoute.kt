package com.example.nightclean.ui.shopreport

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.nightclean.ui.common.PlaceholderScreen

// TODO: ShopReportViewModel 추가 (월간 지표, 암호 손님 +1 기록)
@Composable
fun ShopReportRoute(onBack: () -> Unit) {
    ShopReportScreen(onBack = onBack)
}

@Composable
fun ShopReportScreen(onBack: () -> Unit) {
    PlaceholderScreen(title = "상인 리포트", description = "청소한 밤, 광고 노출, 수거량, QR 스캔, 암호 손님 수가 들어갈 자리") {
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("돌아가기") }
    }
}
