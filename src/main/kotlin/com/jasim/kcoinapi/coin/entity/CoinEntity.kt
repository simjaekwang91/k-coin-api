package com.jasim.kcoinapi.coin.entity

import com.jasim.kcoinapi.event.entity.EventEntity
import com.jasim.kcoinapi.common.entity.embeddable.Audit
import com.jasim.kcoinapi.exception.CoinException
import com.jasim.kcoinapi.exception.CoinException.CoinErrorType
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
@Table(name = "coin")
@EntityListeners(AuditingEntityListener::class)
class CoinEntity(
    pPerUserLimit: Int,
    pTotalCoinCount: Int,
    pRemainCoinCount: Int,
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
        mappedBy = "coin",
        fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    var userCoins: MutableList<UserCoinEntity> = mutableListOf()
        protected set

    @Column(name = "per_user_limit")
    var perUserLimit: Int = pPerUserLimit
        protected set

    @Column(name = "per_issue_count")
    var perIssueCount: Int = 1

    @Column(name = "total_coin_count")
    var totalCoinCount: Int = pTotalCoinCount
        protected set

    @Column(name = "remain_coin_count")
    var remainCoinCount: Int = pRemainCoinCount
        protected set

    @Embedded
    var audit: Audit = Audit()
        protected set

    fun issueCoin() {
        if (remainCoinCount <= 0) {
            throw CoinException(CoinErrorType.OUT_OF_STOCK_COIN)
        }

        remainCoinCount = remainCoinCount - perIssueCount
    }
}