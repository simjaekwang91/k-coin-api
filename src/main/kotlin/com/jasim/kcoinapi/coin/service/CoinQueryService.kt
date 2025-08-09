package com.jasim.kcoinapi.coin.service

interface CoinQueryService {
    //전체 응모 코인 현황
    fun getAllCoinInfo(userId: String)
    //사용자 응모 코인 수량 조회
    fun getCoinInfoByUserId(userId: String)
}