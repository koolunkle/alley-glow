package io.github.koolunkle.alleyglow.ui.zonemap

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

    private fun viewModel(result: Result<List<Zone>>, name: Result<String?> = Result.success("데모 워커")) =
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
