package com.taller.test.processor;

import com.taller.test.model.Payment;
import com.taller.test.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProcessorTest {

    private PaymentProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PaymentProcessor();
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

        List<Payment> allPayments = processor.getAllPayments();
        assertEquals(2, allPayments.size());
        assertTrue(allPayments.contains(payment1));
        assertTrue(allPayments.contains(payment2));
    }

    @Test
    void testGetPaymentsByStatus() {
        processor.addPayment(new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.FAILED));
        processor.addPayment(new Payment("TEST003", new BigDecimal("150.0"), "GBP", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST004", new BigDecimal("75.0"), "USD", PaymentStatus.PENDING));

        List<Payment> successPayments = processor.getPaymentsByStatus(PaymentStatus.SUCCESS);
        List<Payment> failedPayments = processor.getPaymentsByStatus(PaymentStatus.FAILED);
        List<Payment> pendingPayments = processor.getPaymentsByStatus(PaymentStatus.PENDING);

        assertEquals(2, successPayments.size());
        assertEquals(1, failedPayments.size());
        assertEquals(1, pendingPayments.size());
    }

    @Test
    void testGetTotalSuccessfulAmount() {
        processor.addPayment(new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.FAILED));
        processor.addPayment(new Payment("TEST003", new BigDecimal("150.0"), "GBP", PaymentStatus.SUCCESS));

        BigDecimal total = processor.getTotalSuccessfulAmount();
        assertEquals(new BigDecimal("250.0"), total);
    }

    @Test
    void testGetAverageSuccessfulAmount() {
        processor.addPayment(new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST003", new BigDecimal("150.0"), "GBP", PaymentStatus.FAILED));

        BigDecimal average = processor.getAverageSuccessfulAmount();
        assertEquals(new BigDecimal("150.00"), average);
    }

    @Test
    void testGetAverageSuccessfulAmountWithNoSuccessfulPayments() {
        processor.addPayment(new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.FAILED));
        processor.addPayment(new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.PENDING));

        BigDecimal average = processor.getAverageSuccessfulAmount();
        assertEquals(BigDecimal.ZERO, average);
    }

    @Test
    void testGetPaymentsSortedByAmount() {
        processor.addPayment(new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST002", new BigDecimal("300.0"), "EUR", PaymentStatus.FAILED));
        processor.addPayment(new Payment("TEST003", new BigDecimal("200.0"), "GBP", PaymentStatus.SUCCESS));

        List<Payment> sorted = processor.getPaymentsSortedByAmount();

        assertEquals(3, sorted.size());
        assertEquals(new BigDecimal("300.0"), sorted.get(0).amount());
        assertEquals(new BigDecimal("200.0"), sorted.get(1).amount());
        assertEquals(new BigDecimal("100.0"), sorted.get(2).amount());
    }

    @Test
    void testGetPaymentCountByStatus() {
        processor.addPayment(new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST003", new BigDecimal("150.0"), "GBP", PaymentStatus.FAILED));
        processor.addPayment(new Payment("TEST004", new BigDecimal("75.0"), "USD", PaymentStatus.PENDING));

        Map<PaymentStatus, Long> countByStatus = processor.getPaymentCountByStatus();

        assertEquals(2L, countByStatus.get(PaymentStatus.SUCCESS));
        assertEquals(1L, countByStatus.get(PaymentStatus.FAILED));
        assertEquals(1L, countByStatus.get(PaymentStatus.PENDING));
    }

    @Test
    void testGetTotalAmountByCurrency() {
        processor.addPayment(new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST002", new BigDecimal("200.0"), "USD", PaymentStatus.FAILED));
        processor.addPayment(new Payment("TEST003", new BigDecimal("150.0"), "EUR", PaymentStatus.SUCCESS));

        Map<String, BigDecimal> amountByCurrency = processor.getTotalAmountByCurrency();

        assertEquals(new BigDecimal("300.0"), amountByCurrency.get("USD"));
        assertEquals(new BigDecimal("150.0"), amountByCurrency.get("EUR"));
    }

    @Test
    void testCalculateStatistics() {
        processor.addPayment(new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST003", new BigDecimal("150.0"), "GBP", PaymentStatus.FAILED));
        processor.addPayment(new Payment("TEST004", new BigDecimal("75.0"), "USD", PaymentStatus.PENDING));

        var stats = processor.calculateStatistics();

        assertEquals(4, stats.totalPayments());
        assertEquals(2, stats.successfulPayments());
        assertEquals(1, stats.failedPayments());
        assertEquals(1, stats.pendingPayments());
        assertEquals(new BigDecimal("300.0"), stats.totalSuccessfulAmount());
        assertEquals(new BigDecimal("150.00"), stats.averageSuccessfulAmount());
    }

    @Test
    void testProcessPaymentsAsync() throws Exception {
        List<Payment> payments = List.of(
                new Payment("ASYNC001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS),
                new Payment("ASYNC002", new BigDecimal("200.0"), "EUR", PaymentStatus.SUCCESS),
                new Payment("ASYNC003", new BigDecimal("300.0"), "GBP", PaymentStatus.SUCCESS)
        );

        CompletableFuture<Void> future = processor.processPaymentsAsync(payments);
        future.get(); // Wait for completion

        assertEquals(3, processor.getTotalPaymentCount());
        assertEquals(new BigDecimal("600.0"), processor.getTotalSuccessfulAmount());
    }

    @Test
    void testClear() {
        processor.addPayment(new Payment("TEST001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS));
        processor.addPayment(new Payment("TEST002", new BigDecimal("200.0"), "EUR", PaymentStatus.FAILED));

        assertEquals(2, processor.getTotalPaymentCount());

        processor.clear();

        assertEquals(0, processor.getTotalPaymentCount());
        assertTrue(processor.getAllPayments().isEmpty());
    }
}
