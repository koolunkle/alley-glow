package com.example.nightclean.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.nightclean.domain.model.UserRole
import com.example.nightclean.ui.checkin.CheckinRoute
import com.example.nightclean.ui.collection.CollectionRoute
import com.example.nightclean.ui.login.LoginRoute
import com.example.nightclean.ui.shift.ShiftRoute
import com.example.nightclean.ui.shopreport.ShopReportRoute
import com.example.nightclean.ui.zonemap.ZoneMapRoute

@Composable
fun NightCleanNavHost() {
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
                onStartShift = { zoneId -> navController.navigate(AppRoute.Shift(zoneId)) },
                onSignOut = signOut,
            )
        }
        composable<AppRoute.Shift> { entry ->
            val zoneId = entry.toRoute<AppRoute.Shift>().zoneId
            ShiftRoute(
                zoneId = zoneId,
                onCheckin = { shiftId, startedAt -> navController.navigate(AppRoute.Checkin(shiftId, zoneId, startedAt)) },
                onFinish = { shiftId, startedAt, beforePhotos, bagsByHour ->
                    navController.navigate(AppRoute.Collection(shiftId, zoneId, startedAt, beforePhotos, bagsByHour))
                },
            )
        }
        composable<AppRoute.Checkin> {
            CheckinRoute(onDone = { navController.popBackStack() })
        }
        composable<AppRoute.Collection> {
            CollectionRoute(onSubmitted = { navController.popBackStack(AppRoute.ZoneMap, inclusive = false) })
        }
        composable<AppRoute.ShopReport> {
            ShopReportRoute(onSignOut = signOut)
        }
    }
}
