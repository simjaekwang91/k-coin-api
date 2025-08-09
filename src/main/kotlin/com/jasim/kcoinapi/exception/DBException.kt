package com.jasim.kcoinapi.exception

class DBException(
    val errorType: DBErrorType? = null,
): RuntimeException(errorType?.errorMessage) {

    enum class DBErrorType(val errorMessage: String){
        LOCK_EXCEPTION("다른 요청이 처리 중입니다. 잠시 후 다시 시도해 주세요.")
    }
}