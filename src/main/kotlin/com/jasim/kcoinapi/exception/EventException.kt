package com.jasim.kcoinapi.exception

import com.jasim.kcoinapi.exception.DBException.DBErrorType

class EventException(
    private val errorType: EventErrorType? = null,
): RuntimeException(errorType?.errorMessage) {

    enum class EventErrorType(val errorMessage: String){
        NOT_FOUND_TYPE("존재하지 않는 동작입니다."),
        NOT_FOUND_REWARD("존재하지 않는 휴가권 입니다."),
        NOT_FOUND_EVENT_COIN("이벤트에 발급된 코인이 없습니다."),
        NOT_FOUND_ENTERED_LOG("응모 기록이 없습니다."),
        ALREADY_ENTERED("이미 응모 되었습니다. 중복 응모는 불가능 합니다.")
    }
}