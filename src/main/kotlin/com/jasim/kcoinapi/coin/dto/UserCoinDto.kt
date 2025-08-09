package com.jasim.kcoinapi.coin.dto

import com.jasim.kcoinapi.coin.entity.UserCoinEntity

data class UserCoinDto(
    val userId: String,
    val acquiredTotal: Int,
    val balance: Int,
) {
    companion object {
        fun from(userCoinEntity: UserCoinEntity) = UserCoinDto(
            userId = userCoinEntity.userId,
            acquiredTotal = userCoinEntity.acquiredTotal,
            balance = userCoinEntity.balance
        )

    }
}