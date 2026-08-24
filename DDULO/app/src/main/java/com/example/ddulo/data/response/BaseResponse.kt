package com.example.ddulo.data.response

// Root-level response structure
data class BaseResponse<T>(
    val result: String,
    val message: String?,
    val data: T
)