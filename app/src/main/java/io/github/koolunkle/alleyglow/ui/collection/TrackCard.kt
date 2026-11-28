package io.github.koolunkle.alleyglow.ui.collection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.domain.model.ShiftTrack
import io.github.koolunkle.alleyglow.domain.model.metersFrom
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import io.github.koolunkle.alleyglow.ui.theme.cardSurface
import kotlin.math.abs

// 배정 칸(한 변 250m)과 둘레 한 칸씩, 3×3칸
private const val CELL_HALF_M = 125.0
private const val VIEW_HALF_M = 375.0

/** 배정 구역 칸을 가운데 둔 3×3칸 위에 GPS 기록을 순서대로 이은 선. 범위 밖 기록은 개수만 */
@Composable
internal fun TrackCard(track: ShiftTrack) {
    val offsets = remember(track) { track.points.map { it.metersFrom(track.zoneCenter) } }
    val outside = offsets.count { (x, y) -> abs(x) > VIEW_HALF_M || abs(y) > VIEW_HALF_M }
    val gridColor = MaterialTheme.colorScheme.outline
    val cellColor = MaterialTheme.colorScheme.background
    val pathColor = MaterialTheme.colorScheme.onSurface
    val caption = stringResource(R.string.collection_track_caption) +
        if (outside > 0) stringResource(R.string.collection_track_outside, outside) else ""
    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface()
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(stringResource(R.string.collection_track), style = MaterialTheme.typography.titleMedium)
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f).clipToBounds()) {
            val scale = size.width / (2 * VIEW_HALF_M).toFloat()
            fun at(x: Double, y: Double) = Offset(((x + VIEW_HALF_M) * scale).toFloat(), ((VIEW_HALF_M - y) * scale).toFloat())
            val cell = (2 * CELL_HALF_M * scale).toFloat()
            drawRect(cellColor, topLeft = at(-CELL_HALF_M, CELL_HALF_M), size = Size(cell, cell))
            for (i in 1..2) {
                val p = i * size.width / 3
                drawLine(gridColor, Offset(p, 0f), Offset(p, size.height))
                drawLine(gridColor, Offset(0f, p), Offset(size.width, p))
            }
            val path = Path()
            offsets.forEachIndexed { i, (x, y) ->
                val o = at(x, y)
                if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            drawPath(path, pathColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            offsets.lastOrNull()?.let { (x, y) -> drawCircle(pathColor, 4.dp.toPx(), at(x, y)) }
        }
        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
