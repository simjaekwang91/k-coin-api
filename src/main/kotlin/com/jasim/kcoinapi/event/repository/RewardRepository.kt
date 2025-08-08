package com.jasim.kcoinapi.event.repository

import com.jasim.kcoinapi.event.entity.RewardEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RewardRepository: JpaRepository<RewardEntity, Long> {
}