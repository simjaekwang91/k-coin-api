package com.jasim.kcoinapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class LockProperties {
    @Value("\${dblock.lock-key}")
    lateinit var lockKey: String
}