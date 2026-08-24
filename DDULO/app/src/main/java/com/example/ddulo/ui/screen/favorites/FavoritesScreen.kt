package com.example.ddulo.ui.screen.favorites

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.ddulo.data.local.entity.FavoriteType
import com.example.ddulo.ui.component.station.StationSheetContent
import com.example.ddulo.ui.layout.MainLayout
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.example.ddulo.viewmodel.NavigationEvent
import com.example.ddulo.viewmodel.SharedViewModel
import com.example.ddulo.viewmodel.StationDetailViewModel
import com.example.ddulo.viewmodel.state.RouteSearchState
import kotlinx.coroutines.launch

/**
 * FavoritesScreen + 탭, Pager 포함
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    favoriteViewModel: FavoriteViewModel,
    sharedViewModel: SharedViewModel,
    stationDetailViewModel: StationDetailViewModel
) {
    val favorites by favoriteViewModel.favorites.collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // 탭 상태 관리
    val tabs = listOf("경로", "역")
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    // 타입별 필터링
    val stationFavorites = favorites.filter { it.type == FavoriteType.STATION }
    val routeFavorites = favorites.filter { it.type == FavoriteType.ROUTE }

    // 바텀시트 상태 관리
    var showSheet by remember { mutableStateOf(false) }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    // 시트 표시 트리거 감지
    val shouldShowStationSheet by sharedViewModel.shouldShowStationSheet.collectAsState()
    LaunchedEffect(shouldShowStationSheet) {
        if (shouldShowStationSheet) {
            showSheet = true
            sharedViewModel.consumeStationSheetRequest()
        }
    }

    // showSheet 상태 제어
    LaunchedEffect(showSheet) {
        if (showSheet) {
            focusManager.clearFocus()
            scaffoldState.bottomSheetState.partialExpand()
        } else {
            scaffoldState.bottomSheetState.hide()
        }
    }

    // 즐겨찾기 검색 에러 처리
    val searchState = favoriteViewModel.state
    LaunchedEffect(searchState) {
        if (searchState is RouteSearchState.Error) {
            snackbarHostState.showSnackbar(searchState.message)
            favoriteViewModel.resetState()
        }
    }

    BackHandler(enabled = showSheet) {
        showSheet = false
        focusManager.clearFocus()
    }

    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        if (scaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
            showSheet = false
            stationDetailViewModel.clearState()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        sheetPeekHeight = if (showSheet) screenHeight * 0.75f else 0.dp,
        sheetDragHandle = { if (showSheet) BottomSheetDefaults.DragHandle() },
        sheetContent = {
            if (showSheet) {
                val selectedGroup = sharedViewModel.selectedStationGroup.value ?: emptyList()
                StationSheetContent(
                    stationGroup = selectedGroup,
                    stationDetailViewModel = stationDetailViewModel,
                    favoriteViewModel = favoriteViewModel,
                    onSetDeparture = { station ->
                        sharedViewModel.confirmDeparture(station)
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        showSheet = false
                    },
                    onSetDestination = { station ->
                        sharedViewModel.confirmDestination(station)
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        showSheet = false
                    },
                    isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                )
            } else {
                Box(Modifier.fillMaxWidth().height(1.dp))
            }
        }
    ) {
        MainLayout(navController = navController) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    TabRow(selectedTabIndex = pagerState.currentPage) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = { Text(title) }
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        when (page) {
                            0 -> RouteFavoritesList(
                                favorites = routeFavorites,
                                favoriteViewModel = favoriteViewModel,
                                onRouteClick = { favoriteData ->
                                    // ⭐️ 저장된 데이터가 아닌 실시간 재검색 수행
                                    favoriteViewModel.searchRoute(
                                        from = favoriteData.departureName,
                                        to = favoriteData.destinationName,
                                        onSuccess = { successState ->
                                            sharedViewModel.selectRoute(
                                                successState.routes.first(),
                                                successState.routeResultResponse
                                            )
                                            sharedViewModel.emitNavigationEvent(NavigationEvent.ToRouteDetail)
                                        }
                                    )
                                }
                            )

                            1 -> StationFavoritesList(
                                favorites = stationFavorites,
                                favoriteViewModel = favoriteViewModel,
                                onStationClick = { stations ->
                                    sharedViewModel.selectStationGroup(stations)
                                    sharedViewModel.requestStationSheet()
                                    showSheet = true
                                }
                            )
                        }
                    }
                }

                // 로딩 인디케이터
                if (searchState is RouteSearchState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                if (showSheet) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showSheet = false
                                focusManager.clearFocus()
                            }
                    )
                }
            }
        }
    }
}
