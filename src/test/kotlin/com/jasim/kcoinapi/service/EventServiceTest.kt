package com.jasim.kcoinapi.service

import com.jasim.kcoinapi.coin.entity.CoinEntity
import com.jasim.kcoinapi.coin.entity.CoinLogEntity
import com.jasim.kcoinapi.coin.entity.UserCoinEntity
import com.jasim.kcoinapi.coin.repository.CoinLogRepository
import com.jasim.kcoinapi.coin.repository.CoinRepository
import com.jasim.kcoinapi.coin.repository.UserCoinRepository
import com.jasim.kcoinapi.common.entity.ProcessLockEntity
import com.jasim.kcoinapi.common.enums.CommonEnums.EventEntryStatus
import com.jasim.kcoinapi.common.repository.ProcessLockRepository
import com.jasim.kcoinapi.config.LockProperties
import com.jasim.kcoinapi.event.entity.EventEntity
import com.jasim.kcoinapi.event.entity.EventEntryEntity
import com.jasim.kcoinapi.event.entity.EventEntryEntity.EntryStatus
import com.jasim.kcoinapi.event.entity.RewardEntity
import com.jasim.kcoinapi.event.repository.EventEntryRepository
import com.jasim.kcoinapi.event.repository.RewardRepository
import com.jasim.kcoinapi.event.service.impl.EventCommandImpl
import com.jasim.kcoinapi.exception.CoinException
import com.jasim.kcoinapi.exception.DBException
import com.jasim.kcoinapi.exception.EventException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.quality.Strictness
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.Optional

@ActiveProfiles("test")
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT) // 필요 시 제거 가능
class EventServiceTest {

    @Mock lateinit var eventEntryRepository: EventEntryRepository
    @Mock lateinit var rewardRepository: RewardRepository
    @Mock lateinit var coinRepository: CoinRepository
    @Mock lateinit var userCoinRepository: UserCoinRepository
    @Mock lateinit var lockRepository: ProcessLockRepository
    @Mock lateinit var coinLogRepository: CoinLogRepository
    @Mock lateinit var lockProperties: LockProperties

    @InjectMocks lateinit var service: EventCommandImpl

    private val userId = "u1"
    private val eventId = 10L
    private val rewardId = 20L
    private val coinId = 30L

    private lateinit var eventFixture: EventEntity
    private lateinit var rewardFixture: RewardEntity
    private lateinit var coinFixture: CoinEntity
    private lateinit var userCoinFixture: UserCoinEntity

    @BeforeEach
    fun setup() {
        // --- 도메인 픽스처 ---
        eventFixture = EventEntity("이벤트", Instant.now(), Instant.now().plusSeconds(3600))
        rewardFixture = RewardEntity(
            pRewardName = "1일 휴가권",
            pWinningQuota = 10,
            pRequiredCoins = 2,
            pEvent = eventFixture
        )
        coinFixture = CoinEntity(
            pPerUserLimit = 5,
            pTotalCoinCount = 100,
            pRemainCoinCount = 100,
            pEvent = eventFixture
        )
        userCoinFixture = UserCoinEntity(
            pUserId = userId,
            pBalance = 3,
            pAcquiredTotal = 3,
            pCoinInfo = coinFixture
        )

        setId(eventFixture, eventId)
        setId(rewardFixture, rewardId)
        setId(coinFixture,   coinId)

        whenever(lockProperties.lockKey).thenReturn("global")
        doReturn(ProcessLockEntity()).`when`(lockRepository).lockWithTimeout(any())

        whenever(rewardRepository.findById(eq(rewardId))).thenReturn(Optional.of(rewardFixture))
        whenever(coinRepository.findByEventId(eq(eventId))).thenReturn(coinFixture)
        whenever(userCoinRepository.findByUserIdAndCoinId(eq(userId), eq(coinId)))
            .thenReturn(userCoinFixture)

        // 기본: 아직 응모 안 함
        whenever(eventEntryRepository.existsByRewardIdAndUserIdAndStatus(eq(rewardId), eq(userId), eq(EntryStatus.ENTERED)))
            .thenReturn(false)
        // 취소 분기에서만 사용, 기본은 null
        whenever(eventEntryRepository.findByRewardIdAndUserId(eq(rewardId), eq(userId)))
            .thenReturn(null)

        // save(...)는 받은 객체를 그대로 반환 (Kotlin null-safety 회피)
        doAnswer { it.getArgument<EventEntryEntity>(0) }
            .`when`(eventEntryRepository).save(any())
        doAnswer { it.getArgument<CoinLogEntity>(0) }
            .`when`(coinLogRepository).save(any())
    }

    @Test
    @DisplayName("응모 성공 → 잔액 감소 테스트")
    fun `응모 성공 테스트`() {
        // when
        service.entryReward(eventId, rewardId, userId, EventEntryStatus.ENTERED)

        // then: 3 -> 1 (requiredCoins = 2)
        assertThat(userCoinFixture.balance).isEqualTo(1)
        verify(eventEntryRepository).existsByRewardIdAndUserIdAndStatus(eq(rewardId), eq(userId), eq(EntryStatus.ENTERED))
        verify(eventEntryRepository).save(any())
        verify(coinLogRepository).save(any())
    }

