package com.jasim.kcoinapi.event.service.impl

import com.jasim.kcoinapi.coin.entity.CoinLogEntity
import com.jasim.kcoinapi.coin.entity.CoinLogEntity.Reason
import com.jasim.kcoinapi.coin.repository.CoinLogRepository
import com.jasim.kcoinapi.coin.repository.CoinRepository
import com.jasim.kcoinapi.coin.repository.UserCoinRepository
import com.jasim.kcoinapi.common.repository.ProcessLockRepository
import com.jasim.kcoinapi.config.LockProperties
import com.jasim.kcoinapi.event.entity.EventEntryEntity
import com.jasim.kcoinapi.event.entity.EventEntryEntity.EntryStatus
import com.jasim.kcoinapi.event.repository.EventEntryRepository
import com.jasim.kcoinapi.event.repository.RewardRepository
import com.jasim.kcoinapi.event.service.EventCommandService
import com.jasim.kcoinapi.exception.CoinException
import com.jasim.kcoinapi.exception.CoinException.CoinErrorType
import com.jasim.kcoinapi.exception.DBException
import com.jasim.kcoinapi.exception.DBException.DBErrorType
import com.jasim.kcoinapi.exception.EventException
import com.jasim.kcoinapi.exception.EventException.EventErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventCommandImpl(
    private val eventEntryRepository: EventEntryRepository,
    private val rewardRepository: RewardRepository,
    private val coinRepository: CoinRepository,
    private val userCoinRepository: UserCoinRepository,
    private val lockRepository: ProcessLockRepository,
    private val coinLogRepository: CoinLogRepository,
    private val lockProperties: LockProperties,
) : EventCommandService {

    @Transactional(timeout = 10)
    override fun setReward(eventId: Long, rewardId: Long, userId: String, status: Int): Boolean {
        //1) lock 획득
        lockRepository.lockWithTimeout(lockProperties.lockKey)
            ?: throw DBException(DBErrorType.LOCK_EXCEPTION)

        //2) 리워드 (1일휴가권 OR 3일 휴가권)
        val reward = rewardRepository.findById(rewardId).orElseThrow {
            EventException(EventErrorType.NOT_FOUND_REWARD)
        }

        //3)코인 정보 확인
        val coinInfo = coinRepository.findByEventId(eventId)
            ?: throw EventException(EventErrorType.NOT_FOUND_EVENT_COIN)

        val userCoinInfo = userCoinRepository.findByUserIdAndCoinId(userId, coinInfo.id!!)
            ?: throw CoinException(CoinErrorType.NOT_FOUND_USER_COIN)

        //4)응모/취소 하기
        when (EntryStatus.fromCode(status)) {
            //응모
            EntryStatus.ENTERED -> {
                if (eventEntryRepository.existsByRewardIdAndUserIdAndStatus(rewardId, userId, EntryStatus.ENTERED)) {
                    throw EventException(EventErrorType.ALREADY_ENTERED)
                }

                userCoinInfo.entryCoin(reward.requiredCoins)
                eventEntryRepository.save(
                    EventEntryEntity(
                        userId,
                        EntryStatus.ENTERED,
                        reward
                    )
                )

                // 5) 로깅
                coinLogRepository.save(
                    CoinLogEntity(userId, coinInfo.id!!, reward.requiredCoins, Reason.ENTERED, eventId)
                )
            }
            //응모취소
            EntryStatus.CANCELLED -> {
                userCoinInfo.cancelCoin(reward.requiredCoins)
                val eventEntryInfo = eventEntryRepository.findByRewardIdAndUserIdAndStatus(rewardId, userId, EntryStatus.ENTERED)
                    ?: throw EventException(EventErrorType.NOT_FOUND_ENTERED_LOG)

                eventEntryInfo.cancel()
                // 5) 로깅
                coinLogRepository.save(
                    CoinLogEntity(userId, coinInfo.id!!, reward.requiredCoins, Reason.CANCELLED, eventId)
                )
            }
        }

        return true
    }

}