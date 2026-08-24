package com.example.ddulo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ddulo.data.response.StationDetailResponse
import com.example.ddulo.domain.repository.StationRepository
import com.example.ddulo.viewmodel.state.StationDetailState
import kotlinx.coroutines.launch

class StationDetailViewModel(
    private val repository: StationRepository = StationRepository()
) : ViewModel() {

    // 현재 선택된 stationId (탭 식별자)
    var selectedStationId by mutableStateOf<String?>(null)
        private set

    // stationId를 키로 사용하는 캐시
    private val cache = mutableMapOf<String, StationDetailResponse>()

    var state by mutableStateOf<StationDetailState>(StationDetailState.Idle)
        private set

    /**
     * 특정 역(호선 포함)을 선택했을 때 호출됩니다.
     */
    fun selectStation(stationId: String) {
        // 이미 선택된 ID이고 성공 상태라면 무시 (중복 호출 방지)
        if (selectedStationId == stationId && state is StationDetailState.Success) return

        selectedStationId = stationId

        // 1. 캐시 히트 확인
        cache[stationId]?.let {
            state = StationDetailState.Success(it)
            return
        }

        // 2. 캐시 미스 시 API 로드
        loadStationDetail(stationId)
    }

    fun loadStationDetail(stationId: String) {
        viewModelScope.launch {
            state = StationDetailState.Loading
            try {
                val data = repository.getStationDetail(stationId)
                cache[stationId] = data
                state = StationDetailState.Success(data)
            } catch (e: Exception) {
                state = StationDetailState.Error(e.message ?: "역 정보 조회 실패")
            }
        }
    }

    /**
     * 상태 및 캐시를 초기화합니다.
     */
    fun clearState() {
        cache.clear()
        selectedStationId = null
        state = StationDetailState.Idle
    }
}
