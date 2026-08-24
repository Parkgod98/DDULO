package com.example.ddulo.viewmodel.state

import com.example.ddulo.domain.model.Station

// ✅ Success 상태에 전체 역 리스트(allStations)를 추가
sealed class InitialLoadState {
    object Loading : InitialLoadState()
    data class Success(
        val nearbyStations: List<Station>,
        val allStations: List<Station>,
        val fallbackStation: Station?
    ) : InitialLoadState()
    data class Error(val message: String) : InitialLoadState()
}