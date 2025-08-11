package com.jasim.kcoinapi.coin.service.impl

import com.jasim.kcoinapi.coin.dto.CoinDto
import com.jasim.kcoinapi.coin.dto.UserCoinDto
import com.jasim.kcoinapi.coin.repository.CoinRepository
import com.jasim.kcoinapi.coin.repository.UserCoinRepository
import com.jasim.kcoinapi.coin.service.CoinQueryService
import com.jasim.kcoinapi.common.repository.ProcessLockRepository
import com.jasim.kcoinapi.config.LockProperties
import com.jasim.kcoinapi.exception.CoinException
import com.jasim.kcoinapi.exception.CoinException.CoinErrorType
import com.jasim.kcoinapi.exception.DBException
import com.jasim.kcoinapi.exception.DBException.DBErrorType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoinQueryImpl(
    private val coinRepository: CoinRepository,
    private val userCoinRepository: UserCoinRepository,
    private val lockRepository: ProcessLockRepository,
    private val lockProperties: LockProperties
) : CoinQueryService {
    //남은 응모 코인 수와 사용자별 획득한 응모 코인 수량을 조회한다
    @Transactional(timeout = 10)
    override fun getAllCoinSummary(coinId: Long): CoinDto? {
        //1) lock 획득
        lockRepository.lockWithTimeout(lockProperties.lockKey)
            ?: throw DBException(DBErrorType.LOCK_EXCEPTION)

        return coinRepository.findByIdOrNull(coinId)?.let {
            CoinDto(it.remainCoinCount, userCoinRepository.findSummariesByCoinId(coinId))
        } ?: throw CoinException(CoinErrorType.NOT_EXIST_COIN)
    }

    @Transactional(timeout = 10)
    override fun getUserCoinSummary(userId: String, coinId: Long): UserCoinDto? {
        lockRepository.lockWithTimeout(lockProperties.lockKey)
            ?: throw DBException(DBErrorType.LOCK_EXCEPTION)

        return userCoinRepository.findUserCoinSummary(userId, coinId)
            ?: throw CoinException(CoinErrorType.NOT_EXIST_COIN)
    }
}