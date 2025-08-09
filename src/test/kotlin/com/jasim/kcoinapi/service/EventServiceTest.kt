package com.jasim.kcoinapi.service

import com.jasim.kcoinapi.coin.entity.CoinEntity
import com.jasim.kcoinapi.coin.entity.CoinLogEntity
import com.jasim.kcoinapi.coin.entity.UserCoinEntity
import com.jasim.kcoinapi.coin.repository.CoinLogRepository
import com.jasim.kcoinapi.coin.repository.CoinRepository
import com.jasim.kcoinapi.coin.repository.UserCoinRepository
import com.jasim.kcoinapi.config.LockProperties
import com.jasim.kcoinapi.event.entity.EventEntity
import com.jasim.kcoinapi.event.entity.EventEntryEntity
import com.jasim.kcoinapi.event.entity.RewardEntity
import com.jasim.kcoinapi.event.repository.EventEntryRepository
import com.jasim.kcoinapi.event.repository.EventRepository
import com.jasim.kcoinapi.event.repository.RewardRepository
import com.jasim.kcoinapi.event.service.impl.EventCommandImpl
import com.jasim.kcoinapi.event.service.impl.EventQueryImpl
import com.jasim.kcoinapi.exception.CoinException
import com.jasim.kcoinapi.exception.EventException
import com.jasim.kcoinapi.exception.EventException.EventErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.jvm.java

@ActiveProfiles("test")
@DataJpaTest
@Import(EventCommandImpl::class, LockProperties::class, EventQueryImpl::class) // 설정 빈 등록
@TestPropertySource(properties = ["dblock.lock-key=global"]) // 값 주입
class EventServiceTest {

    @Autowired
    lateinit var eventCommandService: EventCommandImpl

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var rewardRepository: RewardRepository

    @Autowired
    lateinit var eventEntryRepository: EventEntryRepository

    @Autowired
    lateinit var coinRepository: CoinRepository

    @Autowired
    lateinit var userCoinRepository: UserCoinRepository

    @Autowired
    lateinit var coinLogRepository: CoinLogRepository

    @Autowired
    lateinit var jdbc: org.springframework.jdbc.core.JdbcTemplate

    private lateinit var event: EventEntity
    private lateinit var coin: CoinEntity
    private lateinit var reward: RewardEntity
    private lateinit var userCoin: UserCoinEntity
    private lateinit var entry: EventEntryEntity
    private val userId = "testUser"

    @BeforeEach
    fun setup() {
        // 락 키 준비 (스키마에 기본 키를 넣었다면 생략 가능)
        jdbc.update("MERGE INTO process_lock (lock_key) KEY(lock_key) VALUES (?)", "global")

        event = eventRepository.save(
            EventEntity(
                "2025 여름휴가 이벤트", Instant.now(), Instant.now().plus(7, ChronoUnit.DAYS)
            )
        )

        coin = coinRepository.saveAndFlush(
            CoinEntity(
                pPerUserLimit = 5,
                pTotalCoinCount = 100,
                pRemainCoinCount = 100,
                pEvent = event
            )
        )

        // requiredCoins = 2 짜리 리워드
        reward = rewardRepository.save(
            RewardEntity(
                pRewardName = "1일 휴가권",
                pWinningQuota = 10,
                pRequiredCoins = 2,
                pEvent = event,
            )
        )

        // 유저 코인: balance 3 / 총 3
        userCoin = userCoinRepository.save(
            UserCoinEntity(
                pUserId = userId,
                pBalance = 3,
                pAcquiredTotal = 3,
                pCoinInfo = coin
            )
        )

    }

    @Test
    @DisplayName("응모 성공: 잔액 감소, 엔트리 생성, 로그 기록")
    fun enter_success() {
        //given
        val beforeBalance = userCoinRepository.findByUserIdAndCoinId(userId, coin.id!!)!!.balance

        // when
        eventCommandService.setReward(event.id!!, reward.id!!, userId, 0)

        // then
        val after = userCoinRepository.findByUserIdAndCoinId(userId, coin.id!!)!!
        assertThat(after.balance).isEqualTo(beforeBalance - reward.requiredCoins)

        val entry = eventEntryRepository.findByRewardIdAndUserId(reward.id!!, userId)
        assertThat(entry).isNotNull
        assertThat(entry!!.status).isEqualTo(EventEntryEntity.EntryStatus.ENTERED)

        val logs = coinLogRepository.findAll()
        assertThat(logs).hasSize(1)
        assertThat(logs[0].reason).isEqualTo(CoinLogEntity.Reason.ENTERED)
        assertThat(logs[0].useAmount).isEqualTo(reward.requiredCoins)
    }

    @Test
    @DisplayName("중복 응모 시 에러(ALREADY_ENTERED)")
    fun enter_alreadyEntered_fail() {
        //given
        eventCommandService.setReward(event.id!!, reward.id!!, userId, EventEntryEntity.EntryStatus.ENTERED.code)

        assertThatThrownBy {
            eventCommandService.setReward(event.id!!, reward.id!!, userId, 0)
        }
            .isInstanceOf(EventException::class.java)
            .hasMessageContaining(EventErrorType.ALREADY_ENTERED.errorMessage)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("취소 성공: 잔액 복구, 엔트리 상태 CANCELLED, 로그 기록")
    fun cancel_success() {
        // 응모로 코인 2 소모
        eventCommandService.setReward(event.id!!, reward.id!!, userId, 0)

        val mid = userCoinRepository.findByUserIdAndCoinId(userId, coin.id!!)!!
        // when: 취소
        eventCommandService.setReward(event.id!!, reward.id!!, userId, 1)

        // then
        val after = userCoinRepository.findByUserIdAndCoinId(userId, coin.id!!)!!
        //취소후 잔액은 응모 이후 + 요구 코인 수량과 같아야 한다
        assertThat(after.balance).isEqualTo(mid.balance + reward.requiredCoins)

        val entry = eventEntryRepository.findByRewardIdAndUserId(reward.id!!, userId)
        assertThat(entry).isNotNull
        assertThat(entry!!.status).isEqualTo(EventEntryEntity.EntryStatus.CANCELLED)

        val logs = coinLogRepository.findAll()
        assertThat(logs.any { it.reason == CoinLogEntity.Reason.CANCELLED }).isTrue()
    }

    @Test
    @DisplayName("잘못된 status 코드 → NOT_FOUND_TYPE 예외")
    fun invalid_status_code_fails() {
        assertThatThrownBy {
            eventCommandService.setReward(event.id!!, reward.id!!, userId, 999)
        }
            .isInstanceOf(EventException::class.java)
            .hasMessageContaining(EventErrorType.NOT_FOUND_TYPE.errorMessage)
    }

    @Test
    @DisplayName("사용자 코인 없음 → NOT_FOUND_USER_COIN")
    fun no_user_coin_fails() {
        // 다른 유저
        assertThatThrownBy {
            eventCommandService.setReward(event.id!!, reward.id!!, "someone-else", 0)
        }
            .isInstanceOf(CoinException::class.java)
            .hasMessageContaining(CoinException.CoinErrorType.NOT_FOUND_USER_COIN.errorMessage)
    }
}