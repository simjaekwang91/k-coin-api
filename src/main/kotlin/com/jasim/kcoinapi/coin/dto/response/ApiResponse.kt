package com.jasim.kcoinapi.coin.dto.response

data class ApiResponse <T> (
    val status: String,
    val data: T?
)


