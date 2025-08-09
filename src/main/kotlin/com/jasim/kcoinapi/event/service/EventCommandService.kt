package com.jasim.kcoinapi.event.service

interface EventCommandService {
    //휴가 쿠폰 응모
    fun entryReward(rewardId: Long, userId: String)
    //휴가 쿠폰 응모 취소
    fun cancelReward(rewardId: Long, userId: String)

}