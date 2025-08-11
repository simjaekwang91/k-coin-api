package com.jasim.kcoinapi.repository

import com.jasim.kcoinapi.coin.entity.CoinEntity
import com.jasim.kcoinapi.coin.repository.CoinRepository
import com.jasim.kcoinapi.event.entity.EventEntity
import com.jasim.kcoinapi.event.repository.EventRepository
import com.jasim.kcoinapi.exception.CoinException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

@ActiveProfiles("test")
@DataJpaTest
class CoinRepositoryTest {

    @Autowired
    lateinit var coinRepository: CoinRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    lateinit var baseCoin: CoinEntity

    @BeforeEach
    fun setUp() {
        val event = eventRepository.save(EventEntity(
            "2025 여름휴가 이벤트",
            Instant.now(),
            Instant.now().plus(10, ChronoUnit.DAYS),
        ))

        baseCoin = CoinEntity(
            pPerUserLimit   = 5,
            pTotalCoinCount = 100,
            pRemainCoinCount= 100,
            event
        )
    }

    @Test
    @DisplayName("저장, 조회시 정상 테스트")
    fun `저장 및 조회 테스트`() {
        // given
        val saved = coinRepository.saveAndFlush(baseCoin)

        // when
        val findEntity = coinRepository.findById(saved.id!!).get()

        // then — 기본 필드
        assertThat(findEntity.id).isNotNull
        assertThat(findEntity.perUserLimit).isEqualTo(5)
        assertThat(findEntity.totalCoinCount).isEqualTo(100)
        assertThat(findEntity.remainCoinCount).isEqualTo(100)

        // then — Audit 타임스탬프가 현재로부터 1초 이내에 설정되었는지
        assertThat(saved.audit.createdAt)
            .`as`("createdAt은 현재 시각으로부터 1초 이내여야 합니다.")
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))

        assertThat(saved.audit.updatedAt)
            .`as`("updatedAt은 현재 시각으로부터 1초 이내여야 합니다.")
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))

    }

    @Test
    @DisplayName("코인 발급 이후 잔여코인 수량 DB 저장 검증")
    fun `응모_코인_발급_테스트`() {
        // given
        val start = baseCoin.remainCoinCount
        val coin = coinRepository.saveAndFlush(baseCoin)
        // when
        coin.issueCoin()

        // then
        val found = coinRepository.findById(coin.id!!).get()
        assertThat(found.remainCoinCount).isEqualTo(start - 1)
    }

    @Test
    @DisplayName("코인 발급시 잔여 코인이 없을때 실패 검증")
    fun `응모_코인_발급_예외_테스트`() {
        var save = coinRepository.saveAndFlush(baseCoin)
        var find: CoinEntity

        repeat(100) {
            find = coinRepository.findById(save.id!!).get()
            find.issueCoin()
        }

        // when / then
        val ex = assertThrows<CoinException> {
            find = coinRepository.findById(save.id!!).get()
            find.issueCoin()
        }
        assertEquals("남은 코인 수량이 없습니다.", ex.message)
    }
}

