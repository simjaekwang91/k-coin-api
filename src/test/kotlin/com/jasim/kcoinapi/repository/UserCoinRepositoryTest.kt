package com.jasim.kcoinapi.repository

import com.jasim.kcoinapi.coin.entity.UserCoinEntity
import com.jasim.kcoinapi.coin.repository.UserCoinRepository
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

@ActiveProfiles("test")
@DataJpaTest
class UserCoinRepositoryTest() {
    @Autowired
    lateinit var userCoinRepository: UserCoinRepository
    lateinit var baseEntity: UserCoinEntity

    @BeforeEach
    fun setUp() {
        baseEntity = UserCoinEntity(
            pUserId = "user123",
            pBalance = 2,
            pAcquiredTotal = 5
        )
    }

    @Test
    @DisplayName("저장 시 기본 필드 및 Audit(created/updated) 자동 설정 확인")
    fun `audit 타임스탬프 체크`() {
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
    fun `코인 응모시 정상적으로 1감소 테스트`() {
        // given
        val persisted = userCoinRepository.saveAndFlush(baseEntity)

        // when
        persisted.entryCoin()

        // then
        val found = userCoinRepository.findById(persisted.id!!).get()
        assertThat(found.balance).isEqualTo(1)
        assertThat(found.acquiredTotal).isEqualTo(5)
    }

    @Test
    @DisplayName("entryCoin(): balance 또는 acquiredTotal이 0 이면 예외 발생")
    fun `보유코인 및 사용가능 코인 유효성 동작 테스트`() {
        //given
        val uc1 = userCoinRepository.saveAndFlush(
            UserCoinEntity("u1", pBalance = 0, pAcquiredTotal = 1)
        )
        assertThrows<IllegalStateException> { uc1.entryCoin() }

        val uc2 = userCoinRepository.saveAndFlush(
            UserCoinEntity("u2", pBalance = 1, pAcquiredTotal = 0)
        )
        assertThrows<IllegalStateException> { uc2.entryCoin() }
    }

    @Test
    @DisplayName("cancelCoin(): balance만 1 감소 후 DB 반영")
    fun `취소_동작시_잔액1 증가_테스트`() {
        // given
        val persisted = userCoinRepository.saveAndFlush(baseEntity)

        // when
        persisted.cancelCoin()
        // then
        val found = userCoinRepository.findById(persisted.id!!).get()
        assertThat(found.balance).isEqualTo(3)
        assertThat(found.acquiredTotal).isEqualTo(5)
    }
}