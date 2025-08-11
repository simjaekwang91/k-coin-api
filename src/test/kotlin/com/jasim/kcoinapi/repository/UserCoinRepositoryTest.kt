package com.jasim.kcoinapi.repository

import com.jasim.kcoinapi.coin.entity.CoinEntity
import com.jasim.kcoinapi.coin.entity.UserCoinEntity
import com.jasim.kcoinapi.coin.repository.CoinRepository
import com.jasim.kcoinapi.coin.repository.UserCoinRepository
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

@ActiveProfiles("test")
@DataJpaTest
class UserCoinRepositoryTest() {
    @Autowired
    lateinit var userCoinRepository: UserCoinRepository
    @Autowired
    lateinit var coinRepository: CoinRepository
    @Autowired
    lateinit var eventRepository: EventRepository
    lateinit var baseEntity: UserCoinEntity
    lateinit var coinEntity: CoinEntity
    lateinit var eventEntity: EventEntity

    @BeforeEach
    fun setUp() {
        eventEntity = eventRepository.save(EventEntity(
            "2025 여름휴가 이벤트",
            Instant.now(),
            Instant.now().plus(10, ChronoUnit.DAYS),
        ))

        coinEntity = coinRepository.save(CoinEntity(
            pPerUserLimit   = 5,
            pTotalCoinCount = 100,
            pRemainCoinCount= 100,
            pEvent = eventEntity
        ))

        baseEntity = UserCoinEntity(
            pUserId = "user123",
            pBalance = 2,
            pAcquiredTotal = 5,
            pCoinInfo = coinEntity
        )
    }

    @Test
    @DisplayName("저장 시 기본 필드 및 Audit 확인")
    fun `저장_조회_테스트`() {
        // given
        val before = Instant.now()

        // when
        val saved = userCoinRepository.saveAndFlush(baseEntity)
        val findEntity = userCoinRepository.findById(saved.id!!).get()

        // then
        assertThat(findEntity.audit.createdAt)
            .`as`("createdAt은 현재 시각으로부터 1초 이내여야 합니다.")
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))

        assertThat(findEntity.audit.updatedAt)
            .`as`("updatedAt은 현재 시각으로부터 1초 이내여야 합니다.")
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
    }

    @Test
    @DisplayName("코인 응모 이후 값이 정상적으로 감소 하여 저장되는지 검증")
    fun `코인 응모시 정상적으로 1감소 테스트`() {
        // given
        val persisted = userCoinRepository.saveAndFlush(baseEntity)

        // when
        persisted.entryCoin(1)

        // then
        val found = userCoinRepository.findById(persisted.id!!).get()
        assertThat(found.balance).isEqualTo(1)
        assertThat(found.acquiredTotal).isEqualTo(5)
    }

    @Test
    @DisplayName("보유 코인으로 응모시 잔여코인이 응모 요구량보다 많은 경우 실패 테스트")
    fun `보유코인 및 사용가능 코인 유효성 동작 테스트`() {
        //given
        val uc2 = userCoinRepository.saveAndFlush(
            UserCoinEntity("u2", pBalance = 1, pAcquiredTotal = 3, pCoinInfo = coinEntity)
        )
        assertThrows<CoinException> { uc2.entryCoin(2) }
    }

    @Test
    @DisplayName("응모 취소 동작시 잔여 코인 수량 1 감소 후 DB 반영 테스트")
    fun `취소_동작시_잔액1 증가_테스트`() {
        // given
        val persisted = userCoinRepository.saveAndFlush(baseEntity)

        // when
        persisted.cancelCoin(1)
        // then
        val found = userCoinRepository.findById(persisted.id!!).get()
        assertThat(found.balance).isEqualTo(3)
        assertThat(found.acquiredTotal).isEqualTo(5)
    }
}