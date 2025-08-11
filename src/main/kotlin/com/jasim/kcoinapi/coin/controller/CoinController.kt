package com.jasim.kcoinapi.coin.controller

import com.jasim.kcoinapi.coin.dto.CoinDto
import com.jasim.kcoinapi.coin.dto.UserCoinDto
import com.jasim.kcoinapi.coin.dto.request.IssueCoinRequest
import com.jasim.kcoinapi.common.dto.response.ApiResponse
import com.jasim.kcoinapi.coin.service.CoinCommandService
import com.jasim.kcoinapi.coin.service.CoinQueryService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/coins")
class CoinController(
    private val coinQueryService: CoinQueryService,
    private val coinCommandService: CoinCommandService
) {

    @Operation(
        summary = "전체 응모 코인 현황 조회",
    )
    @GetMapping("/summary/{coinId}")
    fun getAllCoinSummary(@PathVariable coinId: Long): ApiResponse<CoinDto> =
        ApiResponse<CoinDto>(HttpStatus.OK.name, coinQueryService.getAllCoinSummary(coinId))

    @Operation(
        summary = "사용자 응모 코인 수량 조회",
    )
    @GetMapping("/summary/{coinId}/{userId}")
    fun getUserCoinSummary(@PathVariable coinId: Long, @PathVariable userId: String) =
        ApiResponse<UserCoinDto>(HttpStatus.OK.name, coinQueryService.getUserCoinSummary(userId, coinId))

    @Operation(
        summary = "응모 코인 획득",
    )
    @PostMapping("/issue-coin")
    fun issueCoin(@RequestBody issueCoinRequest: IssueCoinRequest) =
        ApiResponse<Boolean>(
            HttpStatus.OK.name,
            coinCommandService.issueCoin(issueCoinRequest.userId, issueCoinRequest.coinId, issueCoinRequest.eventId)
        )

}