    @Test
    @DisplayName("중복 응모 → 중복 응모 실패 테스트")
    fun `중복 응모 실패 검증 테스트`() {
        // given: 이미 응모한 상태
        whenever(eventEntryRepository.existsByRewardIdAndUserIdAndStatus(eq(rewardId), eq(userId), eq(EntryStatus.ENTERED)))
            .thenReturn(true)

        // then
        assertThatThrownBy {
            service.entryReward(eventId, rewardId, userId, EventEntryStatus.ENTERED)
        }
            .isInstanceOf(EventException::class.java)
            .hasMessageContaining("이미 응모 되었습니다. 중복 응모는 불가능 합니다.")

        verify(eventEntryRepository).existsByRewardIdAndUserIdAndStatus(eq(rewardId), eq(userId), eq(EntryStatus.ENTERED))
        verify(eventEntryRepository, never()).save(any())
        verify(coinLogRepository,  never()).save(any())
    }

    @Test
    @DisplayName("응모 취소 성공 → 코인 복구 검증")
    fun `응모 취소시 잔액 정상적으로 반영 되는지 검증 테스트`() {
        // given: 이미 한 번 응모해서 현재 잔액이 1
        val afterEnter = UserCoinEntity(
            pUserId = userId, pBalance = 1, pAcquiredTotal = 3, pCoinInfo = coinFixture
        )
        whenever(userCoinRepository.findByUserIdAndCoinId(eq(userId), eq(coinId)))
            .thenReturn(afterEnter)

        // 취소 시에는 기존 엔트리 하나가 있어야 함
        val entered = EventEntryEntity(userId, EntryStatus.ENTERED, rewardFixture)
        whenever(eventEntryRepository.findByRewardIdAndUserIdAndStatus(eq(rewardId), eq(userId), eq(EntryStatus.ENTERED)))
            .thenReturn(entered)

        // when
        service.entryReward(eventId, rewardId, userId, EventEntryStatus.CANCELLED)

        // then: 1 + 2 = 3 으로 복구
        assertThat(afterEnter.balance).isEqualTo(3)
        assertThat(entered.status).isEqualTo(EntryStatus.CANCELLED)
        verify(coinLogRepository).save(any())
    }

    @Test
    @DisplayName("잔액 부족 → 응모 실패")
    fun `응모시 발급받은 잔여 코인이 부족한 경우 실패 테스트`() {
        // given: requiredCoins=2, 사용자 잔액=1
        val lowBalance = UserCoinEntity(
            pUserId = userId, pBalance = 1, pAcquiredTotal = 1, pCoinInfo = coinFixture
        )
        whenever(userCoinRepository.findByUserIdAndCoinId(eq(userId), eq(coinId)))
            .thenReturn(lowBalance)

        // when + then
        assertThatThrownBy {
            service.entryReward(eventId, rewardId, userId, EventEntryStatus.ENTERED)
        }
            .isInstanceOf(CoinException::class.java)
            .hasMessageContaining("잔여 코인")

        verify(eventEntryRepository, never()).save(any())
        verify(coinLogRepository,  never()).save(any())
    }

    @Test
    @DisplayName("응모 이력 없이 취소 시 실패")
    fun `응모 취소시 응모 이력이 없는 경우 실패 검증 테스트`() {
        // given: 사용자 코인은 정상 존재
        whenever(userCoinRepository.findByUserIdAndCoinId(eq(userId), eq(coinId)))
            .thenReturn(userCoinFixture)

        // 응모 이력 없음
        whenever(eventEntryRepository.findByRewardIdAndUserIdAndStatus(eq(rewardId), eq(userId), eq(EventEntryEntity.EntryStatus.ENTERED)))
            .thenReturn(null)

        // when + then
        assertThatThrownBy {
            service.entryReward(eventId, rewardId, userId, EventEntryStatus.CANCELLED)
        }
            .isInstanceOf(EventException::class.java)

        verify(coinLogRepository, never()).save(any())
    }

    @Test
    @DisplayName("리워드 없음 → 실패")
    fun `응모 하려는 리워드(휴가권)가 없는 경우 실패 테스트`() {
        // given
        whenever(rewardRepository.findById(eq(rewardId))).thenReturn(Optional.empty())

        // when + then
        assertThatThrownBy {
            service.entryReward(eventId, rewardId, userId, EventEntryStatus.ENTERED)
        }
            .isInstanceOf(EventException::class.java)

        verify(eventEntryRepository, never()).save(any())
    }

    private fun setId(entity: Any, value: Long) {
        val f = entity::class.java.getDeclaredField("id")
        f.isAccessible = true
        f.set(entity, value)
    }
}