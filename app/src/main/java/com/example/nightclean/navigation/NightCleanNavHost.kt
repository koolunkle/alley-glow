package com.example.nightclean.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.nightclean.ui.checkin.CheckinRoute
import com.example.nightclean.ui.collection.CollectionRoute
import com.example.nightclean.ui.shift.ShiftRoute
import com.example.nightclean.ui.shopreport.ShopReportRoute
import com.example.nightclean.ui.zonemap.ZoneMapRoute

@Composable
fun NightCleanNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppRoute.ZoneMap) {
        composable<AppRoute.ZoneMap> {
            ZoneMapRoute(
                onStartShift = { zoneId -> navController.navigate(AppRoute.Shift(zoneId)) },
                onOpenShopReport = { navController.navigate(AppRoute.ShopReport) },
            )
        }
        composable<AppRoute.Shift> { entry ->
            ShiftRoute(
                zoneId = entry.toRoute<AppRoute.Shift>().zoneId,
                onCheckin = { navController.navigate(AppRoute.Checkin) },
                onFinish = { navController.navigate(AppRoute.Collection) },
            )
        }
        composable<AppRoute.Checkin> {
            CheckinRoute(onDone = { navController.popBackStack() })
        }
        composable<AppRoute.Collection> {
            CollectionRoute(onSubmitted = { navController.popBackStack(AppRoute.ZoneMap, inclusive = false) })
        }
        composable<AppRoute.ShopReport> {
            ShopReportRoute(onBack = { navController.popBackStack() })
        }
    }
}
