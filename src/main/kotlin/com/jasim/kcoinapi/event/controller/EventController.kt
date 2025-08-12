package com.jasim.kcoinapi.event.controller

import com.jasim.kcoinapi.common.dto.response.ApiResponse
import com.jasim.kcoinapi.event.dto.request.UserEntryRequest
import com.jasim.kcoinapi.event.service.EventCommandService
import com.jasim.kcoinapi.event.service.EventQueryService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/v1/event"])
class EventController(
    private val eventQueryService: EventQueryService,
    private val eventCommandService: EventCommandService
) {

    @Operation(
        summary = "휴가 쿠폰 응모/취소(ENTERED/CANCELLED)"
    )
    @PostMapping("/entry-reward")
    fun entryReward(
        @RequestBody userEntryRequest: UserEntryRequest
    ) = ApiResponse(
        HttpStatus.OK.name,
        eventCommandService.entryReward(
            userEntryRequest.eventId,
            userEntryRequest.rewardId,
            userEntryRequest.userId,
            userEntryRequest.entryStatus
        )
    )

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