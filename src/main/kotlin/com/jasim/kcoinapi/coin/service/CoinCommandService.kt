package com.jasim.kcoinapi.coin.service

interface CoinCommandService {
    //응모 코인 획득
    fun issueCoin(userId: String, coinId: Long, eventId: Long): Boolean

    fun issueCoinWithNoLock(userId: String, coinId: Long, eventId: Long): Boolean
}