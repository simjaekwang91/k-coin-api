package com.jasim.kcoinapi.service

import com.jasim.kcoinapi.coin.entity.CoinEntity
import com.jasim.kcoinapi.coin.entity.UserCoinEntity
import com.jasim.kcoinapi.coin.repository.CoinLogRepository
import com.jasim.kcoinapi.coin.repository.CoinRepository
import com.jasim.kcoinapi.coin.repository.UserCoinRepository
import com.jasim.kcoinapi.coin.service.CoinCommandSerivce
import com.jasim.kcoinapi.coin.service.CoinQueryService
import com.jasim.kcoinapi.coin.service.impl.CoinCommandImpl
import com.jasim.kcoinapi.coin.service.impl.CoinQueryImpl
import com.jasim.kcoinapi.config.LockProperties
import com.jasim.kcoinapi.event.entity.EventEntity
import com.jasim.kcoinapi.event.repository.EventRepository
import com.jasim.kcoinapi.exception.CoinException
import com.jasim.kcoinapi.exception.CoinException.CoinErrorType
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@ActiveProfiles("test")
@DataJpaTest
@Import(CoinCommandImpl::class, LockProperties::class, CoinQueryImpl::class) // 설정 빈 등록
@TestPropertySource(properties = ["dblock.lock-key=global"]) // 값 주입
class CoinCommandConcurrencyTest {

    @Autowired
    lateinit var coinCommandService: CoinCommandSerivce
    @Autowired
    lateinit var coinQueryService: CoinQueryService
    @Autowired
    lateinit var coinRepository: CoinRepository
    @Autowired
    lateinit var coinLogRepository: CoinLogRepository
    @Autowired
    lateinit var userCoinRepository: UserCoinRepository
    @Autowired
    lateinit var eventRepository: EventRepository
    @Autowired
    lateinit var jdbc: org.springframework.jdbc.core.JdbcTemplate

    private val initialRemain = 5
    private val threads = 50
    private lateinit var coin: CoinEntity
    lateinit var userCoinInfo1: UserCoinEntity
    lateinit var userCoinInfo2: UserCoinEntity

