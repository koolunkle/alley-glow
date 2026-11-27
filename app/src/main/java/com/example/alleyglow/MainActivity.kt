package com.example.alleyglow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.alleyglow.navigation.AlleyGlowNavHost
import com.example.alleyglow.ui.theme.AlleyGlowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 다크 테마 전용: 시스템 설정과 무관하게 시스템 바 아이콘을 밝게 유지
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            AlleyGlowTheme {
                AlleyGlowNavHost()
            }
        }
    }
}
