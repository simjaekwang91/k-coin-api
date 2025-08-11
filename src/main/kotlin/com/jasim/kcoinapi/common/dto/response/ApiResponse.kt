package com.jasim.kcoinapi.common.dto.response

data class ApiResponse <T> (
    val status: String,
    val data: T?
)


