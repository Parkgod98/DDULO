package com.example.ddulo.viewmodel.state

import com.example.ddulo.data.response.PathPredictionResponse
import com.example.ddulo.data.response.RouteResultResponse
import com.example.ddulo.domain.model.RouteResult

sealed class RouteSearchState {
    object Idle : RouteSearchState()
    object Loading : RouteSearchState()
    data class Success(
        val routes: List<RouteResult>,
        val routeResultResponse: RouteResultResponse,
        val pathPredictionResponse: PathPredictionResponse,
        val fetchedAt: Long = System.currentTimeMillis()
    ) : RouteSearchState()
    data class Error(val message: String) : RouteSearchState()
}