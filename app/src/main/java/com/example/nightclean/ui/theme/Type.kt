package com.example.nightclean.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.nightclean.R

// Pretendard(OFL, res/raw/pretendard_license.txt). 쓰는 굵기 3개만 포함, 500은 400으로 대체
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_black, FontWeight.Black),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Pretendard,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

/** 글자 크기 단계. 화면에서는 이 이름으로만 고르고 크기를 직접 쓰지 않음 */
val Typography = Typography(
    displayMedium = style(56, 64, FontWeight.Black, -0.02).copy(fontFeatureSettings = "tnum"), // 근무 타이머
    headlineMedium = style(32, 40, FontWeight.Black, -0.02), // 카드 속 큰 숫자
    headlineSmall = style(24, 32, FontWeight.Black, -0.02),  // 화면 제목
    titleLarge = style(20, 28, FontWeight.Bold, -0.01),      // 카드 제목, 큰 숫자
    titleMedium = style(16, 24, FontWeight.Bold),            // 섹션 제목
    titleSmall = style(14, 20, FontWeight.Bold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal),
    labelLarge = style(16, 24, FontWeight.Bold),             // 버튼
    labelMedium = style(13, 18, FontWeight.Bold),            // 칩, 상단바
    labelSmall = style(11, 14, FontWeight.Bold),             // 배지
)
