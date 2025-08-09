package com.jasim.kcoinapi.exception

class CoinException(
    val coinErrorType: CoinErrorType? = null,
) : RuntimeException(coinErrorType?.errorMessage) {
    enum class CoinErrorType(val errorMessage: String) {
        NOT_EXIST_COIN("발급 요청한 코인이 없습니다."),
        NOT_OVER_PER_LIMIT("코인 발급 한도를 초과하여 발급할 수 없습니다."),
        OUT_OF_STOCK_COIN("남은 코인 수량이 없습니다.")
    }
}