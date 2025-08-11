package com.jasim.kcoinapi.event.service.impl

import com.jasim.kcoinapi.event.dto.RewardEntryDto
import com.jasim.kcoinapi.event.dto.UserEntryDto
import com.jasim.kcoinapi.common.repository.ProcessLockRepository
import com.jasim.kcoinapi.config.LockProperties
import com.jasim.kcoinapi.event.entity.EventEntryEntity.EntryStatus
import com.jasim.kcoinapi.event.repository.EventEntryRepository
import com.jasim.kcoinapi.event.repository.RewardRepository
import com.jasim.kcoinapi.event.service.EventQueryService
import com.jasim.kcoinapi.exception.DBException
import com.jasim.kcoinapi.exception.DBException.DBErrorType
import com.jasim.kcoinapi.exception.EventException
import com.jasim.kcoinapi.exception.EventException.EventErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventQueryImpl(
    private val eventEntryRepository: EventEntryRepository,
    private val rewardRepository: RewardRepository,
    private val lockRepository: ProcessLockRepository,
    private val lockProperties: LockProperties
): EventQueryService {

    @Transactional(timeout = 10)
    override fun getAllEntryInfoByReward(eventId: Long, rewardId: Long): RewardEntryDto {
        //1) lock 획득
        lockRepository.lockWithTimeout(lockProperties.lockKey)
            ?: throw DBException(DBErrorType.LOCK_EXCEPTION)

        //2) 리워드 (1일휴가권 OR 3일 휴가권)
        val reward = rewardRepository.findById(rewardId).orElseThrow {
            EventException(EventErrorType.NOT_FOUND_REWARD)
        }

        //3)응모 수량 확인
        val entryCount = eventEntryRepository.countByRewardIdAndStatus(rewardId, EntryStatus.ENTERED).toInt()

        return RewardEntryDto(reward.rewardName, entryCount)
    }

    @Transactional(timeout = 10)
    override fun getUserEntryInfo(eventId: Long, rewardId: Long, userId: String): UserEntryDto {
        //1) lock 획득
        lockRepository.lockWithTimeout(lockProperties.lockKey)
            ?: throw DBException(DBErrorType.LOCK_EXCEPTION)

        //2) 리워드 (1일휴가권 OR 3일 휴가권)
        val reward = rewardRepository.findById(rewardId).orElseThrow {
            EventException(EventErrorType.NOT_FOUND_REWARD)
        }

        //3) 유저 응모 현황 조회
        val userEntries = eventEntryRepository.findUserEntryDetails(rewardId, userId)

        return UserEntryDto(userId, userEntries)
    }
}