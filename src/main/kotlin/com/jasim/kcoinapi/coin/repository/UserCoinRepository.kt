package com.jasim.kcoinapi.coin.repository

import com.jasim.kcoinapi.coin.entity.UserCoinEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserCoinRepository : JpaRepository<UserCoinEntity, Long> {

    fun findByCoinId(coinId: Long): List<UserCoinEntity>?

    fun findByUserIdAndCoinId(userId: String, coinId: Long): UserCoinEntity?
}