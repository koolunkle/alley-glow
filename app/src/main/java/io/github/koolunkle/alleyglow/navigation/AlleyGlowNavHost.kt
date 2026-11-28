package io.github.koolunkle.alleyglow.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.koolunkle.alleyglow.domain.model.UserRole
import io.github.koolunkle.alleyglow.ui.checkin.CheckinRoute
import io.github.koolunkle.alleyglow.ui.collection.CollectionRoute
import io.github.koolunkle.alleyglow.ui.login.LoginRoute
import io.github.koolunkle.alleyglow.ui.shift.ShiftRoute
import io.github.koolunkle.alleyglow.ui.shopreport.ShopReportRoute
import io.github.koolunkle.alleyglow.ui.zonemap.ZoneMapRoute

@Composable
fun AlleyGlowNavHost() {
    val navController = rememberNavController()
    // 스택을 모두 비우고 로그인 화면으로
    val signOut = {
        navController.navigate(AppRoute.Login(signOut = true)) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
    NavHost(navController = navController, startDestination = AppRoute.Login()) {
        composable<AppRoute.Login> {
            LoginRoute(onSignedIn = { role ->
                val home = if (role == UserRole.SHOP) AppRoute.ShopReport else AppRoute.ZoneMap
                navController.navigate(home) { popUpTo<AppRoute.Login> { inclusive = true } }
            })
        }
        composable<AppRoute.ZoneMap> {
            ZoneMapRoute(
                onStartShift = { zoneId, zoneName -> navController.navigate(AppRoute.Shift(zoneId, zoneName)) },
                onSignOut = signOut,
            )
        }
        composable<AppRoute.Shift> { entry ->
            val route = entry.toRoute<AppRoute.Shift>()
            val zoneId = route.zoneId
            ShiftRoute(
                zoneId = zoneId,
                zoneName = route.zoneName,
                onCheckin = { shiftId, startedAt -> navController.navigate(AppRoute.Checkin(shiftId, zoneId, startedAt)) },
                onFinish = { shiftId, startedAt, beforePhotos, bagsByHour ->
                    navController.navigate(AppRoute.Collection(shiftId, zoneId, startedAt, beforePhotos, bagsByHour))
                },
                onLeave = { navController.popBackStack() },
            )
        }
        composable<AppRoute.Checkin> {
            CheckinRoute(onDone = { navController.popBackStack() })
        }
        composable<AppRoute.Collection> {
            CollectionRoute(
                onSubmitted = { navController.popBackStack(AppRoute.ZoneMap, inclusive = false) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<AppRoute.ShopReport> {
            ShopReportRoute(onSignOut = signOut)
        }
    }
}
