package com.example.dripguardai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dripguardai.ui.DripGuardViewModel
import com.example.dripguardai.ui.screens.HomeScreen
import com.example.dripguardai.ui.screens.PlannerScreen
import com.example.dripguardai.ui.screens.SavedLocationsHistoryScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Planner : Screen("planner")
    object History : Screen("history")
}

@Composable
fun AppNavigation(
    viewModel: DripGuardViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToPlanner = { navController.navigate(Screen.Planner.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) }
            )
        }
        composable(Screen.Planner.route) {
            PlannerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            SavedLocationsHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSelectLocationForWeather = { cityName ->
                    viewModel.fetchDashboardWeather(cityName)
                }
            )
        }
    }
}
