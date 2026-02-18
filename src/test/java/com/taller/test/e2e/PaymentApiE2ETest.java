package com.taller.test.e2e;


import com.taller.test.model.Payment;
import com.taller.test.model.PaymentStatus;
import com.taller.test.processor.PaymentProcessor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Integration Tests for Payment Statistics API.
 * Tests the complete workflow via REST endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentApiE2ETest {

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
    @DisplayName("E2E: Complete Payment Workflow")
    void testCompletePaymentWorkflow() {
        System.out.println("\n=== E2E Test: Complete Payment Statistics Workflow ===\n");

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
        System.out.println("✓ Created " + samplePayments.size() + " payments\n");

        // Step 2: Get all payments
        System.out.println("2. Getting all payments via GET /api/payments...");
        ResponseEntity<Payment[]> allPaymentsResponse = restTemplate.getForEntity(
            "/api/payments",
            Payment[].class
        );
        assertEquals(HttpStatus.OK, allPaymentsResponse.getStatusCode());
        Payment[] allPayments = allPaymentsResponse.getBody();
        assertNotNull(allPayments);
        assertEquals(5, allPayments.length);
        System.out.println("✓ Retrieved " + allPayments.length + " payments\n");

        // Step 3: Get payment by ID
        System.out.println("3. Getting specific payment via GET /api/payments/{id}...");
        ResponseEntity<Payment> paymentResponse = restTemplate.getForEntity(
            "/api/payments/PAY001",
            Payment.class
        );
        assertEquals(HttpStatus.OK, paymentResponse.getStatusCode());
        Payment payment = paymentResponse.getBody();
        assertNotNull(payment);
        assertEquals("PAY001", payment.id());
        assertEquals(new BigDecimal("150.00"), payment.amount());
        System.out.println("✓ Found payment: " + payment.id() + "\n");

        // Step 4: Get payments by status
        System.out.println("4. Getting payments by status via GET /api/payments/status/{status}...");
        ResponseEntity<Payment[]> successPayments = restTemplate.getForEntity(
            "/api/payments/status/SUCCESS",
            Payment[].class
        );
        assertEquals(HttpStatus.OK, successPayments.getStatusCode());
        assertNotNull(successPayments.getBody());
        assertEquals(3, successPayments.getBody().length);
        System.out.println("✓ Found 3 SUCCESS payments\n");

        // Step 5: Get statistics
        System.out.println("5. Getting statistics via GET /api/payments/statistics...");
        ResponseEntity<PaymentProcessor.PaymentStatistics> statsResponse = restTemplate.getForEntity(
            "/api/payments/statistics",
            PaymentProcessor.PaymentStatistics.class
        );
        assertEquals(HttpStatus.OK, statsResponse.getStatusCode());
        PaymentProcessor.PaymentStatistics stats = statsResponse.getBody();
        assertNotNull(stats);
        assertEquals(5, stats.totalPayments());
        assertEquals(3, stats.successfulPayments());
        assertEquals(1, stats.failedPayments());
        assertEquals(1, stats.pendingPayments());
        System.out.println("✓ Statistics calculated correctly\n");

        // Step 6: Get sorted payments
        System.out.println("6. Getting sorted payments via GET /api/payments/sorted...");
        ResponseEntity<Payment[]> sortedResponse = restTemplate.getForEntity(
            "/api/payments/sorted",
            Payment[].class
        );
        assertEquals(HttpStatus.OK, sortedResponse.getStatusCode());
        Payment[] sortedPayments = sortedResponse.getBody();
        assertNotNull(sortedPayments);
        assertEquals(5, sortedPayments.length);
        // Verify descending order
        assertTrue(sortedPayments[0].amount().compareTo(sortedPayments[1].amount()) >= 0);
        System.out.println("✓ Payments sorted correctly\n");

        // Step 7: Validate a payment
        System.out.println("7. Validating payment via POST /api/payments/validate...");
        Payment testPayment = new Payment("TEST", new BigDecimal("500.00"), "USD", PaymentStatus.SUCCESS);
        ResponseEntity<String> validationResponse = restTemplate.postForEntity(
            "/api/payments/validate",
            testPayment,
            String.class
        );
        assertEquals(HttpStatus.OK, validationResponse.getStatusCode());
        assertEquals("Valid payment", validationResponse.getBody());
        System.out.println("✓ Validation working\n");

        // Step 8: Get count
        System.out.println("8. Getting payment count via GET /api/payments/count...");
        ResponseEntity<Integer> countResponse = restTemplate.getForEntity(
            "/api/payments/count",
            Integer.class
        );
        assertEquals(HttpStatus.OK, countResponse.getStatusCode());
        assertEquals(5, countResponse.getBody());
        System.out.println("✓ Count correct: " + countResponse.getBody() + "\n");

        System.out.println("=== E2E Test Complete ===");
        System.out.println("All REST endpoints tested successfully!\n");
    }

    @Test
    @Order(2)
    @DisplayName("E2E: Payment Not Found")
    void testPaymentNotFound() {
        ResponseEntity<Payment> response = restTemplate.getForEntity(
            "/api/payments/NONEXISTENT",
            Payment.class
        );
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @Order(3)
    @DisplayName("E2E: Get Statistics By Status")
    void testGetStatisticsByStatus() {
        // Create test data
        restTemplate.postForEntity("/api/payments",
            new Payment("STAT001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS),
            Payment.class);
        restTemplate.postForEntity("/api/payments",
            new Payment("STAT002", new BigDecimal("200.00"), "EUR", PaymentStatus.SUCCESS),
            Payment.class);
        restTemplate.postForEntity("/api/payments",
            new Payment("STAT003", new BigDecimal("300.00"), "GBP", PaymentStatus.FAILED),
            Payment.class);

        // Get statistics
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/payments/statistics/by-status",
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("SUCCESS"));
    }

    @Test
    @Order(4)
    @DisplayName("E2E: Get Statistics By Currency")
    void testGetStatisticsByCurrency() {
        // Create test data
        restTemplate.postForEntity("/api/payments",
            new Payment("CURR001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS),
            Payment.class);
        restTemplate.postForEntity("/api/payments",
            new Payment("CURR002", new BigDecimal("200.00"), "USD", PaymentStatus.SUCCESS),
            Payment.class);

        // Get statistics
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/api/payments/statistics/by-currency",
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("USD"));
    }

    @Test
    @Order(5)
    @DisplayName("E2E: Clear All Payments")
    void testClearPayments() {
        // Add a payment
        restTemplate.postForEntity("/api/payments",
            new Payment("CLEAR001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS),
            Payment.class);

        // Verify it exists
        ResponseEntity<Integer> countBefore = restTemplate.getForEntity(
            "/api/payments/count",
            Integer.class
        );
        assertTrue(countBefore.getBody() > 0);

        // Clear all payments
        restTemplate.delete("/api/payments");

        // Verify cleared
        ResponseEntity<Integer> countAfter = restTemplate.getForEntity(
            "/api/payments/count",
            Integer.class
        );
        assertEquals(0, countAfter.getBody());
    }

    @Test
    @Order(6)
    @DisplayName("E2E: BigDecimal Precision")
    void testBigDecimalPrecision() {
        // Create payment with precise decimal
        Payment payment = new Payment("PREC001", new BigDecimal("0.10"), "USD", PaymentStatus.SUCCESS);
        restTemplate.postForEntity("/api/payments", payment, Payment.class);

        Payment payment2 = new Payment("PREC002", new BigDecimal("0.20"), "USD", PaymentStatus.SUCCESS);
        restTemplate.postForEntity("/api/payments", payment2, Payment.class);

        // Get statistics
        ResponseEntity<PaymentProcessor.PaymentStatistics> response = restTemplate.getForEntity(
            "/api/payments/statistics",
            PaymentProcessor.PaymentStatistics.class
        );

        PaymentProcessor.PaymentStatistics stats = response.getBody();
        assertNotNull(stats);

        // BigDecimal should give exact result: 0.10 + 0.20 = 0.30 (not 0.30000000000000004)
        assertEquals(new BigDecimal("0.30"), stats.totalSuccessfulAmount());
        System.out.println("✓ BigDecimal precision verified: 0.10 + 0.20 = " + stats.totalSuccessfulAmount());
    }
}
