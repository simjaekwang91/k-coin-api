package com.jasim.kcoinapi

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<KCoinApiApplication>().with(TestcontainersConfiguration::class).run(*args)
}
