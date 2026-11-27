package io.github.koolunkle.alleyglow.data.tracking

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.data.remote.ShiftRemoteDataSource
import io.github.koolunkle.alleyglow.data.remote.dto.ShiftStartDto
import io.github.koolunkle.alleyglow.data.remote.dto.TrackPointDto
import io.github.koolunkle.alleyglow.data.remote.dto.toIsoUtc
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 근무 중 위치를 30초마다 받아 5개씩(약 2분 30초마다) track_points에 올리는 포그라운드 서비스.
 * 백그라운드 위치 권한 없이 동작하도록 근무 화면(앱이 보이는 상태)에서만 시작한다.
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var remote: ShiftRemoteDataSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }

    // shortcut: 올리지 못한 위치는 메모리에만 보관(최대 MAX_BUFFER), 오프라인이 길어지면 기기 DB에 쌓도록
    private val buffer = mutableListOf<TrackPointDto>()
    private var shift: ShiftStartDto? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val current = shift ?: return
            result.locations.forEach {
                buffer += TrackPointDto(current.id, it.time.toIsoUtc(), it.latitude, it.longitude, it.accuracy)
            }
            if (buffer.size >= BATCH_SIZE) flush()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val shiftId = intent?.getStringExtra(EXTRA_SHIFT_ID)
        val zoneId = intent?.getStringExtra(EXTRA_ZONE_ID)
        if (shiftId == null || zoneId == null || shift != null) return START_NOT_STICKY
        val started = ShiftStartDto(shiftId, zoneId, intent.getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis()).toIsoUtc())
        shift = started

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        try {
            // 이동 거리 조건 없이 30초마다: 제자리에서 일한 시간도 기록돼야 구역 확인 근거가 됨
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS).build()
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // 권한이 철회된 경우
            stopSelf()
            return START_NOT_STICKY
        }
        running.value = true
        scope.launch { runCatching { remote.insertShiftIfAbsent(started) } }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        client.removeLocationUpdates(callback)
        running.value = false
        // 남은 위치는 서비스 수명과 무관하게 마저 올림
        val current = shift
        val rest = buffer.toList()
        if (current != null && rest.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch { runCatching { upload(current, rest) } }
        }
        scope.cancel()
        super.onDestroy()
    }

    /** 메인 스레드에서 호출. 실패하면 버퍼 앞에 되돌려 다음에 다시 올림 */
    private fun flush() {
        val current = shift ?: return
        val points = buffer.toList()
        buffer.clear()
        scope.launch {
            try {
                upload(current, points)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    buffer.addAll(0, points)
                    if (buffer.size > MAX_BUFFER) buffer.subList(0, buffer.size - MAX_BUFFER).clear()
                }
            }
        }
    }

    private suspend fun upload(current: ShiftStartDto, points: List<TrackPointDto>) {
        remote.insertShiftIfAbsent(current)
        remote.insertTrackPoints(points)
    }

    private fun notification() = run {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.tracking_channel), NotificationManager.IMPORTANCE_LOW))
        }
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.shift_status_tracking))
            .setContentText(getString(R.string.tracking_notification_text))
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_SHIFT_ID = "shiftId"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_STARTED_AT = "startedAt"

        /** 기록 중 여부(화면 표시용) */
        val running = MutableStateFlow(false)

        private const val CHANNEL_ID = "tracking"
        private const val NOTIFICATION_ID = 1
        private const val INTERVAL_MS = 30_000L
        private const val BATCH_SIZE = 5
        private const val MAX_BUFFER = 2_000
    }
}
