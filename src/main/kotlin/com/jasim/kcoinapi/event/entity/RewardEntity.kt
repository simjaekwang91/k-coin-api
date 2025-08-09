package com.jasim.kcoinapi.event.entity

import com.jasim.kcoinapi.common.entity.embeddable.Audit
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "reward")
@EntityListeners(AuditingEntityListener::class)
class RewardEntity(
    pRewardName: String,
    pWinningQuota: Int,
    pRequiredCoins: Int,
    pEvent: EventEntity
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "event_id",
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    var event: EventEntity = pEvent
        protected set

    @OneToMany(
        mappedBy = "reward",
        fetch = FetchType.LAZY
    )
    @BatchSize(size = 50)
    var entries: MutableList<EventEntryEntity> = mutableListOf()
        protected set

    @Column(name = "reward_name")
    var rewardName: String = pRewardName
        protected set

    @Column(name = "winning_quota")
    var winningQuota: Int = pWinningQuota
        protected set

    @Column(name = "required_coins")
    var requiredCoins: Int = pRequiredCoins
        protected set

    @Embedded
    var audit: Audit = Audit()
        protected set
}