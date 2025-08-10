package com.jasim.kcoinapi.coin.service.impl

import com.jasim.kcoinapi.coin.entity.CoinLogEntity
import com.jasim.kcoinapi.coin.entity.CoinLogEntity.Reason
import com.jasim.kcoinapi.coin.entity.UserCoinEntity
import com.jasim.kcoinapi.coin.repository.CoinLogRepository
import com.jasim.kcoinapi.coin.repository.CoinRepository
import com.jasim.kcoinapi.coin.repository.UserCoinRepository
import com.jasim.kcoinapi.coin.service.CoinCommandService
import com.jasim.kcoinapi.common.repository.ProcessLockRepository
import com.jasim.kcoinapi.config.LockProperties
import com.jasim.kcoinapi.exception.CoinException
import com.jasim.kcoinapi.exception.CoinException.CoinErrorType
import com.jasim.kcoinapi.exception.DBException
import com.jasim.kcoinapi.exception.DBException.DBErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoinCommandImpl(
    private val coinRepository: CoinRepository,
    private val coinLogRepository: CoinLogRepository,
    private val userCoinRepository: UserCoinRepository,
    private val lockRepository: ProcessLockRepository,
    private val lockProperties: LockProperties
) : CoinCommandService {

    @Transactional(timeout = 10)
    override fun issueCoin(userId: String, coinId: Long, eventId: Long): Boolean {
        //1) lock 획득
        lockRepository.lockWithTimeout(lockProperties.lockKey)
            ?: throw DBException(DBErrorType.LOCK_EXCEPTION)

        val coinInfo = coinRepository.findById(coinId).orElseThrow {
            CoinException(CoinErrorType.NOT_EXIST_COIN)
        }

        // 2) 유저코인 정보 확인 및 생성
        val userCoin = userCoinRepository.findByUserIdAndCoinId(userId, coinId)
            ?: UserCoinEntity(
                pUserId = userId,
                pBalance = 0,
                pAcquiredTotal = 0,
                pCoinInfo = coinInfo
            ).also(userCoinRepository::save)

        // 3) 최대 한도 획득 했는지 여부 검증
        if (userCoin.acquiredTotal >= coinInfo.perUserLimit) {
            throw CoinException(CoinErrorType.NOT_OVER_PER_LIMIT)
        }

        // 4) 코인 획득
        coinInfo.issueCoin()
        userCoin.issueCoin()

        // 5) 로깅
        coinLogRepository.save(
            CoinLogEntity(userId, coinId, coinInfo.perIssueCount, Reason.ISSUE, eventId)
        )

        return true
    }

    @Transactional(timeout = 10)
    override fun issueCoinWithNoLock(userId: String, coinId: Long, eventId: Long): Boolean {
        val coinInfo = coinRepository.findById(coinId).orElseThrow {
            CoinException(CoinErrorType.NOT_EXIST_COIN)
        }

        // 1) 유저코인 정보 확인 및 생성
        val userCoin = userCoinRepository.findByUserIdAndCoinId(userId, coinId)
            ?: UserCoinEntity(
                pUserId = userId,
                pBalance = coinInfo.perIssueCount,
                pAcquiredTotal = coinInfo.perIssueCount,
                pCoinInfo = coinInfo
            ).also(userCoinRepository::save)

        // 3) 최대 한도 획득 했는지 여부 검증
        if (userCoin.acquiredTotal >= coinInfo.perUserLimit) {
            throw CoinException(CoinErrorType.NOT_OVER_PER_LIMIT)
        }

        // 4) 코인 획득
        coinInfo.issueCoin()
        userCoin.issueCoin()

        // 5) 로깅
        coinLogRepository.save(
            CoinLogEntity(userId, coinId, coinInfo.perIssueCount, Reason.ISSUE, eventId)
        )

        return true
    }
}