package com.jasim.kcoinapi.event.dto.request

import com.jasim.kcoinapi.common.enums.CommonEnums.EventEntryStatus

data class UserEntryRequest (
    val eventId: Long,
    val rewardId: Long,
    val userId: String,
    val entryStatus: EventEntryStatus
)