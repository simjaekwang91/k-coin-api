package com.jasim.kcoinapi.event.service

interface EventCommandService {
    //휴가 쿠폰 응모 OR 취소
    //status 0 응모 1 취소
    fun setReward(eventId: Long, rewardId: Long, userId: String, status: Int): Boolean
}