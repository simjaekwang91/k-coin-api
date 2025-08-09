package com.jasim.kcoinapi.coin.entity

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
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "user_coin")
@EntityListeners(AuditingEntityListener::class)
class UserCoinEntity(
    pUserId: String,
    pBalance: Int,
    pAcquiredTotal: Int,
    pCoinInfo: CoinEntity
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
        name = "coin_id",
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    var coin: CoinEntity = pCoinInfo
        protected set

    @Column(name = "balance")
    var balance: Int = pBalance
        protected set

    @Column(name = "acquired_total")
    var acquiredTotal: Int = pAcquiredTotal
        protected set

    @Embedded
    var audit: Audit = Audit()
        protected set

    fun issueCoin() {
        balance++
        acquiredTotal++
    }

    fun entryCoin(requireCoinCount: Int) {
        if (balance < requireCoinCount) {
            throw CoinException(CoinErrorType.NOT_ENOUGH_COIN)
        }

        if (balance <= 0) {
            throw CoinException(CoinErrorType.OUT_OF_STOCK_USER_COIN)
        }

        balance = balance - requireCoinCount
    }

    fun cancelCoin(requireCoinCount: Int) {
        if ((balance + requireCoinCount) > acquiredTotal) {
            throw CoinException(CoinErrorType.NO_MORE_TOTAL_COIN)
        }

        balance = balance + requireCoinCount
    }

}