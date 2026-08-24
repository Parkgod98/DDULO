package com.example.ddulo.domain.repository

import android.util.Log
import com.example.ddulo.data.network.ApiService
import com.example.ddulo.data.network.RetrofitClient
import com.example.ddulo.data.response.BaseResponse
import com.example.ddulo.data.response.RouteSearchResponse
import retrofit2.Response

/**
 * 경로 검색 API 호출
 */
class RouteRepository(
    private val api: ApiService = RetrofitClient.instance
) {
    suspend fun searchRoute(
        fromName: String,
        toName: String
    ): Response<BaseResponse<RouteSearchResponse>> {

        // API를 한 번만 호출하고 결과를 response 변수에 담습니다.
        val response = api.searchRoute(fromName, toName)

        Log.d("API", "isSuccessful = ${response.isSuccessful}")
        Log.d("API", "code = ${response.code()}")

        // 로깅을 위해 본문을 확인해도 되지만, 이미 한 번 호출한 response를 그대로 반환해야 합니다.
        if (response.isSuccessful) {
            Log.d("API", "data = ${response.body()?.data}")
        } else {
            Log.e("API", "Error raw = ${response.raw()}")
        }

        return response
    }
}
