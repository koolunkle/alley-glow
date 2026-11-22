package com.example.nightclean.ui.login

import androidx.lifecycle.SavedStateHandle
import com.example.nightclean.domain.model.UserRole
import com.example.nightclean.domain.repository.AuthRepository
import com.example.nightclean.domain.repository.UnregisteredAccountException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private class FakeAuth(
        val session: Result<UserRole?> = Result.success(null),
        val signInResult: Result<UserRole> = Result.success(UserRole.WORKER),
    ) : AuthRepository {
        override suspend fun displayName() = Result.success<String?>(null)
        var signedOut = false
        override suspend fun restoreSession() = session
        override suspend fun signIn(email: String, password: String) = signInResult
        override suspend fun signOut() { signedOut = true }
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(auth: AuthRepository, signOut: Boolean = false) =
        LoginViewModel(auth, SavedStateHandle(mapOf("signOut" to signOut)))

    @Test
    fun `저장된 세션이 있으면 바로 SignedIn이 된다`() {
        val vm = viewModel(FakeAuth(session = Result.success(UserRole.SHOP)))
        assertEquals(LoginUiState.SignedIn(UserRole.SHOP), vm.uiState.value)
    }

    @Test
    fun `세션이 없으면 입력 상태가 된다`() {
        assertEquals(LoginUiState.Input(), viewModel(FakeAuth()).uiState.value)
    }

    @Test
    fun `로그아웃으로 들어오면 세션을 지우고 입력 상태가 된다`() {
        val auth = FakeAuth(session = Result.success(UserRole.WORKER))
        val vm = viewModel(auth, signOut = true)
        assertTrue(auth.signedOut)
        assertEquals(LoginUiState.Input(), vm.uiState.value)
    }

    @Test
    fun `로그인에 성공하면 역할과 함께 SignedIn이 된다`() {
        val vm = viewModel(FakeAuth())
        vm.signIn("worker@example.com", "pw")
        assertEquals(LoginUiState.SignedIn(UserRole.WORKER), vm.uiState.value)
    }

    @Test
    fun `빈 칸이 있으면 요청하지 않고 안내한다`() {
        val vm = viewModel(FakeAuth())
        vm.signIn(" ", "pw")
        assertEquals(LoginError.EMPTY, (vm.uiState.value as LoginUiState.Input).error)
    }

    @Test
    fun `역할 없는 계정은 등록 안내를 보여준다`() {
        val vm = viewModel(FakeAuth(signInResult = Result.failure(UnregisteredAccountException())))
        vm.signIn("x@example.com", "pw")
        assertEquals(LoginError.UNREGISTERED, (vm.uiState.value as LoginUiState.Input).error)
    }
}
