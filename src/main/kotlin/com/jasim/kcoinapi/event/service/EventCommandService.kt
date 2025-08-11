package com.jasim.kcoinapi.event.service

import com.jasim.kcoinapi.common.enums.CommonEnums.EventEntryStatus

interface EventCommandService {
    //휴가 쿠폰 응모 OR 취소
    fun entryReward(eventId: Long, rewardId: Long, userId: String, status: EventEntryStatus): Boolean
}