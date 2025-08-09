package com.jasim.kcoinapi.event.repository

import com.jasim.kcoinapi.event.entity.EventEntryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface EventEntryRepository: JpaRepository<EventEntryEntity, Long> {
    fun findByRewardIdAndUserId(rewardId: Long, userId: String): EventEntryEntity?

    fun existsByRewardIdAndUserId(rewardId: Long, userId: String): Boolean
}