package com.taller.test.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void testPaymentCreation() {
        Payment payment = new Payment("PAY001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);

        assertEquals("PAY001", payment.id());
        assertEquals(new BigDecimal("100.0"), payment.amount());
        assertEquals("USD", payment.currency());
        assertEquals(PaymentStatus.SUCCESS, payment.status());
    }

    @Test
    void testPaymentWithNullIdThrowsException() {
        assertThrows(NullPointerException.class,
                () -> new Payment(null, new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS));
    }

    @Test
    void testPaymentWithNullAmountThrowsException() {
        assertThrows(NullPointerException.class,
                () -> new Payment("PAY001", null, "USD", PaymentStatus.SUCCESS));
    }

    @Test
    void testPaymentWithNullCurrencyThrowsException() {
        assertThrows(NullPointerException.class,
                () -> new Payment("PAY001", new BigDecimal("100.0"), null, PaymentStatus.SUCCESS));
    }

    @Test
    void testPaymentWithNullStatusThrowsException() {
        assertThrows(NullPointerException.class,
                () -> new Payment("PAY001", new BigDecimal("100.0"), "USD", null));
    }

    @Test
    void testPaymentWithNegativeAmountThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Payment("PAY001", new BigDecimal("-100.0"), "USD", PaymentStatus.SUCCESS));
    }

    @Test
    void testPaymentEquals() {
        Payment payment1 = new Payment("PAY001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("PAY001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        Payment payment3 = new Payment("PAY002", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);

        assertEquals(payment1, payment2);
        assertNotEquals(payment1, payment3);
    }

    @Test
    void testPaymentHashCode() {
        Payment payment1 = new Payment("PAY001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        Payment payment2 = new Payment("PAY001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);

        assertEquals(payment1.hashCode(), payment2.hashCode());
    }

    @Test
    void testPaymentToString() {
        Payment payment = new Payment("PAY001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        String toString = payment.toString();

        assertTrue(toString.contains("PAY001"));
        assertTrue(toString.contains("100"));
        assertTrue(toString.contains("USD"));
        assertTrue(toString.contains("SUCCESS"));
    }

    @Test
    void testPaymentWithZeroAmount() {
        Payment payment = new Payment("PAY001", BigDecimal.ZERO, "USD", PaymentStatus.PENDING);
        assertEquals(BigDecimal.ZERO, payment.amount());
    }

    @Test
    void testPaymentImmutability() {
        Payment payment = new Payment("PAY001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);

        // Verify record accessors return correct values
        String id = payment.id();
        BigDecimal amount = payment.amount();
        String currency = payment.currency();
        PaymentStatus status = payment.status();

        // Original payment should remain unchanged
        assertEquals("PAY001", payment.id());
        assertEquals(new BigDecimal("100.0"), payment.amount());
        assertEquals("USD", payment.currency());
        assertEquals(PaymentStatus.SUCCESS, payment.status());
    }

    @Test
    void testPaymentValidation() {
        Payment validPayment = new Payment("PAY001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        assertEquals("Valid payment", validPayment.validate());

        Payment highValuePayment = new Payment("PAY002", new BigDecimal("2000000"), "USD", PaymentStatus.SUCCESS);
        assertEquals("Warning: unusually high amount", highValuePayment.validate());

        Payment failedPayment = new Payment("PAY003", new BigDecimal("100.0"), "USD", PaymentStatus.FAILED);
        assertEquals("Failed payment", failedPayment.validate());

        Payment pendingPayment = new Payment("PAY004", new BigDecimal("100.0"), "USD", PaymentStatus.PENDING);
        assertEquals("Pending approval", pendingPayment.validate());
    }

    @Test
    void testPaymentDescription() {
        Payment successPayment = new Payment("PAY001", new BigDecimal("100.0"), "USD", PaymentStatus.SUCCESS);
        assertTrue(successPayment.getDescription().contains("✓"));
        assertTrue(successPayment.getDescription().contains("succeeded"));

        Payment failedPayment = new Payment("PAY002", new BigDecimal("100.0"), "USD", PaymentStatus.FAILED);
        assertTrue(failedPayment.getDescription().contains("✗"));
        assertTrue(failedPayment.getDescription().contains("failed"));

        Payment pendingPayment = new Payment("PAY003", new BigDecimal("100.0"), "USD", PaymentStatus.PENDING);
        assertTrue(pendingPayment.getDescription().contains("⏳"));
        assertTrue(pendingPayment.getDescription().contains("pending"));
    }
}
