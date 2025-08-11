package com.jasim.kcoinapi.event.controller

import com.jasim.kcoinapi.coin.dto.response.ApiResponse
import com.jasim.kcoinapi.common.enums.CommonEnums.EventEntryStatus
import com.jasim.kcoinapi.event.service.EventCommandService
import com.jasim.kcoinapi.event.service.EventQueryService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/event"])
class EventController(
    private val eventQueryService: EventQueryService,
    private val eventCommandService: EventCommandService
) {

    @Operation(
        summary = "휴가 쿠폰 응모/취소 0은 응모 1은 취소"
    )
    @PostMapping("/entry-reward/{eventId}/{rewardId}/{userId}")
    fun entryReward(
        @PathVariable eventId: Long,
        @PathVariable rewardId: Long,
        @PathVariable userId: String,
        @RequestParam("status") status: EventEntryStatus
    ) = ApiResponse(HttpStatus.OK.name, eventCommandService.entryReward(eventId, rewardId, userId, status))

    @Operation(
        summary = "휴가 쿠폰별 전체 응모 현황 조회"
    )
    @GetMapping("/reward-entry-summary/{eventId}/{rewardId}")
    fun getRewardEntries(
        @PathVariable eventId: Long,
        @PathVariable rewardId: Long,
    ) = ApiResponse(HttpStatus.OK.name, eventQueryService.getAllEntryInfoByReward(eventId, rewardId))

    @Operation(
        summary = "사용자 응모 현황 조회"
    )
    @GetMapping("/user-reward-entry-summary/{eventId}/{rewardId}/{userId}")
    fun getUserRewardEntries(
        @PathVariable eventId: Long,
        @PathVariable rewardId: Long,
        @PathVariable userId: String
    ) = ApiResponse(HttpStatus.OK.name, eventQueryService.getUserEntryInfo(eventId, rewardId, userId))


}