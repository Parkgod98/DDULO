package com.example.ddulo.viewmodel.state

import com.example.ddulo.data.response.StationDetailResponse

sealed class StationDetailState {
    object Idle : StationDetailState()
    object Loading : StationDetailState()
    data class Success(val data: StationDetailResponse) : StationDetailState()
    data class Error(val message: String) : StationDetailState()
}