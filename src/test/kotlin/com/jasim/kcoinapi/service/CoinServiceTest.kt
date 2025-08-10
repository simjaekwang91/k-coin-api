package com.jasim.kcoinapi.service

import com.jasim.kcoinapi.coin.entity.CoinEntity
import com.jasim.kcoinapi.coin.entity.CoinLogEntity
import com.jasim.kcoinapi.coin.entity.UserCoinEntity
import com.jasim.kcoinapi.coin.repository.CoinLogRepository
import com.jasim.kcoinapi.coin.repository.CoinRepository
import com.jasim.kcoinapi.coin.repository.UserCoinRepository
import com.jasim.kcoinapi.coin.service.CoinCommandService
import com.jasim.kcoinapi.coin.service.impl.CoinCommandImpl
import com.jasim.kcoinapi.common.entity.ProcessLockEntity
import com.jasim.kcoinapi.common.repository.ProcessLockRepository
import com.jasim.kcoinapi.config.LockProperties
import com.jasim.kcoinapi.event.entity.EventEntity
import com.jasim.kcoinapi.exception.CoinException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.Optional

@ActiveProfiles("test")
@ExtendWith(MockitoExtension::class)
class CoinServiceTest {
    @Mock
    lateinit var coinRepository: CoinRepository
    @Mock
    lateinit var coinLogRepository: CoinLogRepository
    @Mock
    lateinit var userCoinRepository: UserCoinRepository
    @Mock
    lateinit var lockRepository: ProcessLockRepository

    // LockProperties는 스프링 없이도 직접 값 주입해서 씀
    private lateinit var lockProps: LockProperties

    // @InjectMocks로 주입하려면 lockProps가 먼저 준비돼야 해서,
    // 생성자 주입을 위해 테스트에서 직접 new 해도 됨 (setup에서 초기화)
    private lateinit var service: CoinCommandService

    private val coinId = 1L
    private val userId = "u1"
    private val eventId = 10L

    private lateinit var coinFixture: CoinEntity
    private lateinit var eventFixture: EventEntity
    private lateinit var userCoinFixture: UserCoinEntity


    @BeforeEach
    fun setup() {
        // 실제 엔티티로 상태 검증
        eventFixture = EventEntity("이벤트", Instant.now(), Instant.now().plusSeconds(3600))

        coinFixture = CoinEntity(
            pPerUserLimit = 3,
            pTotalCoinCount = 5,
            pRemainCoinCount = 5,
            pEvent = eventFixture
        )

        userCoinFixture = UserCoinEntity(
            pUserId = userId,
            pBalance = 3,
            pAcquiredTotal = 3,
            pCoinInfo = coinFixture
        )

        setId(eventFixture, eventId)
        setId(coinFixture, coinId)

        lockProps = LockProperties(lockKey = "global")

        // 서비스 실제 인스턴스 구성
        service = CoinCommandImpl(
            coinRepository = coinRepository,
            coinLogRepository = coinLogRepository,
            userCoinRepository = userCoinRepository,
            lockRepository = lockRepository,
            lockProperties = lockProps
        )

        // 공통 스텁: 락 획득 성공 + 코인 조회 성공
        whenever(lockRepository.lockWithTimeout(eq(lockProps.lockKey)))
            .thenReturn(ProcessLockEntity())
    }

    @Test
    @DisplayName("issueCoin 성공 → 재고 1 감소 + 로그 1건 저장")
    fun issue_success() {
        whenever(coinRepository.findById(eq(coinId))).thenReturn(Optional.of(coinFixture))
        whenever(userCoinRepository.findByUserIdAndCoinId(eq(userId), eq(coinId))).thenReturn(null)
        whenever(coinLogRepository.save(any<CoinLogEntity>())).thenAnswer { it.arguments[0] as CoinLogEntity }

        val before = coinFixture.remainCoinCount
        val ok = service.issueCoin(userId, coinId, eventId)

        assertThat(ok).isTrue()
        assertThat(coinFixture.remainCoinCount).isEqualTo(before - coinFixture.perIssueCount)

        verify(lockRepository).lockWithTimeout(eq(lockProps.lockKey))
        verify(coinRepository).findById(eq(coinId))
        verify(coinLogRepository).save(any())
        verifyNoMoreInteractions(lockRepository, coinRepository, coinLogRepository)
    }

    @Test
    @DisplayName("코인 없음 → CoinException(발급 요청한 코인이 없습니다)")
    fun issue_coinNotFound() {
        whenever(coinRepository.findById(eq(coinId))).thenReturn(Optional.empty())

        assertThatThrownBy {
            service.issueCoin(userId, coinId, eventId)
        }
            .isInstanceOf(CoinException::class.java)
            .hasMessageContaining("발급 요청한 코인이 없습니다")

        verify(lockRepository).lockWithTimeout(eq(lockProps.lockKey))
        verify(coinRepository).findById(eq(coinId))
        verifyNoMoreInteractions(lockRepository, coinRepository)
        verifyNoInteractions(coinLogRepository, userCoinRepository)
    }

    @Test
    @DisplayName("재고 0 → 도메인 로직이 던지는 예외 전파 (남은 코인 수량 없음 등)")
    fun issue_outOfStock() {
        // 재고 0 코인
        val zero = CoinEntity(
            pPerUserLimit = 3,
            pTotalCoinCount = 0,
            pRemainCoinCount = 0,
            pEvent = eventFixture,
        )
        whenever(coinRepository.findById(eq(coinId))).thenReturn(Optional.of(zero))

        assertThatThrownBy {
            service.issueCoin(userId, coinId, eventId)
        }.isInstanceOf(RuntimeException::class.java)

        verify(lockRepository).lockWithTimeout(eq(lockProps.lockKey))
        verify(coinRepository).findById(eq(coinId))
        verifyNoMoreInteractions(lockRepository, coinRepository)
        verifyNoInteractions(coinLogRepository)
    }

    private fun setId(entity: Any, value: Long) {
        val f = entity::class.java.getDeclaredField("id")
        f.isAccessible = true
        f.set(entity, value)
    }
}