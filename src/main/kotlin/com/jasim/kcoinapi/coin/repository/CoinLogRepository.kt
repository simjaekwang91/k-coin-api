package com.jasim.kcoinapi.coin.repository

import com.jasim.kcoinapi.coin.entity.CoinLogEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CoinLogRepository: JpaRepository<CoinLogEntity, Long> {
}