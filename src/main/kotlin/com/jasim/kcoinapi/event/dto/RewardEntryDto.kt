package com.jasim.kcoinapi.event.dto

data class RewardEntryDto (
    val rewardName: String,
    val totalEntryCount: Long,
    val canceledCount: Long,
    val uniqueEntryCount: Long,
)