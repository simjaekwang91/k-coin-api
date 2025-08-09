package com.jasim.kcoinapi.exception

class CoinException(
    override val message: String? = null,
) : RuntimeException(message) {
    enum class CoinErrorType(val errorMessage: String, val code: Int){

    }
}