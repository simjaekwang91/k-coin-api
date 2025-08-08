package com.jasim.kcoinapi.repository

import com.jasim.kcoinapi.event.entity.RewardEntity
import com.jasim.kcoinapi.event.repository.RewardRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.temporal.ChronoUnit

@ActiveProfiles("test")
@DataJpaTest
class RewardRepositoryTest {
    @Autowired
    lateinit var rewardRepository: RewardRepository
    lateinit var baseReward: RewardEntity
    lateinit var name: String
    var quota: Int = 0
    var coins: Int = 0

    @BeforeEach
    fun setUp() {
        name = "1일 휴가권"
        quota = 3
        coins = 1
        baseReward = RewardEntity(
            pRewardName = name,
            pWinningQuota = quota,
            pRequiredCoins = coins
        )
    }

    @Test
    @DisplayName("저장 및 조회: 기본 필드와 연관관계, Audit 검증")
    fun `저장_조회_테스트`() {
        // given
        val before = Instant.now()

        // when
        val saved = rewardRepository.saveAndFlush(baseReward)
        val fetched = rewardRepository.findById(saved.id!!).get()
        val after = Instant.now()

        // then — ID 및 필드
        assertThat(fetched.id)

            .isNotNull()
        assertThat(fetched.rewardName)
            .isEqualTo(name)
        assertThat(fetched.winningQuota)
            .isEqualTo(quota)
        assertThat(fetched.requiredCoins)
            .isEqualTo(coins)

        // then — 연관관계 기본값
        assertThat(fetched.entries)
            .`as`("저장 시 응모 내역 리스트는 빈 리스트여야 한다")
            .isEmpty()
        assertThat(fetched.event)
            .`as`("event는 설정되지 않았으므로 null이어야 한다")
            .isNull()

        // then — Audit 타임스탬프
        assertThat(fetched.audit.createdAt)
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
        assertThat(fetched.audit.updatedAt)
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
    }
}