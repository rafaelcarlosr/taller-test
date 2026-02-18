package com.taller.test.processor;

import com.taller.test.model.Payment;
import com.taller.test.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PaymentProcessor with real database (H2 in-memory for tests).
 */
@SpringBootTest(properties = {
    "spring.cache.type=none",
    "spring.data.redis.repositories.enabled=false"
})
@ActiveProfiles("test")
class PaymentProcessorTest {

    @Autowired
    private PaymentProcessor processor;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        processor.clear();
    }

    @Test
    void testAddPayment() {
        Payment payment = new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        processor.addPayment(payment);

        assertEquals(1, processor.getTotalPaymentCount());
        assertTrue(processor.getAllPayments().contains(payment));
    }

    @Test
    void testAddDuplicatePaymentThrowsException() {
        Payment payment1 = new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("TEST001", new BigDecimal("200.0"), "EUR", PaymentStatus.FAILED);

        processor.addPayment(payment1);

        assertThrows(IllegalArgumentException.class, () -> processor.addPayment(payment2));
    }

    @Test
    void testGetAllPayments() {
        Payment payment1 = new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.FAILED);

        processor.addPayment(payment1);
        processor.addPayment(payment2);

        List<Payment> payments = processor.getAllPayments();
        assertEquals(2, payments.size());
        assertTrue(payments.containsAll(List.of(payment1, payment2)));
    }

    @Test
    void testGetPaymentsByStatus() {
        Payment payment1 = new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.FAILED);
        Payment payment3 = new Payment("TEST003", new BigDecimal("300.0"), "GBP", PaymentStatus.SUCCESS);

        processor.addPayment(payment1);
        processor.addPayment(payment2);
        processor.addPayment(payment3);

        List<Payment> successfulPayments = processor.getPaymentsByStatus(PaymentStatus.SUCCESS);
        assertEquals(2, successfulPayments.size());
        assertTrue(successfulPayments.stream().allMatch(p -> p.getStatus() == PaymentStatus.SUCCESS));

        List<Payment> failedPayments = processor.getPaymentsByStatus(PaymentStatus.FAILED);
        assertEquals(1, failedPayments.size());
    }

    @Test
    void testGetTotalSuccessfulAmount() {
        Payment payment1 = new Payment("TEST001", new BigDecimal("100.50"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("TEST002", new BigDecimal("200.25"), "EUR", PaymentStatus.SUCCESS);
        Payment payment3 = new Payment("TEST003", new BigDecimal("300.00"), "GBP", PaymentStatus.FAILED);

        processor.addPayment(payment1);
        processor.addPayment(payment2);
        processor.addPayment(payment3);

        BigDecimal total = processor.getTotalSuccessfulAmount();
        assertEquals(new BigDecimal("300.75"), total);
    }

    @Test
    void testGetAverageSuccessfulAmount() {
        Payment payment1 = new Payment("TEST001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("TEST002", new BigDecimal("200.00"), "EUR", PaymentStatus.SUCCESS);
        Payment payment3 = new Payment("TEST003", new BigDecimal("300.00"), "GBP", PaymentStatus.FAILED);

        processor.addPayment(payment1);
        processor.addPayment(payment2);
        processor.addPayment(payment3);

        BigDecimal average = processor.getAverageSuccessfulAmount();
        assertEquals(new BigDecimal("150.00"), average);
    }

    @Test
    void testGetAverageWhenNoSuccessfulPayments() {
        Payment payment = new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.FAILED);
        processor.addPayment(payment);

        BigDecimal average = processor.getAverageSuccessfulAmount();
        assertEquals(BigDecimal.ZERO, average);
    }

    @Test
    void testGetPaymentsSortedByAmount() {
        Payment payment1 = new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("TEST002", new BigDecimal("300.0"), "EUR", PaymentStatus.FAILED);
        Payment payment3 = new Payment("TEST003", new BigDecimal("200.0"), "GBP", PaymentStatus.SUCCESS);

        processor.addPayment(payment1);
        processor.addPayment(payment2);
        processor.addPayment(payment3);

        List<Payment> sorted = processor.getPaymentsSortedByAmount();

        assertEquals(3, sorted.size());
        assertEquals(0, new BigDecimal("300.0").compareTo(sorted.get(0).getAmount()));
        assertEquals(0, new BigDecimal("200.0").compareTo(sorted.get(1).getAmount()));
        assertEquals(0, new BigDecimal("100.0").compareTo(sorted.get(2).getAmount()));
    }

    @Test
    void testGetPaymentCountByStatus() {
        Payment payment1 = new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.SUCCESS);
        Payment payment3 = new Payment("TEST003", new BigDecimal("300.0"), "GBP", PaymentStatus.FAILED);
        Payment payment4 = new Payment("TEST004", new BigDecimal("400.0"), "USD", PaymentStatus.PENDING);

        processor.addPayment(payment1);
        processor.addPayment(payment2);
        processor.addPayment(payment3);
        processor.addPayment(payment4);

        Map<PaymentStatus, Long> countByStatus = processor.getPaymentCountByStatus();

        assertEquals(2L, countByStatus.get(PaymentStatus.SUCCESS));
        assertEquals(1L, countByStatus.get(PaymentStatus.FAILED));
        assertEquals(1L, countByStatus.get(PaymentStatus.PENDING));
    }

    @Test
    void testGetTotalAmountByCurrency() {
        Payment payment1 = new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("TEST002", new BigDecimal("150.0"), "USD", PaymentStatus.FAILED);
        Payment payment3 = new Payment("TEST003", new BigDecimal("200.0"), "EUR", PaymentStatus.SUCCESS);

        processor.addPayment(payment1);
        processor.addPayment(payment2);
        processor.addPayment(payment3);

        Map<String, BigDecimal> totalByCurrency = processor.getTotalAmountByCurrency();

        assertEquals(0, new BigDecimal("250.0").compareTo(totalByCurrency.get("USD")));
        assertEquals(0, new BigDecimal("200.0").compareTo(totalByCurrency.get("EUR")));
    }

    @Test
    void testCalculateStatistics() {
        Payment payment1 = new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.SUCCESS);
        Payment payment3 = new Payment("TEST003", new BigDecimal("300.0"), "GBP", PaymentStatus.FAILED);
        Payment payment4 = new Payment("TEST004", new BigDecimal("400.0"), "USD", PaymentStatus.PENDING);

        processor.addPayment(payment1);
        processor.addPayment(payment2);
        processor.addPayment(payment3);
        processor.addPayment(payment4);

        PaymentProcessor.PaymentStatistics stats = processor.calculateStatistics();

        assertEquals(4, stats.totalPayments());
        assertEquals(2, stats.successfulPayments());
        assertEquals(1, stats.failedPayments());
        assertEquals(1, stats.pendingPayments());
        assertEquals(0, new BigDecimal("300.0").compareTo(stats.totalSuccessfulAmount()));
        assertEquals(0, new BigDecimal("150.00").compareTo(stats.averageSuccessfulAmount()));
    }

    @Test
    void testProcessPaymentsAsync() throws Exception {
        List<Payment> payments = List.of(
                new Payment("ASYNC001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS),
                new Payment("ASYNC002", new BigDecimal("200.0"), "EUR", PaymentStatus.FAILED),
                new Payment("ASYNC003", new BigDecimal("300.0"), "GBP", PaymentStatus.PENDING)
        );

        CompletableFuture<Void> future = processor.processPaymentsAsync(payments);
        future.get(); // Wait for completion

        assertEquals(3, processor.getTotalPaymentCount());
    }

    @Test
    void testValidatePaymentsAsync() throws Exception {
        List<Payment> payments = List.of(
                new Payment("VAL001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS),
                new Payment("VAL002", new BigDecimal("15000.0"), "EUR", PaymentStatus.SUCCESS),
                new Payment("VAL003", new BigDecimal("300.0"), "GBP", PaymentStatus.FAILED)
        );

        CompletableFuture<List<String>> future = processor.validatePaymentsAsync(payments);
        List<String> results = future.get();

        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(r -> r.contains("VAL001")));
        assertTrue(results.stream().anyMatch(r -> r.contains("High-value") && r.contains("VAL002")));
        assertTrue(results.stream().anyMatch(r -> r.contains("Failed") && r.contains("VAL003")));
    }

    @Test
    void testClear() {
        Payment payment1 = new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.FAILED);

        processor.addPayment(payment1);
        processor.addPayment(payment2);
        assertEquals(2, processor.getTotalPaymentCount());

        processor.clear();
        assertEquals(0, processor.getTotalPaymentCount());
    }

    @Test
    void testBigDecimalPrecision() {
        // Test that BigDecimal provides exact decimal arithmetic (no floating point errors)
        Payment payment1 = new Payment("PREC001", new BigDecimal("0.1"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("PREC002", new BigDecimal("0.2"), "USD", PaymentStatus.SUCCESS);

        processor.addPayment(payment1);
        processor.addPayment(payment2);

        BigDecimal total = processor.getTotalSuccessfulAmount();

        // With double: 0.1 + 0.2 = 0.30000000000000004
        // With BigDecimal: 0.1 + 0.2 = 0.3 exactly
        assertEquals(0, new BigDecimal("0.3").compareTo(total));
    }
}
