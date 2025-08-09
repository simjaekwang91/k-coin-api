package com.jasim.kcoinapi.event.service

interface EventQueryService {
    //휴가 쿠폰별 전체 응모 현황 조회
    fun getAllEntryInfo(rewardId: Long)
    //사용자 응모 현황 조회
    fun getEntryInfoByUserId(userId: String)

}