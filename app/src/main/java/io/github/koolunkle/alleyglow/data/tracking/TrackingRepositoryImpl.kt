package io.github.koolunkle.alleyglow.data.tracking

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.koolunkle.alleyglow.domain.repository.TrackingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class TrackingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TrackingRepository {

    override val isTracking: StateFlow<Boolean> = LocationTrackingService.running.asStateFlow()

    override fun start(shiftId: String, zoneId: String, startedAt: Long) {
        val intent = Intent(context, LocationTrackingService::class.java)
            .putExtra(LocationTrackingService.EXTRA_SHIFT_ID, shiftId)
            .putExtra(LocationTrackingService.EXTRA_ZONE_ID, zoneId)
            .putExtra(LocationTrackingService.EXTRA_STARTED_AT, startedAt)
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stop() {
        context.stopService(Intent(context, LocationTrackingService::class.java))
    }
}
