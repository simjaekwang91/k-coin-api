package com.jasim.kcoinapi.repository
import com.jasim.kcoinapi.coin.repository.CoinLogRepository
import com.jasim.kcoinapi.coin.entity.CoinLogEntity
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
@DisplayName("CoinLogRepository JPA 테스트")
class CoinLogRepositoryTest (
) {
    @Autowired
    lateinit var coinLogRepository: CoinLogRepository

    lateinit var baseLog: CoinLogEntity
    lateinit var userId: String
    var coinId: Long = 0
    var amount: Int = 0
    lateinit var reason: CoinLogEntity.Reason
    var eventEntryId: Long? = null

    @BeforeEach
    fun setUp() {
        userId = "user1"
        coinId = 42L
        amount = 5
        reason = CoinLogEntity.Reason.ISSUE
        eventEntryId = 100L
        baseLog = CoinLogEntity(
            pUserId = userId,
            pCoinId = coinId,
            pAmount = amount,
            pReason = reason,
            pEventEntryId = eventEntryId
        )
    }

    @Test
    @DisplayName("저장 및 조회: 필드 및 Audit(createdAt/updatedAt) 검증")
    fun `저장_조회_테스트`() {
        // given
        val saved = coinLogRepository.saveAndFlush(baseLog)

        // when
        val find = coinLogRepository.findById(saved.id!!).get()

        // then
        assertThat(find.id)
            .isNotNull()
        assertThat(find.userId)
            .isEqualTo(userId)
        assertThat(find.coinId)
            .isEqualTo(coinId)
        assertThat(find.amount)
            .isEqualTo(amount)
        assertThat(find.reason)
            .isEqualTo(reason)
        assertThat(find.eventEntryId)
            .isEqualTo(eventEntryId)

        // then
        assertThat(find.audit.createdAt)
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
        assertThat(find.audit.updatedAt)
            .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
    }
}