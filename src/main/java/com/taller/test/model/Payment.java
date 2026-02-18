package com.taller.test.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a payment transaction as a JPA entity.
 * Java 25 Feature: Enhanced pattern matching in validation methods
 *
 * Uses BigDecimal for monetary amounts to avoid floating-point precision issues.
 */
@Entity
@Table(name = "payments")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Payment implements Serializable {

    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Default constructor for JPA.
     */
    protected Payment() {
    }

    /**
     * Constructor with required fields.
     */
    public Payment(String id, BigDecimal amount, String currency, PaymentStatus status) {
        Objects.requireNonNull(id, "Payment ID cannot be null");
        Objects.requireNonNull(amount, "Payment amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(status, "Payment status cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                String.format("Payment amount cannot be negative: %s", amount)
            );
        }

        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    /**
     * JPA lifecycle callback for creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * JPA lifecycle callback for update timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Record-style accessor methods for backward compatibility
    public String id() {
        return id;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public PaymentStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment payment)) return false;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("Payment{id='%s', amount=%s, currency='%s', status=%s}",
                           id, amount, currency, status);
    }
}
