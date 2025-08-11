package com.jasim.kcoinapi.integration.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Testcontainers

@TestConfiguration
@Testcontainers
class TestcontainersConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ServiceConnection  // Spring Boot 3.1+ 가
    fun mysql(): MySQLContainer<*> =
        MySQLContainer("mysql:8.0.42")
            .withDatabaseName("coin_test")
            .withUsername("test")
            .withPassword("test")
}