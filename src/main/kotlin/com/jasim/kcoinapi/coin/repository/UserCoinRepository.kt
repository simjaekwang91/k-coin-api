package com.jasim.kcoinapi.coin.repository

import com.jasim.kcoinapi.coin.dto.UserCoinDto
import com.jasim.kcoinapi.coin.entity.UserCoinEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserCoinRepository : JpaRepository<UserCoinEntity, Long> {
    fun findByUserIdAndCoinId(userId: String, coinId: Long): UserCoinEntity?

    // repository
    @Query("""
  select new com.jasim.kcoinapi.coin.dto.UserCoinDto(
    uc.userId, uc.acquiredTotal, uc.balance
  )
  from UserCoinEntity uc
  where uc.userId = :userId and uc.coin.id = :coinId
""")
    fun findUserCoinSummary(userId: String, coinId: Long): UserCoinDto?

    @Query(
        """
  select new com.jasim.kcoinapi.coin.dto.UserCoinDto(uc.userId, uc.acquiredTotal, uc.balance)
  from UserCoinEntity uc
  where uc.coin.id = :coinId
"""
    )
    fun findSummariesByCoinId(coinId: Long): List<UserCoinDto>
}