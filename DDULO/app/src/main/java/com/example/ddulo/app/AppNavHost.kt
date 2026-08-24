package com.example.ddulo.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.ddulo.ui.screen.favorites.FavoritesScreen
import com.example.ddulo.ui.screen.home.HomeScreen
import com.example.ddulo.ui.screen.onboarding.OnboardingFlow
import com.example.ddulo.ui.screen.route.RouteDetailScreen
import com.example.ddulo.ui.screen.route.RouteListScreen
import com.example.ddulo.viewmodel.AppViewModel
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.example.ddulo.viewmodel.FavoriteViewModelFactory
import com.example.ddulo.viewmodel.NavigationEvent
import com.example.ddulo.viewmodel.RouteViewModel
import com.example.ddulo.viewmodel.SharedViewModel
import com.example.ddulo.viewmodel.StationDetailViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    
    val appViewModel: AppViewModel = viewModel()
    
    // SharedViewModel을 viewModel()로 선언하여 수명 주기를 Activity/Graph에 바인딩합니다.
    val sharedViewModel: SharedViewModel = viewModel()

    // RouteViewModel도 상위에서 관리하여 탭 전환 시 상태를 보존합니다.
    val routeViewModel: RouteViewModel = viewModel()

    val stationDetailViewModel: StationDetailViewModel = viewModel()

    val context = LocalContext.current
    val favoriteViewModel: FavoriteViewModel = viewModel(
        factory = FavoriteViewModelFactory(context)
    )

    LaunchedEffect(Unit) {
        sharedViewModel.navigationEvents.collect { event ->
            when (event) {
                NavigationEvent.ToHome -> {
                    navController.navigate("home") {
                        popUpTo("search_graph") { inclusive = false }
                        launchSingleTop = true
                    }
                }
                NavigationEvent.ToRouteList -> {
                    navController.navigate("route_list") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
                NavigationEvent.ToRouteDetail -> {
                    navController.navigate("route_detail") {
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "onboarding_flow") {
        composable("onboarding_flow") {
            OnboardingFlow(navController)
        }

        navigation(route = "search_graph", startDestination = "home") {
            composable("home") {
                HomeScreen(
                    appViewModel,
                    sharedViewModel,
                    favoriteViewModel,
                    stationDetailViewModel,
                    navController
                )
            }
            composable("route_list") { 
                RouteListScreen(
                    appViewModel,
                    sharedViewModel,
                    stationDetailViewModel,
                    routeViewModel,
                    favoriteViewModel,
                    navController
                )
            }
            composable("route_detail") {
                RouteDetailScreen(sharedViewModel, routeViewModel, navController)
            }
        }

        navigation(route = "favorites_graph", startDestination = "favorites") {
            composable("favorites") {
                FavoritesScreen(
                    navController,
                    favoriteViewModel,
                    sharedViewModel,
                    stationDetailViewModel
                )
            }
        }
    }
}
