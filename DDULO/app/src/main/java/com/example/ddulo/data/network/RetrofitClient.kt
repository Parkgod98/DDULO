package com.example.ddulo.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Singleton object to provide a single instance of Retrofit
object RetrofitClient {
    private const val BASE_URL = "https://i14a204.p.ssafy.io"

    // OkHttpClient 인스턴스를 생성하고 타임아웃을 설정합니다.
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // 연결 타임아웃 30초
        .readTimeout(30, TimeUnit.SECONDS)    // 읽기 타임아웃 30초
        .writeTimeout(30, TimeUnit.SECONDS)   // 쓰기 타임아웃 30초
        .build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 위에서 만든 OkHttpClient 인스턴스를 Retrofit에 연결합니다.
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}