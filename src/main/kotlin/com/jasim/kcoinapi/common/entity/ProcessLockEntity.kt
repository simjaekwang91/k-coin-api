package com.jasim.kcoinapi.common.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id

@Entity
@Table(name = "process_lock")
class ProcessLockEntity {
    @Id
    @Column(name = "lock_key")
    var lockKey: String = ""
        protected set
}