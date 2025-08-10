package com.jasim.kcoinapi.integration

import com.jasim.kcoinapi.KCoinApiApplication
import com.jasim.kcoinapi.coin.dto.CoinDto
import com.jasim.kcoinapi.coin.dto.RewardEntryDto
import com.jasim.kcoinapi.coin.dto.UserCoinDto
import com.jasim.kcoinapi.coin.dto.UserEntryDto
import com.jasim.kcoinapi.coin.dto.response.ApiResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode

@SpringBootTest(
    classes = [KCoinApiApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(TestcontainersConfig::class)
@ActiveProfiles("integration")
class EventApiIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    private fun url(p: String) = "http://localhost:$port$p"

    @Test
    fun `리워드1 응모 현황 - 시드 검증`() {
        val typeRef = object : ParameterizedTypeReference<ApiResponse<RewardEntryDto>>() {}
        val res = restTemplate.exchange(
            url("/v1/event/reward-entry-summary/1/1"),
            HttpMethod.GET,
            null,
            typeRef
        )

        assertThat(res.statusCode).isEqualTo(HttpStatus.OK)
        val body = res.body!!
        assertThat(body.status).isEqualTo(HttpStatus.OK.name)

        val data = body.data!!
        assertThat(data.rewardName).isEqualTo("1일 휴가권")
        // 시드: 1001=ENTERED, 1002=CANCELLED → 현재 응모 카운트는 1이라고 가정
        assertThat(data.entryCount).isEqualTo(1)
    }

    @Test
    fun `사용자 응모 현황 조회 - 1001, 리워드1 ENTERED`() {
        val typeRef = object : ParameterizedTypeReference<ApiResponse<UserEntryDto>>() {}
        val res = restTemplate.exchange(
            url("/v1/event/user-reward-entry-summary/1/1/1001"),
            HttpMethod.GET,
            null,
            typeRef
        )

        assertThat(res.statusCode).isEqualTo(HttpStatus.OK)
        val body = res.body!!
        assertThat(body.status).isEqualTo(HttpStatus.OK.name)

        val data = body.data!!
        assertThat(data.userId).isEqualTo("1001")
        assertThat(data.entries).isNotNull
        assertThat(data.entries).isNotEmpty

        val e = data.entries.first()
        assertThat(e.eventName).isEqualTo("2025 여름휴가 이벤트")
        assertThat(e.rewardName).isEqualTo("1일 휴가권")
        assertThat(e.status).isEqualTo("ENTERED")
        assertThat(e.createTime).isNotNull
        assertThat(e.updateTime).isNotNull
    }

    @Test
    fun `사용자 응모 현황 조회 - 1002, 리워드1 CANCELLED`() {
        val typeRef = object : ParameterizedTypeReference<ApiResponse<UserEntryDto>>() {}
        val res = restTemplate.exchange(
            url("/v1/event/user-reward-entry-summary/1/1/1002"),
            HttpMethod.GET,
            null,
            typeRef
        )

        assertThat(res.statusCode).isEqualTo(HttpStatus.OK)
        val body = res.body!!
        assertThat(body.status).isEqualTo(HttpStatus.OK.name)

        val data = body.data!!
        assertThat(data.userId).isEqualTo("1002")
        val e = data.entries.first()
        assertThat(e.rewardName).isEqualTo("1일 휴가권")
        assertThat(e.status).isEqualTo("CANCELLED")
    }

    @Test
    fun `응모-취소 플로우 - 1003 리워드1`() {
        // 1003은 시드에서 코인 0 → 먼저 1개 발급(응모 1개 필요)
        restTemplate.postForEntity(url("/v1/coins/issue-coin/1/1/1003"), null, Void::class.java)

        // 응모 전 카운트 확인(시드 그대로 1)
        run {
            val typeRef = object : ParameterizedTypeReference<ApiResponse<RewardEntryDto>>() {}
            val res = restTemplate.exchange(
                url("/v1/event/reward-entry-summary/1/1"),
                HttpMethod.GET,
                null,
                typeRef
            )
            assertThat(res.body!!.data!!.entryCount).isEqualTo(1)
        }

        // 응모 (status=0)
        run {
            val typeRef = object : ParameterizedTypeReference<ApiResponse<Boolean>>() {}
            val res = restTemplate.exchange(
                url("/v1/event/entry-reward/1/1/1003?status=0"),
                HttpMethod.POST,
                null,
                typeRef
            )
            assertThat(res.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(res.body!!.status).isEqualTo(HttpStatus.OK.name)
            assertThat(res.body!!.data).isTrue()
        }

        // 응모 후 카운트 2로 증가 기대
        run {
            val typeRef = object : ParameterizedTypeReference<ApiResponse<RewardEntryDto>>() {}
            val res = restTemplate.exchange(
                url("/v1/event/reward-entry-summary/1/1"),
                HttpMethod.GET,
                null,
                typeRef
            )
            assertThat(res.body!!.data!!.entryCount).isEqualTo(2)
        }

        // 사용자 상태 확인 → ENTERED
        run {
            val typeRef = object : ParameterizedTypeReference<ApiResponse<UserEntryDto>>() {}
            val res = restTemplate.exchange(
                url("/v1/event/user-reward-entry-summary/1/1/1003"),
                HttpMethod.GET,
                null,
                typeRef
            )
            val e = res.body!!.data!!.entries.first()
            assertThat(e.status).isEqualTo("ENTERED")
        }

        // 취소 (status=1)
        run {
            val typeRef = object : ParameterizedTypeReference<ApiResponse<Boolean>>() {}
            val res = restTemplate.exchange(
                url("/v1/event/entry-reward/1/1/1003?status=1"),
                HttpMethod.POST,
                null,
                typeRef
            )
            assertThat(res.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(res.body!!.status).isEqualTo(HttpStatus.OK.name)
            assertThat(res.body!!.data).isTrue()
        }

        // 취소 후 카운트 다시 1로 복귀
        run {
            val typeRef = object : ParameterizedTypeReference<ApiResponse<RewardEntryDto>>() {}
            val res = restTemplate.exchange(
                url("/v1/event/reward-entry-summary/1/1"),
                HttpMethod.GET,
                null,
                typeRef
            )
            assertThat(res.body!!.data!!.entryCount).isEqualTo(1)
        }

        // 사용자 상태 확인 → CANCELLED
        run {
            val typeRef = object : ParameterizedTypeReference<ApiResponse<UserEntryDto>>() {}
            val res = restTemplate.exchange(
                url("/v1/event/user-reward-entry-summary/1/1/1003"),
                HttpMethod.GET,
                null,
                typeRef
            )
            val e = res.body!!.data!!.entries.first()
            assertThat(e.status).isEqualTo("CANCELLED")
        }
    }
}