package com.jasim.kcoinapi.repository

import com.jasim.kcoinapi.coin.entity.CoinEntity
import com.jasim.kcoinapi.coin.repository.CoinRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

@ActiveProfiles("test")
@DataJpaTest
class CoinRepositoryTest {

    @Autowired
    lateinit var coinRepository: CoinRepository

    lateinit var baseCoin: CoinEntity

    @BeforeEach
    fun setUp() {
        baseCoin = CoinEntity(
            pPerUserLimit   = 5,
            pTotalCoinCount = 100,
            pRemainCoinCount= 100
        )
    }

    @Test
    fun `저장 및 조회 테스트`() {
        // given
        val before = Instant.now()
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
    @DisplayName("decrementCoinCount() 호출 후, DB에 반영된다")
    fun `응모_코인_할당_테스트`() {
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
    @DisplayName("decrementCoinCount() 는 남은 코인이 0일 때 예외를 던진다")
    fun `응모_코인_할당_예외_테스트`() {
        var save = coinRepository.saveAndFlush(baseCoin)
        var find: CoinEntity

        repeat(100) {
            find = coinRepository.findById(save.id!!).get()
            find.issueCoin()
        }

        // when / then
        val ex = assertThrows<IllegalStateException> {
            find = coinRepository.findById(save.id!!).get()
            find.issueCoin()
        }
        assertEquals("남은 코인 수량이 없습니다.", ex.message)
    }
}

