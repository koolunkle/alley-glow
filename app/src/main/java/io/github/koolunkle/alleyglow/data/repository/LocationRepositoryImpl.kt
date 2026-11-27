package io.github.koolunkle.alleyglow.data.repository

import android.annotation.SuppressLint
import android.content.Context
import io.github.koolunkle.alleyglow.domain.model.GeoPoint
import io.github.koolunkle.alleyglow.domain.repository.LocationRepository
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : LocationRepository {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    // 권한은 화면에서 요청. 없으면 SecurityException → null
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): GeoPoint? = try {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(10_000)
            .build()
        val cancel = CancellationTokenSource()
        client.getCurrentLocation(request, cancel.token).await(cancel)?.let { GeoPoint(it.latitude, it.longitude) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
