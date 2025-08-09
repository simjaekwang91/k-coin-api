package com.jasim.kcoinapi.event.entity

import com.jasim.kcoinapi.common.entity.embeddable.Audit
import com.jasim.kcoinapi.exception.EventException
import com.jasim.kcoinapi.exception.EventException.EventErrorType
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "event_entry")
@EntityListeners(AuditingEntityListener::class)
class EventEntryEntity(
    pUserId: String,
    pEntryStatus: EntryStatus,
    pReward: RewardEntity,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "user_id")
    var userId: String = pUserId
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "reward_id",
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    var reward: RewardEntity = pReward
        protected set

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: EntryStatus = pEntryStatus
        protected set

    @Embedded
    var audit: Audit = Audit()
        protected set

    enum class EntryStatus(val code: Int) {
        ENTERED(0),
        CANCELLED(1);

        companion object {
            fun fromCode(code: Int): EntryStatus =
                entries.find { it.code == code }
                    ?: throw EventException(EventErrorType.NOT_FOUND_TYPE)
        }
    }

    fun cancel() {
        status = EntryStatus.CANCELLED
    }
}