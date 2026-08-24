package com.example.ddulo.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ddulo.data.response.RouteResultResponse
import com.example.ddulo.domain.model.RouteResult
import com.example.ddulo.domain.model.Station
import com.example.ddulo.domain.util.StationFilter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SharedViewModel : ViewModel() {
    private val _departureInput = mutableStateOf("")
    val departureInput: State<String> = _departureInput

    private val _destinationInput = mutableStateOf("")
    val destinationInput: State<String> = _destinationInput

    private val _departureStation = mutableStateOf<Station?>(null)
    val departureStation: State<Station?> = _departureStation

    private val _destinationStation = mutableStateOf<Station?>(null)
    val destinationStation: State<Station?> = _destinationStation

    private val _hasExecutedSearch = mutableStateOf(false)
    val hasExecutedSearch: State<Boolean> = _hasExecutedSearch

    private val _selectedStationGroup = mutableStateOf<List<Station>?>(null)
    val selectedStationGroup: State<List<Station>?> = _selectedStationGroup

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _focusedField = MutableStateFlow(FocusedField.NONE)
    val focusedField: StateFlow<FocusedField> = _focusedField

    private val departureDebounceFlow = MutableStateFlow("")
    private val destinationDebounceFlow = MutableStateFlow("")

    private var isSwapping = false

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val navigationEvents = _navigationEvents.asSharedFlow()

    private val _shouldShowStationSheet = MutableStateFlow(false)
    val shouldShowStationSheet: StateFlow<Boolean> = _shouldShowStationSheet

    fun requestStationSheet() {
        _shouldShowStationSheet.value = true
    }

    fun consumeStationSheetRequest() {
        _shouldShowStationSheet.value = false
    }

    private val stationFilter = StationFilter()
    
    private val _filteredStations = mutableStateOf<List<Station>>(emptyList())
    val filteredStations: State<List<Station>> = _filteredStations

    var selectedRoute by mutableStateOf<RouteResult?>(null)
        private set

    var selectedRouteDetail by mutableStateOf<RouteResultResponse?>(null)
        private set

    init {
        viewModelScope.launch {
            combine(
                departureDebounceFlow.debounce(300),
                destinationDebounceFlow.debounce(300),
                _focusedField
            ) { dep, dest, focus ->
                Triple(dep, dest, focus)
            }.collect { (dep, dest, focus) ->
                updateFilteredStations(dep, dest, focus)
            }
        }
    }

    fun setAllStations(stations: List<Station>) {
        stationFilter.updateSource(stations)
        if (_departureInput.value.isBlank() && _destinationInput.value.isBlank()) {
            _filteredStations.value = stations
        }
    }

    fun updateDepartureInput(name: String) {
        if (_departureInput.value == name) return
        _departureInput.value = name
        departureDebounceFlow.value = name
        invalidateSearchStateIfNeeded()
    }

    fun updateDestinationInput(name: String) {
        if (_destinationInput.value == name) return
        _destinationInput.value = name
        destinationDebounceFlow.value = name
        invalidateSearchStateIfNeeded()
    }
    
    fun setFocusedField(field: FocusedField) {
        _focusedField.value = field
    }

    private fun updateFilteredStations(dep: String, dest: String, focus: FocusedField) {
        _filteredStations.value = when (focus) {
            FocusedField.DEPARTURE -> stationFilter.filter(dep)
            FocusedField.DESTINATION -> stationFilter.filter(dest)
            FocusedField.NONE -> _filteredStations.value
        }
    }

    fun selectStationGroup(stations: List<Station>) {
        _selectedStationGroup.value = stations
    }

    fun selectRoute(route: RouteResult, routeDetail: RouteResultResponse? = null) {
        selectedRoute = route
        selectedRouteDetail = routeDetail
    }

    private fun tryExecuteSearch() {
        if (_departureStation.value != null && _destinationStation.value != null) {
            _hasExecutedSearch.value = true
            // ⭐️ 검색 결과 진입 시 포커스 강제 해제
            _focusedField.value = FocusedField.NONE
            viewModelScope.launch {
                _navigationEvents.emit(NavigationEvent.ToRouteList)
            }
        }
    }

    fun confirmDeparture(station: Station) {
        if (validateSameStation(station, _destinationStation.value)) {
            _departureStation.value = station
            _departureInput.value = station.name
            departureDebounceFlow.value = station.name
            clearError()
            tryExecuteSearch()
        }
    }

    fun confirmDestination(station: Station) {
        if (validateSameStation(station, _departureStation.value)) {
            _destinationStation.value = station
            _destinationInput.value = station.name
            destinationDebounceFlow.value = station.name
            clearError()
            tryExecuteSearch()
        }
    }

    private fun invalidateSearchStateIfNeeded() {
        if (isSwapping) return

        val focus = _focusedField.value

        when (focus) {
            FocusedField.DEPARTURE -> {
                if (_departureStation.value != null && _departureStation.value?.name != _departureInput.value) {
                    _departureStation.value = null
                    _hasExecutedSearch.value = false
                }
            }
            FocusedField.DESTINATION -> {
                if (_destinationStation.value != null && _destinationStation.value?.name != _destinationInput.value) {
                    _destinationStation.value = null
                    _hasExecutedSearch.value = false
                }
            }
            FocusedField.NONE -> {}
        }
    }

    private fun validateSameStation(newStation: Station, otherStation: Station?): Boolean {
        return if (otherStation != null && newStation.name == otherStation.name) {
            _errorMessage.value = "출발지와 도착지는 같을 수 없습니다."
            false
        } else {
            true
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun swap() {
        val dep = _departureStation.value
        val dest = _destinationStation.value

        if (dep != null && dest != null) {
            isSwapping = true 
            try {
                _departureStation.value = dest
                _destinationStation.value = dep
                _departureInput.value = dest.name
                _destinationInput.value = dep.name
                departureDebounceFlow.value = dest.name
                destinationDebounceFlow.value = dep.name
                clearError()
                tryExecuteSearch()
            } finally {
                isSwapping = false 
            }
        } else {
            _hasExecutedSearch.value = false
            _errorMessage.value = "출발지와 도착지를 모두 선택해 주세요."
        }
    }

    fun emitNavigationEvent(event: NavigationEvent) {
        viewModelScope.launch {
            _navigationEvents.emit(event)
        }
    }
}
