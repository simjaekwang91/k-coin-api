package com.jasim.kcoinapi.integration

import com.jasim.kcoinapi.KCoinApiApplication
import com.jasim.kcoinapi.event.dto.RewardEntryDto
import com.jasim.kcoinapi.event.dto.UserEntryDto
import com.jasim.kcoinapi.coin.dto.request.IssueCoinRequest
import com.jasim.kcoinapi.common.dto.response.ApiResponse
import com.jasim.kcoinapi.common.enums.CommonEnums.EventEntryStatus
import com.jasim.kcoinapi.event.dto.request.UserEntryRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.test.context.ActiveProfiles
import java.net.URI

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

    @Autowired lateinit var ds: javax.sql.DataSource
    @BeforeEach
    fun poke() {
        println("** DS URL = " + (ds as com.zaxxer.hikari.HikariDataSource).jdbcUrl)
    }

    @Test
    @DisplayName("리워드 응모 현황 검증")
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
        //유저: 1001=ENTERED, 1002=CANCELLED → Entered 상태의 값은 한개
        assertThat(data.entryCount).isEqualTo(1)
    }

    @Test
    @DisplayName("사용자 리워드 응모 현황 검증 - 1001")
    fun `사용자 응모 현황 조회 - 1001`() {
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
        //응모 정보가 있기에 리스트가 null 혹은 빈값이 되면 안된다.
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
    @DisplayName("사용자 리워드 응모 현황 검증 - 1002")
    fun `사용자 응모 현황 조회 - 1002`() {
        val typeRef = object : ParameterizedTypeReference<ApiResponse<UserEntryDto>>() {}
        val res = restTemplate.exchange(
            url("/v1/event/user-reward-entry-summary/1/1/1002"),
            HttpMethod.GET,
            null,
            typeRef
        )

        //리워드 취소 목록이 정상적으로 조회 되는지?
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
    @DisplayName("응모 및 응모 취소 로직 검증")
    fun `응모-취소 검증`() {
        // 1003은 시드에서 코인 0 → 먼저 1개 발급(응모 1개 필요)
        val req = IssueCoinRequest(eventId = 1L, coinId = 1L, userId = "1003")

        val request = RequestEntity
            .post(URI.create(url("/v1/coins/issue-coin")))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(req)

        val typeRefIssueCoin = object : ParameterizedTypeReference<ApiResponse<Boolean>>() {}
        restTemplate.exchange(request, typeRefIssueCoin)

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
            val entryRequestBody =
                UserEntryRequest(eventId = 1L, rewardId = 1L, userId = "1003", entryStatus = EventEntryStatus.ENTERED)

            val entryRequest = RequestEntity
                .post(URI.create(url("/v1/event/entry-reward")))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(entryRequestBody)

            val typeRef = object : ParameterizedTypeReference<ApiResponse<Boolean>>() {}
            val res = restTemplate.exchange(entryRequest, typeRef)

            assertThat(res.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(res.body!!.status).isEqualTo(HttpStatus.OK.name)
            assertThat(res.body!!.data).isTrue()
        }

        // 응모 후 카운트 2로 증가
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
            val entryRequestBody =
                UserEntryRequest(eventId = 1L, rewardId = 1L, userId = "1003", entryStatus = EventEntryStatus.CANCELLED)

            val entryRequest = RequestEntity
                .post(URI.create(url("/v1/event/entry-reward")))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(entryRequestBody)

            val typeRef = object : ParameterizedTypeReference<ApiResponse<Boolean>>() {}
            val res = restTemplate.exchange(entryRequest, typeRef)

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