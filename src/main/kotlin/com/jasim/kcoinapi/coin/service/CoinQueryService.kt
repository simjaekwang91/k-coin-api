package com.jasim.kcoinapi.coin.service

import com.jasim.kcoinapi.coin.dto.CoinDto
import com.jasim.kcoinapi.coin.dto.UserCoinDto

interface CoinQueryService {
    //남은 응모 코인 수와 사용자별 획득한 응모 코인 수량을 조회한다
    fun getAllCoinInfo(coinId: Long): CoinDto?
    //사용자 응모 코인 수량 조회
    fun getCoinInfoByUserId(userId: String, coinId: Long): UserCoinDto?
}