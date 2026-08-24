package com.example.ddulo.ui.screen.route

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ddulo.ui.component.routeList.RouteCard
import com.example.ddulo.ui.component.search.SearchBar
import com.example.ddulo.ui.component.search.UnifiedSearchContent
import com.example.ddulo.ui.component.station.StationSheetContent
import com.example.ddulo.ui.layout.MainLayout
import com.example.ddulo.viewmodel.AppViewModel
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.example.ddulo.viewmodel.FocusedField
import com.example.ddulo.viewmodel.NavigationEvent
import com.example.ddulo.viewmodel.RouteViewModel
import com.example.ddulo.viewmodel.SharedViewModel
import com.example.ddulo.viewmodel.StationDetailViewModel
import com.example.ddulo.viewmodel.state.RouteSearchState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    appViewModel: AppViewModel,
    sharedViewModel: SharedViewModel,
    stationDetailViewModel: StationDetailViewModel,
    routeViewModel: RouteViewModel,
    favoriteViewModel: FavoriteViewModel,
    navController: NavController
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by sharedViewModel.errorMessage
    val focusedField by sharedViewModel.focusedField.collectAsState()
    val hasExecutedSearch = sharedViewModel.hasExecutedSearch.value
    
    val departureInput = sharedViewModel.departureInput.value
    val destinationInput = sharedViewModel.destinationInput.value
    val departureStation = sharedViewModel.departureStation.value
    val destinationStation = sharedViewModel.destinationStation.value

    val currentInput = when (focusedField) {
        FocusedField.DEPARTURE -> departureInput
        FocusedField.DESTINATION -> destinationInput
        else -> ""
    }

    var showSheet by remember { mutableStateOf(false) }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    val routeState = routeViewModel.state

    LaunchedEffect(
        sharedViewModel.departureStation.value,
        sharedViewModel.destinationStation.value,
        sharedViewModel.hasExecutedSearch.value
    ) {
        val from = sharedViewModel.departureStation.value
        val to = sharedViewModel.destinationStation.value
        
        if (sharedViewModel.hasExecutedSearch.value && from != null && to != null) {
            // 이미 캐시된 결과가 있다면 새로 API를 호출하지 않습니다.
            if (!routeViewModel.hasCache(from, to)) {
                routeViewModel.searchRoute(from, to)
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            sharedViewModel.clearError()
        }
    }

    BackHandler(enabled = showSheet) {
        showSheet = false
        focusManager.clearFocus()
    }

    val shouldShowStationSheet by sharedViewModel.shouldShowStationSheet.collectAsState()
    LaunchedEffect(shouldShowStationSheet) {
        if (shouldShowStationSheet) {
            showSheet = true
            sharedViewModel.consumeStationSheetRequest()
        }
    }

    LaunchedEffect(showSheet) {
        if (showSheet) {
            focusManager.clearFocus()
            scaffoldState.bottomSheetState.partialExpand()
        } else {
            scaffoldState.bottomSheetState.hide()
        }
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
                        if (sharedViewModel.errorMessage.value == null) {
                            focusManager.clearFocus()
                            showSheet = false
                        }
                    },
                    onSetDestination = { station ->
                        sharedViewModel.confirmDestination(station)
                        if (sharedViewModel.errorMessage.value == null) {
                            focusManager.clearFocus()
                            showSheet = false
                        }
                    },
                    isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                )
            } else {
                Box(Modifier.fillMaxWidth().height(1.dp))
            }
        }
    ) { scaffoldPaddingValues ->
        MainLayout(navController = navController) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    SearchBar(
                        departure = departureInput,
                        destination = destinationInput,
                        departureConfirmed = departureStation != null,
                        destinationConfirmed = destinationStation != null,
                        onDepartureChange = { sharedViewModel.updateDepartureInput(it) },
                        onDestinationChange = { sharedViewModel.updateDestinationInput(it) },
                        onSwap = {
                            focusManager.clearFocus()
                            sharedViewModel.setFocusedField(FocusedField.NONE)
                            sharedViewModel.swap()
                        },
                        onFocusChange = { sharedViewModel.setFocusedField(it) }
                    )

                    Spacer(Modifier.height(24.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        // 1. 결과 레이어: 검색이 실행되었고, 현재 입력 중(포커스)이 아닐 때만 표시
                        if (hasExecutedSearch && focusedField == FocusedField.NONE) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "추천 경로",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                when (routeState) {
                                    is RouteSearchState.Loading -> {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                    is RouteSearchState.Error -> {
                                        Text(text = routeState.message, color = MaterialTheme.colorScheme.error)
                                    }
                                    is RouteSearchState.Success -> {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(routeState.routes) { route ->
                                                RouteCard(
                                                    route = route,
                                                    pred = routeState.pathPredictionResponse,
                                                    detail = routeState.routeResultResponse,
                                                    favoriteViewModel = favoriteViewModel,
                                                    onClick = {
                                                        sharedViewModel.selectRoute(route, routeState.routeResultResponse)
                                                        sharedViewModel.emitNavigationEvent(NavigationEvent.ToRouteDetail)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }

                        // 2. 검색 레이어: 검색이 실행되지 않았거나, 현재 입력 창을 클릭하여 입력 중일 때 표시
                        if (!hasExecutedSearch || focusedField != FocusedField.NONE) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                val isSearching = focusedField != FocusedField.NONE && currentInput.isNotBlank()
                                UnifiedSearchContent(
                                    isSearching = isSearching,
                                    appViewModel = appViewModel,
                                    sharedViewModel = sharedViewModel,
                                    favoriteViewModel = favoriteViewModel,
                                    stationDetailViewModel = stationDetailViewModel,
                                    onShowSheet = { showSheet = true }
                                )
                            }
                        }
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
