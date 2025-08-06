package com.jasim.kcoinapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KCoinApiApplication

fun main(args: Array<String>) {
    runApplication<KCoinApiApplication>(*args)
}
