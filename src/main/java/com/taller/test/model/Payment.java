package com.taller.test.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a payment transaction using a Java Record.
 * Java 16+ Feature: Records provide immutable data classes with automatic implementations.
 * Java 25 Feature: Enhanced pattern matching with records
 *
 * Uses BigDecimal for monetary amounts to avoid floating-point precision issues.
 */
public record Payment(
    String id,
    BigDecimal amount,
    String currency,
    PaymentStatus status
) {
    /**
     * Compact constructor with validation.
     * This runs before field initialization.
     */
    public Payment {
        Objects.requireNonNull(id, "Payment ID cannot be null");
        Objects.requireNonNull(amount, "Payment amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(status, "Payment status cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                String.format("Payment amount cannot be negative: %s", amount)
            );
        }
    }

    /**
     * Returns a formatted description of the payment using pattern matching.
     * Java 25 Feature: Enhanced pattern matching with switch expressions
     */
    public String getDescription() {
        return switch (status) {
            case SUCCESS -> String.format("✓ Payment %s succeeded: %s %s", id, amount, currency);
            case FAILED -> String.format("✗ Payment %s failed: %s %s", id, amount, currency);
            case PENDING -> String.format("⏳ Payment %s pending: %s %s", id, amount, currency);
        };
    }

    /**
     * Validates the payment using pattern matching.
     * Java 25 Feature: Pattern matching with guards
     */
    public String validate() {
        return switch (this) {
            case Payment p when p.amount.compareTo(BigDecimal.ZERO) <= 0 ->
                "Invalid: amount must be positive";
            case Payment p when p.amount.compareTo(new BigDecimal("1000000")) > 0 ->
                "Warning: unusually high amount";
            case Payment p when p.status == PaymentStatus.FAILED ->
                "Failed payment";
            case Payment p when p.status == PaymentStatus.PENDING ->
                "Pending approval";
            default -> "Valid payment";
        };
    }

    /**
     * Custom equals using enhanced pattern matching (Java 25).
     * Note: Records already provide equals(), but this demonstrates pattern matching.
     */
    public boolean equalsWithPatternMatching(Object o) {
        return switch (o) {
            case null -> false;
            case Payment p when p == this -> true;
            case Payment p -> amount.compareTo(p.amount) == 0 &&
                             Objects.equals(id, p.id) &&
                             Objects.equals(currency, p.currency) &&
                             status == p.status;
            default -> false;
        };
    }

    /**
     * Custom toString with enhanced formatting.
     * Note: Records auto-generate toString(), but this provides custom formatting.
     */
    @Override
    public String toString() {
        return String.format("Payment{id='%s', amount=%s, currency='%s', status=%s}",
                           id, amount, currency, status);
    }
}
