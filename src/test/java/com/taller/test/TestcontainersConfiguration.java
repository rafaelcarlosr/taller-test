package com.taller.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for integration tests.
 * Automatically provides PostgreSQL database for tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * PostgreSQL container for tests.
     * @ServiceConnection automatically configures Spring Boot datasource properties.
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("payment_stats_test")
                .withUsername("test_user")
                .withPassword("test_pass")
                .withReuse(true);
    }
}
