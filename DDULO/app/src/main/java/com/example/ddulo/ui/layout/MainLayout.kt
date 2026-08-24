package com.example.ddulo.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun MainLayout(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit
) {
    val tabs = listOf("검색", "즐겨찾기")
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val selectedTabIndex = when {
        currentDestination?.hierarchy?.any { it.route == "favorites_graph" } == true -> 1
        else -> 0
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            val graphRoute = if (index == 0) "search_graph" else "favorites_graph"

                            // ⭐️ 유저 제안: 즐겨찾기 탭에서 '검색' 탭 클릭 시 뒤로가기로 동작
                            if (index == 0 && selectedTabIndex == 1) {
                                if (!navController.popBackStack()) {
                                    // 만약 백스택이 비어있다면 일반적인 탭 이동 수행
                                    navController.navigate(graphRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            } else if (selectedTabIndex != index) {
                                // 일반적인 탭 전환
                                navController.navigate(graphRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}