    @BeforeEach
    fun setUp() {
        // 전역락 키 보장 (DDL에 미리 넣었다면 생략 가능)
        jdbc.update(
            "MERGE INTO process_lock (lock_key) KEY(lock_key) VALUES (?)",
            "global"
        )

        val event = eventRepository.save(
            EventEntity(
                "2025 여름휴가 이벤트",
                Instant.now(),
                Instant.now().plus(10, ChronoUnit.DAYS),
            )
        )
        // 코인정보 생성
        val coin = CoinEntity(
            pPerUserLimit = 3,
            pTotalCoinCount = initialRemain,
            pRemainCoinCount = initialRemain,
            pEvent = event,
        )


        userCoinInfo1 = userCoinRepository.save(UserCoinEntity("u1", pBalance = 2, pAcquiredTotal = 3, pCoinInfo = coin))
        userCoinInfo2 = userCoinRepository.save(UserCoinEntity("u2", pBalance = 1, pAcquiredTotal = 1, pCoinInfo = coin))

        this.coin = coinRepository.saveAndFlush(coin)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 스레드별 커밋 보이게
    @DisplayName("50개 쓰레드 동시 요청시 무결성 유지 검증")
    fun `lock을 통한 코인 발급 요청 무결성 유지 검증 테스트`() {
        val pool = Executors.newFixedThreadPool(50)
        val done = CountDownLatch(threads)

        val success = AtomicInteger(0)

        repeat(threads) { i ->
            pool.submit {
                try {
                    coinCommandService.issueCoin("testUser${i}", coin.id!!, coin.event.id!!)
                    val coin = coinRepository.findById(coin.id!!).get()
                    val userCoinInfo = userCoinRepository.findByUserIdAndCoinId("testUser${i}", coin.id!!)
                    println(userCoinInfo)
                    println("testUser${i} 코인 발급 잔여 코인 수량 ${coin.remainCoinCount}")
                    success.incrementAndGet()
                } catch (e: CoinException) {
                    if ((e.message ?: "").contains("다른 요청이 처리 중") || (e.message ?: "").contains("남은 코인 수량이 없습니다")) {
                        println("testUser${i} error : ${e.message}")
                    }
                } finally {
                    done.countDown()
                }
            }
        }

        done.await(10, TimeUnit.SECONDS)
        pool.shutdownNow()

        val updated = coinRepository.findById(coin.id!!).get()
        val logsForCoin = coinLogRepository.count()
        println("최종 발급 코인 수량 ${success.get()}")

        assertThat(success.get())
            .describedAs("최종 코인 발급 수량은 totalCoinCount 수량을 초과할 수 없음")
            .isLessThanOrEqualTo(updated.totalCoinCount)

        assertThat(updated.remainCoinCount)
            .describedAs("최종 잔여 코인 = 초기 - 성공")
            .isEqualTo(initialRemain - success.get())

        assertThat(logsForCoin)
            .describedAs("로그 수 = 성공 수")
            .isEqualTo(success.get().toLong())

        assertThat(updated.remainCoinCount)
            .describedAs("잔여 코인은 음수가 될 수 없다.")
            .isGreaterThanOrEqualTo(0)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 스레드별 커밋 보이게
    @DisplayName("50개 쓰레드 동시 요청시 무결성 깨짐 검증(실패 테스트)")
    fun `Lock 없이 수행시 무결성 깨짐 테스트`() {
        val pool = Executors.newFixedThreadPool(50)
        val done = CountDownLatch(threads)

        val success = AtomicInteger(0)

        repeat(threads) { i ->
            pool.submit {
                try {
                    coinCommandService.issueCoinWithNoLock("testUser$i", coin.id!!, coin.event.id!!)
                    val coin = coinRepository.findById(coin.id!!).get()
                    println("testUser${i} 코인 발급 잔여 코인 수량 ${coin.remainCoinCount}")
                    success.incrementAndGet()
                } catch (e: CoinException) {
                    println(e.message)
                } finally {
                    done.countDown()
                }
            }
        }

        done.await(10, TimeUnit.SECONDS)
        pool.shutdownNow()

        val updated = coinRepository.findById(coin.id!!).get()
        println("최종 발급 코인 수량 ${success.get()}")

        // 정상 invariant: remain + logs == initialRemain
        assertThat(success.get())
            .describedAs("최종 코인 발급 수량은 totalCoinCount 수량을 초과할 수 없음(무결성 깨짐)")
            .isGreaterThan(updated.totalCoinCount)
    }

    @Test
    fun `getAllCoinInfo - 남은코인과 사용자 목록 반환`() {
        val dto = coinQueryService.getAllCoinInfo(coin.id!!)
        assertThat(dto?.remainCoinCount).isEqualTo(5)
        assertThat(dto?.userCoinInfo).hasSize(2)
    }

    @Test
    fun `getAllCoinInfo - 코인 없음 예외`() {
        assertThatThrownBy { coinQueryService.getAllCoinInfo(-1L) }
            .isInstanceOf(CoinException::class.java)
            .hasMessageContaining(CoinErrorType.NOT_EXIST_COIN.errorMessage)
    }

    @Test
    fun `getCoinInfoByUserId - 특정 사용자 코인정보`() {
        val dto = coinQueryService.getCoinInfoByUserId("u1", coin.id!!)
        assertThat(dto).isNotNull
        assertThat(dto!!.userId).isEqualTo("u1")
        assertThat(dto.balance).isEqualTo(2)
        assertThat(dto.acquiredTotal).isEqualTo(3)
    }

    @Test
    fun `getCoinInfoByUserId - 없으면 null`() {
        val dto = coinQueryService.getCoinInfoByUserId("noneInfo", coin.id!!)
        assertThat(dto).isNull()
    }
}