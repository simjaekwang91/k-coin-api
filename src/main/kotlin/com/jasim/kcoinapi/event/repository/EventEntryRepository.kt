package com.jasim.kcoinapi.event.repository

import com.jasim.kcoinapi.event.entity.EventEntryEntity
import com.jasim.kcoinapi.event.entity.EventEntryEntity.EntryStatus
import org.springframework.data.jpa.repository.JpaRepository

interface EventEntryRepository: JpaRepository<EventEntryEntity, Long> {
    fun findByRewardIdAndStatus(rewardId:Long, status: EntryStatus): List<EventEntryEntity>?
    fun findByRewardIdAndUserId(rewardId: Long, userId: String): List<EventEntryEntity>?
    fun findByRewardIdAndUserIdAndStatus(rewardId: Long, userId: String, status: EntryStatus): EventEntryEntity?
    fun existsByRewardIdAndUserIdAndStatus(rewardId: Long, userId: String, status: EntryStatus): Boolean
}