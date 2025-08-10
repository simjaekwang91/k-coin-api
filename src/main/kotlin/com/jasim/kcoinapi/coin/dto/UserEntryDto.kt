package com.jasim.kcoinapi.coin.dto

import com.jasim.kcoinapi.event.entity.EventEntryEntity
import java.time.Instant

data class UserEntryDto (
    val userId: String,
    val entries: List<UserEntryDetail>
) {
    companion object {
        fun from(eventEntry: EventEntryEntity) = UserEntryDetail(
            eventEntry.reward.event.eventName,
            eventEntry.reward.rewardName,
            eventEntry.status.toString(),
            eventEntry.audit.createdAt!!,
            eventEntry.audit.updatedAt!!
        )
    }
}

data class UserEntryDetail (
    val eventName: String,
    val rewardName: String,
    val status: String,
    val createTime: Instant,
    val updateTime: Instant
)