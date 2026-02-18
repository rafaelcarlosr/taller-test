package com.taller.test.e2e;

import com.taller.test.TestcontainersConfiguration;
import com.taller.test.model.Payment;
import com.taller.test.model.PaymentStatus;
import com.taller.test.processor.PaymentProcessor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full E2E Integration Tests with PostgreSQL via Testcontainers.
 * Tests the complete workflow with real PostgreSQL database to catch database-specific issues.
 *
 * These tests are slower but catch PostgreSQL-specific query problems that H2 might miss.
 * Run separately with: ./gradlew test --tests PaymentApiPostgreSQLTest
 *
 * Requires Docker to be running.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cache.type=none",
        "spring.data.redis.repositories.enabled=false"
    }
)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("postgresql")
@Tag("slow")
class PaymentApiPostgreSQLTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentProcessor processor;

    @BeforeEach
    void setUp() {
        // Clear payments before each test
        processor.clear();
    }

    @Test
    @Order(1)
    @DisplayName("PostgreSQL E2E: Complete Payment Workflow")
    void testCompletePaymentWorkflow() {
        System.out.println("\n=== PostgreSQL E2E Test: Complete Payment Statistics Workflow ===\n");

        // Step 1: Create multiple payments
        System.out.println("1. Creating payments via POST /api/payments...");
        List<Payment> samplePayments = List.of(
                new Payment("PAY001", new BigDecimal("150.00"), "USD", PaymentStatus.SUCCESS),
                new Payment("PAY002", new BigDecimal("75.50"), "EUR", PaymentStatus.SUCCESS),
                new Payment("PAY003", new BigDecimal("200.00"), "USD", PaymentStatus.FAILED),
                new Payment("PAY004", new BigDecimal("325.75"), "GBP", PaymentStatus.SUCCESS),
                new Payment("PAY005", new BigDecimal("50.00"), "USD", PaymentStatus.PENDING)
        );

        for (Payment payment : samplePayments) {
            ResponseEntity<Payment> response = restTemplate.postForEntity(
                "/api/payments",
                payment,
                Payment.class
            );
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
        }
        System.out.println("✓ Created " + samplePayments.size() + " payments in PostgreSQL\n");

        // Step 2: Verify PostgreSQL-specific behavior - decimal precision
        System.out.println("2. Verifying PostgreSQL decimal precision...");
        ResponseEntity<Payment> paymentResponse = restTemplate.getForEntity(
            "/api/payments/PAY004",
            Payment.class
        );
        Payment payment = paymentResponse.getBody();
        assertNotNull(payment);
        // PostgreSQL NUMERIC(19,2) should preserve exact decimal
        assertEquals(0, new BigDecimal("325.75").compareTo(payment.getAmount()));
        System.out.println("✓ PostgreSQL decimal precision verified\n");

        // Step 3: Test aggregate queries (PostgreSQL specific)
        System.out.println("3. Testing PostgreSQL aggregate queries...");
        ResponseEntity<PaymentProcessor.PaymentStatistics> statsResponse = restTemplate.getForEntity(
            "/api/payments/statistics",
            PaymentProcessor.PaymentStatistics.class
        );
        assertEquals(HttpStatus.OK, statsResponse.getStatusCode());
        PaymentProcessor.PaymentStatistics stats = statsResponse.getBody();
        assertNotNull(stats);
        assertEquals(5, stats.totalPayments());
        assertEquals(3, stats.successfulPayments());
        System.out.println("✓ PostgreSQL aggregate queries working\n");

        // Step 4: Test sorting (PostgreSQL specific ORDER BY)
        System.out.println("4. Testing PostgreSQL ORDER BY...");
        ResponseEntity<Payment[]> sortedResponse = restTemplate.getForEntity(
            "/api/payments/sorted",
            Payment[].class
        );
        assertEquals(HttpStatus.OK, sortedResponse.getStatusCode());
        Payment[] sortedPayments = sortedResponse.getBody();
        assertNotNull(sortedPayments);
        assertEquals(5, sortedPayments.length);
        // Verify descending order
        assertTrue(sortedPayments[0].getAmount().compareTo(sortedPayments[1].getAmount()) >= 0);
        assertTrue(sortedPayments[1].getAmount().compareTo(sortedPayments[2].getAmount()) >= 0);
        System.out.println("✓ PostgreSQL ORDER BY working correctly\n");

        System.out.println("=== PostgreSQL E2E Test Complete ===");
        System.out.println("All PostgreSQL-specific features tested successfully!\n");
    }

    @Test
    @Order(2)
    @DisplayName("PostgreSQL E2E: Currency Grouping")
    void testCurrencyGrouping() {
        // Test PostgreSQL GROUP BY behavior
        restTemplate.postForEntity("/api/payments",
            new Payment("CURR001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS),
            Payment.class);
        restTemplate.postForEntity("/api/payments",
            new Payment("CURR002", new BigDecimal("200.00"), "USD", PaymentStatus.SUCCESS),
            Payment.class);
        restTemplate.postForEntity("/api/payments",
            new Payment("CURR003", new BigDecimal("150.00"), "EUR", PaymentStatus.SUCCESS),
            Payment.class);

        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/payments/statistics/by-currency",
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("USD"));
        assertTrue(response.getBody().containsKey("EUR"));
    }

    @Test
    @Order(3)
    @DisplayName("PostgreSQL E2E: Transaction Rollback")
    void testTransactionRollback() {
        // Test that duplicate IDs trigger proper rollback in PostgreSQL
        Payment payment1 = new Payment("DUP001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS);

        ResponseEntity<Payment> response1 = restTemplate.postForEntity(
            "/api/payments",
            payment1,
            Payment.class
        );
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());

        // Try to create duplicate - should fail
        ResponseEntity<Payment> response2 = restTemplate.postForEntity(
            "/api/payments",
            payment1,
            Payment.class
        );
        // Should fail (either 400 or 500 depending on error handling)
        assertTrue(response2.getStatusCode().is4xxClientError() || response2.getStatusCode().is5xxServerError());

        // Verify only one payment exists
        ResponseEntity<Long> countResponse = restTemplate.getForEntity(
            "/api/payments/count",
            Long.class
        );
        assertEquals(1L, countResponse.getBody());
    }

    @Test
    @Order(4)
    @DisplayName("PostgreSQL E2E: Timestamp Handling")
    void testTimestampHandling() {
        // Test PostgreSQL TIMESTAMP handling
        Payment payment = new Payment("TIME001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS);

        ResponseEntity<Payment> createResponse = restTemplate.postForEntity(
            "/api/payments",
            payment,
            Payment.class
        );
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        Payment created = createResponse.getBody();
        assertNotNull(created);
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        // PostgreSQL should store timestamps with proper precision
        System.out.println("Created timestamp: " + created.getCreatedAt());
        System.out.println("Updated timestamp: " + created.getUpdatedAt());
    }

    @Test
    @Order(5)
    @DisplayName("PostgreSQL E2E: Large Batch Insert")
    void testLargeBatchInsert() {
        // Test PostgreSQL performance with larger batch
        System.out.println("\n=== Testing PostgreSQL batch insert performance ===\n");

        int batchSize = 50;
        for (int i = 0; i < batchSize; i++) {
            Payment payment = new Payment(
                "BATCH" + String.format("%03d", i),
                new BigDecimal("100.00"),
                "USD",
                PaymentStatus.SUCCESS
            );
            restTemplate.postForEntity("/api/payments", payment, Payment.class);
        }

        ResponseEntity<Long> countResponse = restTemplate.getForEntity(
            "/api/payments/count",
            Long.class
        );
        assertEquals((long) batchSize, countResponse.getBody());

        System.out.println("✓ Successfully inserted " + batchSize + " payments into PostgreSQL\n");
    }

    @Test
    @Order(6)
    @DisplayName("PostgreSQL E2E: Decimal Scale Precision")
    void testDecimalScalePrecision() {
        // Test that PostgreSQL NUMERIC(19,2) properly handles scale
        Payment payment1 = new Payment("SCALE001", new BigDecimal("0.10"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("SCALE002", new BigDecimal("0.20"), "USD", PaymentStatus.SUCCESS);

        restTemplate.postForEntity("/api/payments", payment1, Payment.class);
        restTemplate.postForEntity("/api/payments", payment2, Payment.class);

        ResponseEntity<PaymentProcessor.PaymentStatistics> response = restTemplate.getForEntity(
            "/api/payments/statistics",
            PaymentProcessor.PaymentStatistics.class
        );

        PaymentProcessor.PaymentStatistics stats = response.getBody();
        assertNotNull(stats);

        // PostgreSQL NUMERIC should give exact result: 0.10 + 0.20 = 0.30
        assertEquals(0, new BigDecimal("0.30").compareTo(stats.totalSuccessfulAmount()));
        System.out.println("✓ PostgreSQL NUMERIC precision verified: 0.10 + 0.20 = " + stats.totalSuccessfulAmount());
    }
}
