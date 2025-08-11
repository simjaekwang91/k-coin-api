package com.jasim.kcoinapi.event.service

import com.jasim.kcoinapi.event.dto.RewardEntryDto
import com.jasim.kcoinapi.event.dto.UserEntryDto

interface EventQueryService {
    //휴가 쿠폰별 전체 응모 현황 조회
    fun getAllEntryInfoByReward(eventId:Long, rewardId: Long): RewardEntryDto
    //사용자 응모 현황 조회
    fun getUserEntryInfo(eventId:Long, rewardId:Long, userId: String): UserEntryDto

}