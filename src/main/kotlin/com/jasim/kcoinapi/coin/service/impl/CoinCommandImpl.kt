package com.jasim.kcoinapi.coin.service.impl

import com.jasim.kcoinapi.coin.entity.CoinLogEntity
import com.jasim.kcoinapi.coin.entity.CoinLogEntity.Reason
import com.jasim.kcoinapi.coin.repository.CoinLogRepository
import com.jasim.kcoinapi.coin.repository.CoinRepository
import com.jasim.kcoinapi.coin.service.CoinCommandSerivce
import com.jasim.kcoinapi.common.repository.ProcessLockRepository
import com.jasim.kcoinapi.config.LockProperties
import com.jasim.kcoinapi.exception.CoinException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CoinCommandImpl(
    private val coinRepository: CoinRepository,
    private val coinLogRepository: CoinLogRepository,
    private val lockRepository: ProcessLockRepository,
    private val lockProperties: LockProperties
) : CoinCommandSerivce {

    @Transactional(timeout = 10)
    override fun issueCoin(userId: String, coinId: Long): Boolean {
        lockRepository.lockWithTimeout(lockProperties.lockKey)
            ?: throw CoinException("다른 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요.")

        val coinInfo = coinRepository.findById(coinId).orElseThrow {
            CoinException("발급 요청한 코인이 없습니다.")
        }

        coinInfo.issueCoin()
        coinLogRepository.save(CoinLogEntity(userId, coinId, coinInfo.perIssueCount, Reason.ISSUE))

        return true
    }
}