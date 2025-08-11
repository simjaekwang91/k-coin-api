package com.jasim.kcoinapi.event.repository

import com.jasim.kcoinapi.event.dto.UserEntryDetail
import com.jasim.kcoinapi.event.entity.EventEntryEntity
import com.jasim.kcoinapi.event.entity.EventEntryEntity.EntryStatus
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EventEntryRepository : JpaRepository<EventEntryEntity, Long> {
    fun countByRewardIdAndStatus(rewardId: Long, status: EntryStatus): Long
    // 유저 상세는 reward, event까지 한 번에 (N+1 방지)
    @EntityGraph(attributePaths = ["reward", "reward.event"])
    fun findByRewardIdAndUserId(rewardId: Long, userId: String): List<EventEntryEntity>
    fun findByRewardIdAndUserIdAndStatus(rewardId: Long, userId: String, status: EntryStatus): EventEntryEntity?
    fun existsByRewardIdAndUserIdAndStatus(rewardId: Long, userId: String, status: EntryStatus): Boolean

    @Query("""
        select new com.jasim.kcoinapi.event.dto.UserEntryDetail(
            e.eventName,
            r.rewardName,
            cast(ee.status as string),
            ee.audit.createdAt,
            ee.audit.updatedAt
        )
        from EventEntryEntity ee
        join ee.reward r
        join r.event e
        where r.id = :rewardId
          and ee.userId = :userId
        order by ee.audit.createdAt desc
    """)
    fun findUserEntryDetails(
        @Param("rewardId") rewardId: Long,
        @Param("userId") userId: String
    ): List<UserEntryDetail>
}