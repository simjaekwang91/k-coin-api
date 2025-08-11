package com.jasim.kcoinapi.repository

import com.jasim.kcoinapi.event.entity.EventEntity
import com.jasim.kcoinapi.event.entity.RewardEntity
import com.jasim.kcoinapi.event.repository.EventRepository
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
    @Autowired
    lateinit var eventRepository: EventRepository
    lateinit var baseReward: RewardEntity
    lateinit var name: String
    var quota: Int = 0
    var coins: Int = 0

    @BeforeEach
    fun setUp() {
        val event = eventRepository.save(EventEntity(
            "2025 여름휴가 이벤트",
            Instant.now(),
            Instant.now().plus(10, ChronoUnit.DAYS),
        ))

        name = "1일 휴가권"
        quota = 3
        coins = 1
        baseReward = RewardEntity(
            pRewardName = name,
            pWinningQuota = quota,
            pRequiredCoins = coins,
            event
        )
    }

    @Test
    @DisplayName("저장 및 조회 기본 필드와, Audit 검증")
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

        // then — Audit 타임스탬프
        assertThat(fetched.audit.createdAt)
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
        assertThat(fetched.audit.updatedAt)
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
    }
}