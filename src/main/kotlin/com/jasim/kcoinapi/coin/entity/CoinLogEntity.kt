package com.jasim.kcoinapi.coin.entity

import com.jasim.kcoinapi.common.entity.embeddable.Audit
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Table(name = "coin_log")
@Entity
@EntityListeners(AuditingEntityListener::class)
class CoinLogEntity(
    pUserId: String,
    pCoinId: Long,
    pAmount: Int,
    pReason: Reason,
    pEventEntryId: Long? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "user_id")
    var userId: String = pUserId
        protected set

    @Column(name = "coin_id")
    var coinId: Long = pCoinId
        protected set

    @Column(name = "amount")
    var useAmount: Int = pAmount
        protected set

    @Column(name = "reason")
    @Enumerated(EnumType.STRING)
    var reason: Reason = pReason
        protected set

    @Column(name = "event_entry_id")
    var eventEntryId: Long? = pEventEntryId
        protected set

    @Embedded
    var audit: Audit = Audit()
        protected set

    enum class Reason(val code: Int) {
        ISSUE(0),
        ENTERED(1),
        CANCELLED(2)
    }
}