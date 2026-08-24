package com.example.ddulo.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ddulo.data.mapper.toDomain
import com.example.ddulo.domain.model.Station
import com.example.ddulo.domain.repository.RouteRepository
import com.example.ddulo.viewmodel.state.RouteSearchState
import kotlinx.coroutines.launch

class RouteViewModel(
    private val repository: RouteRepository = RouteRepository()
) : ViewModel() {

    var state by mutableStateOf<RouteSearchState>(RouteSearchState.Idle)
        private set

    private var lastFrom: Station? = null
    private var lastTo: Station? = null

    fun refresh() {
        searchRoute(lastFrom, lastTo, forceRefresh = true)
    }

    // 캐시 존재 여부 확인 (목록 화면 복귀 시 사용)
    fun hasCache(from: Station?, to: Station?): Boolean {
        return from != null && to != null && from == lastFrom && to == lastTo && state is RouteSearchState.Success
    }

    fun searchRoute(from: Station?, to: Station?, forceRefresh: Boolean = false) {
        if (from == null || to == null) return

        // 명시적 새로고침이 아니고 캐시가 있다면 API 호출 중단
        if (!forceRefresh && hasCache(from, to)) {
            Log.d("RouteVM", "Cache hit. Skipping network request.")
            return
        }

        if (state is RouteSearchState.Loading && !forceRefresh) return

        viewModelScope.launch {
            state = RouteSearchState.Loading
            try {
                val response = repository.searchRoute(from.name, to.name)
                if (!response.isSuccessful) {
                    state = RouteSearchState.Error("경로 검색 실패")
                    return@launch
                }

                val result = response.body()?.data
                if (result == null) {
                    state = RouteSearchState.Error("결과가 없습니다.")
                    return@launch
                }

                lastFrom = from
                lastTo = to

                state = RouteSearchState.Success(
                    routes = result.toDomain(),
                    routeResultResponse = result.resultResponse,
                    pathPredictionResponse = result.pathPredictionResponse,
                    fetchedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                state = RouteSearchState.Error("네트워크 오류")
            }
        }
    }

    fun resetState() {
        state = RouteSearchState.Idle
        lastFrom = null
        lastTo = null
    }
}
