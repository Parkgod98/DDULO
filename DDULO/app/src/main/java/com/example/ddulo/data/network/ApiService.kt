package com.example.ddulo.data.network

import com.example.ddulo.data.response.BaseResponse
import com.example.ddulo.data.response.InitialData
import com.example.ddulo.data.response.RouteSearchResponse
import com.example.ddulo.data.response.StationDetailResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API 정의
 */
interface ApiService {
    @GET("/api/v1/load")
    suspend fun loadData(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String
    ): Response<BaseResponse<InitialData>>

    @GET("/api/v1/route")
    suspend fun searchRoute(
        @Query("from") fromStationName: String,
        @Query("to") toStationName: String
    ): Response<BaseResponse<RouteSearchResponse>>

    @GET("/api/v1/station-detail/{stationCode}")
    suspend fun getStationDetail(
        @Path("stationCode") stationCode: String
    ): Response<BaseResponse<StationDetailResponse>>
}
