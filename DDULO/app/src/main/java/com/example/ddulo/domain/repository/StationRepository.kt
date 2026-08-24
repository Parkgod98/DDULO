package com.example.ddulo.domain.repository

import android.util.Log
import com.example.ddulo.data.network.RetrofitClient
import com.example.ddulo.data.response.BaseResponse
import com.example.ddulo.data.response.InitialData
import com.example.ddulo.data.response.StationDetailResponse
import retrofit2.Response

/**
 * 역 정보 초기 데이터 / 상세 정보 조회
 */
class StationRepository {
    private val api = RetrofitClient.instance

    // 🔥 stationId 기반 메모리 캐시
    private val cache = mutableMapOf<String, StationDetailResponse>()

    suspend fun loadInitialData(lat: String, lon: String): Response<BaseResponse<InitialData>> {
        return api.loadData(lat, lon)
    }

    suspend fun getStationDetail(stationId: String): StationDetailResponse {
        cache[stationId]?.let { return it }

        val response = api.getStationDetail(stationId)

        Log.d("API", "isSuccessful = ${response.isSuccessful}")
        Log.d("API", "code = ${response.code()}")
        Log.d("API", "raw = ${response.raw()}")

        val body = response.body()
        Log.d("API", "body = $body")
        Log.d("API", "data = ${body?.data}")

        if (!response.isSuccessful) {
            throw IllegalStateException("역 정보 조회 실패")
        }

        val data = body?.data
            ?: throw IllegalStateException("응답 데이터 없음")

        cache[stationId] = data
        return data
    }
}