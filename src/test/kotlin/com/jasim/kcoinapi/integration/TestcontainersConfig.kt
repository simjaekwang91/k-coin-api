package com.jasim.kcoinapi.integration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Testcontainers

@TestConfiguration
@Testcontainers
class TestcontainersConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ServiceConnection  // Spring Boot 3.1+ 가 DataSource 자동 설정
    fun mysql(): MySQLContainer<*> =
        MySQLContainer("mysql:8.0.42")
            .withDatabaseName("kcoin")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/coin-schema.sql")
}