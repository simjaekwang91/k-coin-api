package com.jasim.kcoinapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class KCoinApiApplication

fun main(args: Array<String>) {
    runApplication<KCoinApiApplication>(*args)
}
