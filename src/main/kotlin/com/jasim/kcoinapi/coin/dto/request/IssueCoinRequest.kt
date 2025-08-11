package com.jasim.kcoinapi.coin.dto.request

data class IssueCoinRequest (
    val eventId: Long,
    val coinId: Long,
    val userId: String,
)