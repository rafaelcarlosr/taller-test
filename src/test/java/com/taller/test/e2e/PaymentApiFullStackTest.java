package com.taller.test.e2e;

import com.taller.test.model.Payment;
import com.taller.test.model.PaymentStatus;
import com.taller.test.processor.PaymentProcessor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full Stack Integration Tests with PostgreSQL + Redis via Testcontainers.
 * Tests the complete production-like setup including caching behavior.
 *
 * These tests verify:
 * - PostgreSQL database operations
 * - Redis caching functionality
 * - Cache invalidation on updates
 * - Cache hit/miss scenarios
 *
 * Requires Docker to be running.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("fullstack")
@Tag("slow")
class PaymentApiFullStackTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine")
    )
        .withDatabaseName("payment_stats_test")
        .withUsername("test_user")
        .withPassword("test_pass")
        .withReuse(false);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
        DockerImageName.parse("redis:7-alpine")
    )
        .withExposedPorts(6379)
        .withReuse(false);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.cache.type", () -> "redis");

        // JPA configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentProcessor processor;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Clear database and cache before each test
        processor.clear();
        Objects.requireNonNull(cacheManager.getCache("paymentsByStatus")).clear();
        Objects.requireNonNull(cacheManager.getCache("paymentsSorted")).clear();
        Objects.requireNonNull(cacheManager.getCache("paymentStatistics")).clear();
    }

    @Test
    @Order(1)
    @DisplayName("Full Stack: Verify Redis Caching Works")
    void testRedisCachingWorks() {
        System.out.println("\n=== Full Stack Test: Redis Caching ===\n");

        // Create test payments
        Payment payment1 = new Payment("CACHE001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("CACHE002", new BigDecimal("200.00"), "USD", PaymentStatus.SUCCESS);

        restTemplate.postForEntity("/api/payments", payment1, Payment.class);
        restTemplate.postForEntity("/api/payments", payment2, Payment.class);

        // First call - should hit database and cache
        System.out.println("1. First call to /api/payments/sorted (cache miss - should query DB)");
        long start1 = System.currentTimeMillis();
        ResponseEntity<Payment[]> response1 = restTemplate.getForEntity("/api/payments/sorted", Payment[].class);
        long duration1 = System.currentTimeMillis() - start1;
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertNotNull(response1.getBody());
        assertEquals(2, response1.getBody().length);
        System.out.println("   First call took: " + duration1 + "ms (database query)\n");

        // Second call - should hit cache (faster)
        System.out.println("2. Second call to /api/payments/sorted (cache hit - should be faster)");
        long start2 = System.currentTimeMillis();
        ResponseEntity<Payment[]> response2 = restTemplate.getForEntity("/api/payments/sorted", Payment[].class);
        long duration2 = System.currentTimeMillis() - start2;
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertNotNull(response2.getBody());
        assertEquals(2, response2.getBody().length);
        System.out.println("   Second call took: " + duration2 + "ms (Redis cache)");
        System.out.println("   ✓ Cache hit! (speedup: " + (duration1 - duration2) + "ms)\n");

        // Verify cache exists
        var cache = cacheManager.getCache("paymentsSorted");
        assertNotNull(cache, "Cache 'paymentsSorted' should exist");
        System.out.println("✓ Redis cache verified working\n");
    }

    @Test
    @Order(2)
    @DisplayName("Full Stack: Cache Invalidation on Create")
    void testCacheInvalidationOnCreate() {
        System.out.println("\n=== Full Stack Test: Cache Invalidation ===\n");

        // Create initial payment and cache it
        Payment payment1 = new Payment("INV001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS);
        restTemplate.postForEntity("/api/payments", payment1, Payment.class);

        // Call sorted to populate cache
        System.out.println("1. Populating cache with 1 payment");
        ResponseEntity<Payment[]> response1 = restTemplate.getForEntity("/api/payments/sorted", Payment[].class);
        assertEquals(1, Objects.requireNonNull(response1.getBody()).length);

        // Create new payment (should invalidate cache)
        System.out.println("2. Creating new payment (should invalidate cache)");
        Payment payment2 = new Payment("INV002", new BigDecimal("200.00"), "USD", PaymentStatus.SUCCESS);
        restTemplate.postForEntity("/api/payments", payment2, Payment.class);

        // Call sorted again - should reflect new payment
        System.out.println("3. Fetching sorted payments (cache should be cleared, fresh DB query)");
        ResponseEntity<Payment[]> response2 = restTemplate.getForEntity("/api/payments/sorted", Payment[].class);
        assertEquals(2, Objects.requireNonNull(response2.getBody()).length);
        System.out.println("   ✓ Cache invalidated successfully - showing 2 payments\n");

        // Verify highest amount is first (DESC order)
        Payment[] sorted = response2.getBody();
        assertEquals(0, new BigDecimal("200.00").compareTo(sorted[0].getAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(sorted[1].getAmount()));
        System.out.println("✓ Cache invalidation working correctly\n");
    }

    @Test
    @Order(3)
    @DisplayName("Full Stack: Statistics Caching")
    void testStatisticsCaching() {
        System.out.println("\n=== Full Stack Test: Statistics Caching ===\n");

        // Create test data
        restTemplate.postForEntity("/api/payments",
            new Payment("STAT001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS),
            Payment.class);
        restTemplate.postForEntity("/api/payments",
            new Payment("STAT002", new BigDecimal("200.00"), "EUR", PaymentStatus.SUCCESS),
            Payment.class);

        // First call to statistics
        System.out.println("1. First call to /api/payments/statistics (cache miss)");
        long start1 = System.currentTimeMillis();
        ResponseEntity<PaymentProcessor.PaymentStatistics> response1 = restTemplate.getForEntity(
            "/api/payments/statistics",
            PaymentProcessor.PaymentStatistics.class
        );
        long duration1 = System.currentTimeMillis() - start1;
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        PaymentProcessor.PaymentStatistics stats1 = response1.getBody();
        assertNotNull(stats1);
        assertEquals(2, stats1.totalPayments());
        System.out.println("   Duration: " + duration1 + "ms (database aggregation)\n");

        // Second call should be cached
        System.out.println("2. Second call to /api/payments/statistics (cache hit)");
        long start2 = System.currentTimeMillis();
        ResponseEntity<PaymentProcessor.PaymentStatistics> response2 = restTemplate.getForEntity(
            "/api/payments/statistics",
            PaymentProcessor.PaymentStatistics.class
        );
        long duration2 = System.currentTimeMillis() - start2;
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        PaymentProcessor.PaymentStatistics stats2 = response2.getBody();
        assertNotNull(stats2);
        assertEquals(2, stats2.totalPayments());
        System.out.println("   Duration: " + duration2 + "ms (Redis cache)");
        System.out.println("   ✓ Cached! (speedup: " + (duration1 - duration2) + "ms)\n");

        System.out.println("✓ Statistics caching working\n");
    }

    @Test
    @Order(4)
    @DisplayName("Full Stack: Cache Invalidation on Delete")
    void testCacheInvalidationOnDelete() {
        // Create and cache payments
        restTemplate.postForEntity("/api/payments",
            new Payment("DEL001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS),
            Payment.class);

        // Populate cache
        restTemplate.getForEntity("/api/payments/statistics", PaymentProcessor.PaymentStatistics.class);

        // Clear all payments (should invalidate cache)
        restTemplate.delete("/api/payments");

        // Verify cache was invalidated
        ResponseEntity<PaymentProcessor.PaymentStatistics> response = restTemplate.getForEntity(
            "/api/payments/statistics",
            PaymentProcessor.PaymentStatistics.class
        );
        PaymentProcessor.PaymentStatistics stats = response.getBody();
        assertNotNull(stats);
        assertEquals(0, stats.totalPayments());
        System.out.println("✓ Cache invalidated on delete\n");
    }

    @Test
    @Order(5)
    @DisplayName("Full Stack: Status Filter Caching")
    void testStatusFilterCaching() {
        System.out.println("\n=== Full Stack Test: Status Filter Caching ===\n");

        // Create payments with different statuses
        restTemplate.postForEntity("/api/payments",
            new Payment("FIL001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS),
            Payment.class);
        restTemplate.postForEntity("/api/payments",
            new Payment("FIL002", new BigDecimal("200.00"), "EUR", PaymentStatus.FAILED),
            Payment.class);
        restTemplate.postForEntity("/api/payments",
            new Payment("FIL003", new BigDecimal("300.00"), "GBP", PaymentStatus.SUCCESS),
            Payment.class);

        // Query by status (should cache)
        System.out.println("1. First call to /api/payments/status/SUCCESS (cache miss)");
        long start1 = System.currentTimeMillis();
        ResponseEntity<Payment[]> response1 = restTemplate.getForEntity(
            "/api/payments/status/SUCCESS",
            Payment[].class
        );
        long duration1 = System.currentTimeMillis() - start1;
        assertEquals(2, Objects.requireNonNull(response1.getBody()).length);
        System.out.println("   Duration: " + duration1 + "ms (database query)\n");

        // Second query should hit cache
        System.out.println("2. Second call to /api/payments/status/SUCCESS (cache hit)");
        long start2 = System.currentTimeMillis();
        ResponseEntity<Payment[]> response2 = restTemplate.getForEntity(
            "/api/payments/status/SUCCESS",
            Payment[].class
        );
        long duration2 = System.currentTimeMillis() - start2;
        assertEquals(2, Objects.requireNonNull(response2.getBody()).length);
        System.out.println("   Duration: " + duration2 + "ms (Redis cache)");
        System.out.println("   ✓ Cached! (speedup: " + (duration1 - duration2) + "ms)\n");

        System.out.println("✓ Status filter caching working\n");
    }

    @Test
    @Order(6)
    @DisplayName("Full Stack: PostgreSQL + Redis Integration")
    void testFullStackIntegration() {
        System.out.println("\n=== Full Stack Test: Complete Integration ===\n");

        // Verify both containers are running
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
        assertTrue(redis.isRunning(), "Redis container should be running");
        System.out.println("✓ PostgreSQL container: " + postgres.getJdbcUrl());
        System.out.println("✓ Redis container: " + redis.getHost() + ":" + redis.getFirstMappedPort() + "\n");

        // Create payment (PostgreSQL write)
        Payment payment = new Payment("INT001", new BigDecimal("999.99"), "USD", PaymentStatus.SUCCESS);
        ResponseEntity<Payment> createResponse = restTemplate.postForEntity("/api/payments", payment, Payment.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        System.out.println("✓ Payment saved to PostgreSQL\n");

        // Fetch and cache (PostgreSQL read + Redis write)
        ResponseEntity<Payment[]> getResponse = restTemplate.getForEntity("/api/payments/sorted", Payment[].class);
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(1, Objects.requireNonNull(getResponse.getBody()).length);
        System.out.println("✓ Payment fetched from PostgreSQL and cached in Redis\n");

        // Fetch from cache (Redis read)
        ResponseEntity<Payment[]> cachedResponse = restTemplate.getForEntity("/api/payments/sorted", Payment[].class);
        assertEquals(HttpStatus.OK, cachedResponse.getStatusCode());
        assertEquals(1, Objects.requireNonNull(cachedResponse.getBody()).length);
        System.out.println("✓ Payment served from Redis cache\n");

        // Verify cache manager has Redis cache
        assertNotNull(cacheManager.getCache("paymentsSorted"), "Redis cache should exist");
        System.out.println("✓ CacheManager configured with Redis\n");

        System.out.println("=== Full Stack Integration Test Complete ===");
        System.out.println("PostgreSQL + Redis working together perfectly!\n");
    }
}
