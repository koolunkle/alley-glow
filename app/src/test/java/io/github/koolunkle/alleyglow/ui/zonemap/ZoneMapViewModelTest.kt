package io.github.koolunkle.alleyglow.ui.zonemap

import androidx.lifecycle.SavedStateHandle
import io.github.koolunkle.alleyglow.domain.model.Zone
import io.github.koolunkle.alleyglow.domain.model.UserRole
import io.github.koolunkle.alleyglow.domain.repository.AuthRepository
import io.github.koolunkle.alleyglow.domain.repository.ZoneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ZoneMapViewModelTest {

    private val zones = listOf(Zone("G01", "격자 1", 0.9f), Zone("G02", "격자 2", 0.1f))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    // 시간대 밖(12시)이 기본이라 처음 시간대는 평균
    private fun viewModel(result: Result<List<Zone>>, name: Result<String?> = Result.success("데모 워커"), hourOfDay: Int = 12) =
        ZoneMapViewModel(
            object : ZoneRepository {
                override suspend fun getZones() = result
            },
            object : AuthRepository {
                override suspend fun restoreSession() = Result.success<UserRole?>(null)
                override suspend fun signIn(email: String, password: String) = Result.success(UserRole.WORKER)
                override suspend fun signOut() {}
                override suspend fun displayName() = name
            },
            SavedStateHandle(mapOf("hourOfDay" to hourOfDay)),
        )

    @Test
    fun `불러오면 추천도 1위 구역을 미리 고른다`() {
        val reversed = zones.reversed()
        assertEquals(ZoneMapUiState.Success(reversed, "G01"), viewModel(Result.success(reversed)).uiState.value)
    }

    @Test
    fun `워커 이름을 받고 실패하면 null로 둔다`() {
        assertEquals("데모 워커", viewModel(Result.success(zones)).workerName.value)
        assertNull(viewModel(Result.success(zones), Result.failure(IllegalStateException())).workerName.value)
    }

    @Test
    fun `불러오기에 실패하면 Error 상태가 된다`() {
        val vm = viewModel(Result.failure(IllegalStateException("network")))
        assertEquals(ZoneMapUiState.Error, vm.uiState.value)
    }

    @Test
    fun `지금 시각의 시간대를 먼저 고르고 그 시간 1위 구역을 미리 고른다`() {
        val hourly = listOf(
            Zone("G01", "격자 1", 0.9f, scoreByHour = listOf(0.9f, 0.2f, 0.9f, 0.9f)),
            Zone("G02", "격자 2", 0.1f, scoreByHour = listOf(0.1f, 0.8f, 0.1f, 0.1f)),
        )
        assertEquals(ZoneMapUiState.Success(hourly, "G02", slot = 1), viewModel(Result.success(hourly), hourOfDay = 20).uiState.value)
        assertEquals(ZoneMapUiState.Success(hourly, "G01", slot = null), viewModel(Result.success(hourly), hourOfDay = 23).uiState.value)
    }

    @Test
    fun `시간대를 바꿔도 고른 구역은 그대로 둔다`() {
        val vm = viewModel(Result.success(zones))
        vm.selectZone("G02")
        vm.selectSlot(2)
        assertEquals(ZoneMapUiState.Success(zones, "G02", slot = 2), vm.uiState.value)
        vm.selectSlot(null)
        assertNull((vm.uiState.value as ZoneMapUiState.Success).slot)
    }

    @Test
    fun `시간대 값은 고른 칸을 쓰고 없으면 평균으로 돌아간다`() {
        val zone = Zone("G01", "격자 1", 0.5f, nightPop = 100, scoreByHour = listOf(0.1f, 0.2f, 0.3f, 0.4f), nightPopByHour = listOf(1, 2, 3, 4))
        assertEquals(0.5f, zone.scoreAt(null))
        assertEquals(0.3f, zone.scoreAt(2))
        assertEquals(4, zone.nightPopAt(3))
        assertEquals(100, zone.nightPopAt(null))
        assertEquals(0.5f, Zone("G02", "격자 2", 0.5f).scoreAt(1))
        assertNull(zone.weekdayPopAt(1))
    }

    @Test
    fun `격자를 고르면 selectedZone이 바뀐다`() {
        val vm = viewModel(Result.success(zones))
        vm.selectZone("G02")
        assertEquals(zones[1], (vm.uiState.value as ZoneMapUiState.Success).selectedZone)
    }

    @Test
    fun `매출은 억·만 원 단위로 줄여 보인다`() {
        assertEquals("45억 원", formatWon(4_493_365_069))
        assertEquals("4.2억 원", formatWon(418_462_829))
        assertEquals("1.0억 원", formatWon(100_000_000))
        assertEquals("9,999만 원", formatWon(99_990_000))
        assertEquals("0만 원", formatWon(0))
    }

    @Test
    fun `평일 대비 증감률은 부호를 붙이고 평일 0은 null`() {
        assertEquals("+60%", formatChange(5523, 3446))
        assertEquals("-50%", formatChange(50, 100))
        assertEquals("+0%", formatChange(100, 100))
        assertNull(formatChange(10, 0))
    }
}
