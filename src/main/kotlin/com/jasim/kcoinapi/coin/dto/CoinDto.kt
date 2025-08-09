package com.jasim.kcoinapi.coin.dto

import com.jasim.kcoinapi.coin.entity.CoinEntity
import com.jasim.kcoinapi.coin.entity.UserCoinEntity

data class CoinDto(
    val remainCoinCount: Int,
    val userCoinInfo: List<UserCoinDto>?
) {
    companion object {
        fun from(remainCoinCount: Int, userCoinEntities: List<UserCoinEntity>?) =
            CoinDto(
                remainCoinCount,
                userCoinEntities?.map {
                    UserCoinDto(
                        it.userId,
                        it.acquiredTotal,
                        it.balance
                    )
                }
            )
    }
}
