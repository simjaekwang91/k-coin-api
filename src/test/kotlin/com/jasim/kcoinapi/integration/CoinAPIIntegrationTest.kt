package com.jasim.kcoinapi.integration

import com.jasim.kcoinapi.KCoinApiApplication
import com.jasim.kcoinapi.coin.dto.CoinDto
import com.jasim.kcoinapi.coin.dto.UserCoinDto
import com.jasim.kcoinapi.coin.dto.request.IssueCoinRequest
import com.jasim.kcoinapi.common.dto.response.ApiResponse
import com.jasim.kcoinapi.integration.config.TestcontainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
class CoinApiIntegrationTest {

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
    fun `응모 코인 발급 성공`() {
        val req = IssueCoinRequest(eventId = 1L, coinId = 1L, userId = "1001")

        val request = RequestEntity
            .post(URI.create(url("/v1/coins/issue-coin")))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(req)

        val typeRef = object : ParameterizedTypeReference<ApiResponse<Boolean>>() {}
        val res = restTemplate.exchange(request, typeRef)

        assertThat(res.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(res.body!!.status).isEqualTo(HttpStatus.OK.name)
        assertThat(res.body!!.data).isTrue()
    }

    @Test
    fun `전체 응모 코인 현황 조회`() {
        val typeRef = object : ParameterizedTypeReference<ApiResponse<CoinDto>>() {}
        val res = restTemplate.exchange(
            url("/v1/coins/summary/1"),
            HttpMethod.GET,
            null,
            typeRef
        )

        assertThat(res.statusCode).isEqualTo(HttpStatus.OK)
        val body = res.body!!
        assertThat(body.status).isEqualTo(HttpStatus.OK.name)

        val data = body.data!!
        // 남은 수량은 0 이상
        assertThat(data.remainCoinCount).isGreaterThanOrEqualTo(0)

        // 유저 목록 존재
        val users = data.userCoinInfo
        assertThat(users).isNotNull
        assertThat(users).isNotEmpty

        // 조회 편의용 map
        val byId = users!!.associateBy { it.userId }

        // 시드에 넣은 유저들이 포함되어야 함(1001,1002,1003,1004)
        assertThat(byId.keys).contains("1001", "1002", "1003", "1004")

        // 1003은 시드에서 balance/acquiredTotal 모두 0 인지 검증
        val u1003 = byId["1003"]!!
        assertThat(u1003.acquiredTotal).isEqualTo(0)
        assertThat(u1003.balance).isEqualTo(0)

        // 전체 유저 공통 불변식 검증
        //  - 획득 코인은 음수가 될 수 없다
        //  - 현재 잔여 코인은 누적 획득량을 초과할 수 없음
        //  - 누적 획득량은 사용자 최대 한도(시드: 3)를 넘지 않음
        val PER_USER_LIMIT = 3
        users.forEach {
            assertThat(it.acquiredTotal).isGreaterThanOrEqualTo(0)
            assertThat(it.balance).isGreaterThanOrEqualTo(0)
            assertThat(it.acquiredTotal).isGreaterThanOrEqualTo(it.balance)
            assertThat(it.acquiredTotal).isLessThanOrEqualTo(PER_USER_LIMIT)
        }
    }

    @Test
    fun `사용자 응모 코인 수량 조회`() {
        // 사전 발급 한 번 해두고 (신규 유저)
        val req = IssueCoinRequest(eventId = 1L, coinId = 1L, userId = "testUser")

        val request = RequestEntity
            .post(URI.create(url("/v1/coins/issue-coin")))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(req)

        val typeRefIssueCoin = object : ParameterizedTypeReference<ApiResponse<Boolean>>() {}
        restTemplate.exchange(request, typeRefIssueCoin)

        val typeRef = object : ParameterizedTypeReference<ApiResponse<UserCoinDto>>() {}
        val res = restTemplate.exchange(
            url("/v1/coins/summary/1/testUser"),
            HttpMethod.GET,
            null,
            typeRef
        )

        assertThat(res.statusCode).isEqualTo(HttpStatus.OK)
        val body = res.body!!
        assertThat(body.status).isEqualTo(HttpStatus.OK.name)
        val data = body.data!!
        // 상세 값 검증
        assertThat(data.userId).isEqualTo("testUser")

        // 현재 구현(신규 발급 2회 카운트) 기준 기대값: 2
        // 만약 신규 발급 시 중복 카운팅을 수정했다면 기대값을 1로 변경하세요.
        assertThat(data.balance).isEqualTo(1)
        assertThat(data.acquiredTotal).isEqualTo(1)

        // 불변식 검증(사용되면 balance <= acquiredTotal 이어야 함)
        assertThat(data.balance).isGreaterThanOrEqualTo(0)
        assertThat(data.acquiredTotal).isGreaterThanOrEqualTo(data.balance)

        // 상한선 검증(퍼 유저 한도 3이 기본 데이터임)
        assertThat(data.acquiredTotal).isLessThanOrEqualTo(3)
    }
}