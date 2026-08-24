package com.example.ddulo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ddulo.data.local.dao.FavoriteDao
import com.example.ddulo.data.local.entity.FavoriteEntity
import com.example.ddulo.data.local.entity.FavoriteType
import com.example.ddulo.data.mapper.toDomain
import com.example.ddulo.domain.model.FavoriteRoute
import com.example.ddulo.domain.model.RouteResult
import com.example.ddulo.domain.model.Station
import com.example.ddulo.domain.repository.RouteRepository
import com.example.ddulo.viewmodel.state.RouteSearchState
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FavoriteViewModel(
    private val dao: FavoriteDao,
    private val repository: RouteRepository = RouteRepository()
) : ViewModel() {

    var state by mutableStateOf<RouteSearchState>(RouteSearchState.Idle)
        private set

    val favorites: Flow<List<FavoriteEntity>> = dao.observeFavorites()

    /** 역 즐겨찾기 */
    fun addStationFavorite(
        stations: List<Station>,
        name: String? = null
    ) {
        val displayName = name ?: stations.first().stationName

        viewModelScope.launch {
            dao.insert(
                FavoriteEntity(
                    type = FavoriteType.STATION,
                    displayName = displayName,
                    payload = Gson().toJson(stations)
                )
            )
        }
    }

    /**
     * 경로 즐겨찾기
     * @param route 저장 시점의 요약 정보를 위해 RouteResult 객체를 받음
     */
    fun addRouteFavorite(
        route: RouteResult
    ) {
        val departureName = route.legs.first().startStation
        val destinationName = route.legs.last().endStation
        
        val defaultName = "$departureName → $destinationName"
        
        val favoriteRoute = FavoriteRoute(
            departureName = departureName,
            destinationName = destinationName,
            totalTime = route.totalTime,
            transferCount = route.transferCount
        )

        viewModelScope.launch {
            dao.insert(
                FavoriteEntity(
                    type = FavoriteType.ROUTE,
                    displayName = defaultName,
                    payload = Gson().toJson(favoriteRoute)
                )
            )
        }
    }

    /** ID 기반 삭제 */
    fun removeFavoriteById(favoriteId: Long) {
        viewModelScope.launch {
            val favoriteList = dao.observeFavorites().first()
            val favorite = favoriteList.find { it.id == favoriteId }
            favorite?.let { dao.delete(it) }
        }
    }

    fun searchRoute(from: String, to: String, onSuccess: (RouteSearchState.Success) -> Unit = {}) {
        if (state is RouteSearchState.Loading) return

        viewModelScope.launch {
            state = RouteSearchState.Loading
            try {
                val response = repository.searchRoute(from, to)
                if (!response.isSuccessful) {
                    state = RouteSearchState.Error("경로 검색 실패")
                    return@launch
                }

                val result = response.body()?.data
                if (result == null) {
                    state = RouteSearchState.Error("결과가 없습니다.")
                    return@launch
                }

                val successState = RouteSearchState.Success(
                    routes = result.toDomain(),
                    routeResultResponse = result.resultResponse,
                    pathPredictionResponse = result.pathPredictionResponse,
                    fetchedAt = System.currentTimeMillis()
                )
                state = successState
                onSuccess(successState)
            } catch (e: Exception) {
                state = RouteSearchState.Error("네트워크 오류")
            }
        }
    }

    fun resetState() {
        state = RouteSearchState.Idle
    }
}
