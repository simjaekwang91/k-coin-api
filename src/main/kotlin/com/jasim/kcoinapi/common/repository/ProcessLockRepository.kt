package com.jasim.kcoinapi.common.repository

import com.jasim.kcoinapi.common.entity.ProcessLockEntity
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface ProcessLockRepository: JpaRepository<ProcessLockEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
        QueryHint(name = "jakarta.persistence.lock.timeout", value = "500") // ms (0=NOWAIT, -1=무한대기)
    )
    @Query("select p from ProcessLockEntity p where p.lockKey = :key")
    fun lockWithTimeout(@Param("key") key: String): ProcessLockEntity?
}