package com.example.ddulo.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ddulo.domain.repository.StationRepository
import com.example.ddulo.viewmodel.state.InitialLoadState
import kotlinx.coroutines.launch

class AppViewModel(private val stationRepository: StationRepository = StationRepository()) : ViewModel() {

    var loadState by mutableStateOf<InitialLoadState>(InitialLoadState.Loading)
        private set

    fun loadInitialData(latitude: Double?, longitude: Double?) {
        viewModelScope.launch {
            loadState = InitialLoadState.Loading
            
            try {
                val latString = latitude?.toString() ?: ""
                val lonString = longitude?.toString() ?: ""

                val response = stationRepository.loadInitialData(latString, lonString)

                if (response.isSuccessful) {
                    val initialData = response.body()?.data
                    if (initialData != null) {
                        val nearby = initialData.nearbyStations
                        val all = initialData.allStations
                        val fallback = if (nearby.isEmpty()) all.firstOrNull() else null
                        
                        // ✅ allStations를 Success 상태에 담아 전달
                        loadState = InitialLoadState.Success(
                            nearbyStations = nearby,
                            allStations = all,
                            fallbackStation = fallback
                        )
                        Log.d(TAG, "Success: nearby=${nearby.size}, all=${all.size}, fallback=${fallback?.name}")
                    } else {
                        loadState = InitialLoadState.Error("데이터를 불러올 수 없습니다.")
                    }
                } else {
                    loadState = InitialLoadState.Error("API Error: ${response.code()}")
                }
            } catch (e: Exception) {
                loadState = InitialLoadState.Error("Network Error: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "AppViewModel"
    }
}
