package com.jasim.kcoinapi.coin.repository

import com.jasim.kcoinapi.coin.entity.CoinEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CoinRepository: JpaRepository<CoinEntity, Long> {
}