package com.jasim.kcoinapi.event.entity

import com.jasim.kcoinapi.coin.entity.CoinEntity
import com.jasim.kcoinapi.common.entity.embeddable.Audit
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "event")
@EntityListeners(AuditingEntityListener::class)
class EventEntity(
    pEventName: String,
    pStartsAt: Instant,
    pEndsAt: Instant
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "event_name")
    var eventName: String = pEventName
        protected set

    @Column(name = "starts_at")
    var startsAt: Instant = pStartsAt
        protected set

    @Column(name = "ends_at")
    var endsAt: Instant = pEndsAt
        protected set

    @OneToMany(
        mappedBy = "event",
        fetch = FetchType.LAZY
    )
    @BatchSize(size = 50)
    var rewards: MutableList<RewardEntity> = mutableListOf()

    @OneToMany(
        mappedBy = "event",
        fetch = FetchType.LAZY
    )
    @BatchSize(size = 50)
    var coins: MutableList<CoinEntity> = mutableListOf()

    @Embedded
    var audit: Audit = Audit()
        protected set
}
