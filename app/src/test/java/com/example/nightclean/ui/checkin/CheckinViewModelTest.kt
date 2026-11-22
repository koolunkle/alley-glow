package com.example.nightclean.ui.checkin

import androidx.lifecycle.SavedStateHandle
import com.example.nightclean.domain.model.CheckinResult
import com.example.nightclean.domain.model.GeoPoint
import com.example.nightclean.domain.model.shopCodeOf
import com.example.nightclean.domain.repository.CheckinRepository
import com.example.nightclean.domain.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckinViewModelTest {

    private class FakeCheckins(val result: Result<CheckinResult>) : CheckinRepository {
        var sentCode: String? = null
        override suspend fun checkIn(shiftId: String, zoneId: String, startedAt: Long, shopCode: String, location: GeoPoint) =
            result.also { sentCode = shopCode }
        override suspend fun countCheckins(shiftId: String) = Result.success(3)
    }

    private class FakeLocation(val point: GeoPoint?) : LocationRepository {
        override suspend fun currentLocation() = point
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val here = GeoPoint(37.5349, 126.9942)

    private fun viewModel(checkins: CheckinRepository, location: GeoPoint? = here) = CheckinViewModel(
        checkins,
        FakeLocation(location),
        SavedStateHandle(mapOf("shiftId" to "s1", "zoneId" to "G01", "startedAt" to 1_000L)),
    )

    @Test
    fun `50m 안이면 체크인 완료 상태가 된다`() {
        val repo = FakeCheckins(Result.success(CheckinResult.CheckedIn("가게 A", 14)))
        val vm = viewModel(repo)
        vm.checkIn("https://example.com/s/NC-SHOP-001")
        assertEquals("NC-SHOP-001", repo.sentCode)
        val done = vm.uiState.value as CheckinUiState.Done
        assertEquals(CheckinResult.CheckedIn("가게 A", 14), done.result)
        assertEquals(3, done.count)
    }

    @Test
    fun `거리 초과면 체크인 수를 묻지 않는다`() {
        val vm = viewModel(FakeCheckins(Result.success(CheckinResult.TooFar("가게 A", 120))))
        vm.checkIn("NC-SHOP-001")
        assertNull((vm.uiState.value as CheckinUiState.Done).count)
    }

    @Test
    fun `위치를 못 얻으면 서버에 보내지 않고 안내한다`() {
        val repo = FakeCheckins(Result.success(CheckinResult.UnknownShop("NC-SHOP-001")))
        val vm = viewModel(repo, location = null)
        vm.checkIn("NC-SHOP-001")
        assertNull(repo.sentCode)
        assertTrue(vm.uiState.value is CheckinUiState.Error)
    }

    @Test
    fun `네트워크 실패는 고정 문구로 안내한다`() {
        val vm = viewModel(FakeCheckins(Result.failure(IllegalStateException("raw"))))
        vm.checkIn("NC-SHOP-001")
        assertEquals(
            CheckinUiState.Error(CheckinError.NETWORK),
            vm.uiState.value,
        )
    }

    @Test
    fun `거리는 1km 미만은 m, 이상은 소수 한 자리 km로 표시한다`() {
        assertEquals("32m", formatDistance(32))
        assertEquals("999m", formatDistance(999))
        assertEquals("1.0km", formatDistance(1000))
        assertEquals("18.5km", formatDistance(18476))
    }

    @Test
    fun `QR 값은 앞뒤 공백과 URL 경로를 걷어 가게 코드만 쓴다`() {
        assertEquals("NC-SHOP-001", shopCodeOf("  NC-SHOP-001 "))
        assertEquals("NC-SHOP-002", shopCodeOf("https://example.com/s/NC-SHOP-002/"))
        // QR 랜딩 주소
        assertEquals("NC-SHOP-003", shopCodeOf("https://user.github.io/night-clean/?s=NC-SHOP-003"))
        assertEquals("NC-SHOP-004", shopCodeOf("https://user.github.io/night-clean/?utm=x&s=NC-SHOP-004#top"))
    }
}
