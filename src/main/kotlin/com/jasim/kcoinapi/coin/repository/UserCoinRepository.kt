package com.jasim.kcoinapi.coin.repository

import com.jasim.kcoinapi.coin.entity.UserCoinEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserCoinRepository : JpaRepository<UserCoinEntity, Long> {


}