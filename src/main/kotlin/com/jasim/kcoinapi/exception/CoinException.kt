package com.jasim.kcoinapi.exception

class CoinException(
    val errorType: CoinErrorType? = null,
) : RuntimeException(errorType?.errorMessage) {
    enum class CoinErrorType(val errorMessage: String) {
        NOT_FOUND_USER_COIN("해당 사용자에게 발급된 코인 정보가 없습니다."),
        NOT_EXIST_COIN("발급 요청한 코인이 없습니다."),
        NOT_OVER_PER_LIMIT("코인 발급 한도를 초과하여 발급할 수 없습니다."),
        NOT_ENOUGH_COIN("잔여 코인이 응모에 필요한 수량보다 적습니다."),
        NO_MORE_TOTAL_COIN("잔여 코인이 총 수량보다 많을 수 없습니다."),
        OUT_OF_STOCK_COIN("남은 코인 수량이 없습니다."),
        OUT_OF_STOCK_USER_COIN("발급된 잔여 코인이 없습니다.")
    }
}