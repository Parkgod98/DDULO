package com.example.ddulo.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ddulo.ui.component.search.SearchBar
import com.example.ddulo.ui.component.search.UnifiedSearchContent
import com.example.ddulo.ui.component.station.StationSheetContent
import com.example.ddulo.ui.layout.MainLayout
import com.example.ddulo.viewmodel.AppViewModel
import com.example.ddulo.viewmodel.FavoriteViewModel
import com.example.ddulo.viewmodel.FocusedField
import com.example.ddulo.viewmodel.SharedViewModel
import com.example.ddulo.viewmodel.StationDetailViewModel
import com.example.ddulo.viewmodel.state.InitialLoadState

/**
 * HomeScreen + 검색, Sheet 로직
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appViewModel: AppViewModel,
    sharedViewModel: SharedViewModel,
    favoriteViewModel: FavoriteViewModel,
    stationDetailViewModel: StationDetailViewModel,
    navController: NavController
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by sharedViewModel.errorMessage

    val loadState = appViewModel.loadState
    LaunchedEffect(loadState) {
        if (loadState is InitialLoadState.Success) {
            sharedViewModel.setAllStations(loadState.allStations)
        }
    }

    val focusedField by sharedViewModel.focusedField.collectAsState()
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

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            sharedViewModel.clearError()
        }
    }

    // 뒤로가기 시 바텀시트 닫기 처리
    BackHandler(enabled = showSheet) {
        showSheet = false
        focusManager.clearFocus()
    }

    // 즐겨찾기 탭에서 역 클릭 시 바텀시트 표시 트리거 감지
    val shouldShowStationSheet = sharedViewModel.shouldShowStationSheet
    LaunchedEffect(shouldShowStationSheet) {
        if (shouldShowStationSheet.value) {
            showSheet = true
            sharedViewModel.consumeStationSheetRequest()
        }
    }

    // showSheet 상태에 따라 시트 표시/숨김 제어 및 키보드 제어
    LaunchedEffect(showSheet) {
        if (showSheet) {
            focusManager.clearFocus() // 시트 열릴 때 키보드 숨김
            scaffoldState.bottomSheetState.partialExpand()
        } else {
            scaffoldState.bottomSheetState.hide()
        }
    }

    // 시트가 스와이프 등으로 닫혔을 때 상태 동기화
    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        if (scaffoldState.bottomSheetState.currentValue == SheetValue.Hidden) {
            showSheet = false
            stationDetailViewModel.clearState()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
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
    ) { scaffoldPadding ->
        MainLayout(
            navController = navController
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                // 메인 콘텐츠
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
                        // 포커스가 있고 && 입력값이 있을 때만 자동완성(isSearching = true)
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

                // 시트 활성화 시 배경 딤드 처리 (Z-축 레이어 구분)
                if (showSheet) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                showSheet = false
                                focusManager.clearFocus()
                            }
                    )
                }
            }
        }
    }
}
