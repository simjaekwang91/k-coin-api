package com.jasim.kcoinapi.coin.service

interface CoinCommandSerivce {
    //응모 코인 획득
    fun issueCoin(userId: String, coinId: Long): Boolean
}