package com.jasim.kcoinapi.exception

import com.jasim.kcoinapi.exception.CoinException.CoinErrorType
import java.lang.RuntimeException

class DBException(
    override val message: String? = null,
    val errorType: DBErrorType? = null,
): RuntimeException(message) {

    enum class DBErrorType(val errorMessage: String){
        LOCK_EXCEPTION("락 획득에 실패하였습니다.")
    }
}