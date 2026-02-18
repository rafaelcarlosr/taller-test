package com.taller.test.controller;

import com.taller.test.model.Payment;
import com.taller.test.model.PaymentStatus;
import com.taller.test.processor.PaymentProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Payment operations.
 * Provides endpoints for managing payments and calculating statistics.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentProcessor processor;

    public PaymentController(PaymentProcessor processor) {
        this.processor = processor;
    }

    /**
     * Create a new payment.
     * POST /api/payments
     */
    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        processor.addPayment(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    /**
     * Get all payments.
     * GET /api/payments
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(processor.getAllPayments());
    }

    /**
     * Get payment by ID.
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String id) {
        return processor.getAllPayments().stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get payments by status.
     * GET /api/payments/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        return ResponseEntity.ok(processor.getPaymentsByStatus(status));
    }

    /**
     * Get payments sorted by amount.
     * GET /api/payments/sorted
     */
    @GetMapping("/sorted")
    public ResponseEntity<List<Payment>> getPaymentsSorted() {
        return ResponseEntity.ok(processor.getPaymentsSortedByAmount());
    }

    /**
     * Get payment statistics.
     * GET /api/payments/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<PaymentProcessor.PaymentStatistics> getStatistics() {
        return ResponseEntity.ok(processor.calculateStatistics());
    }

    /**
     * Get payment count by status.
     * GET /api/payments/statistics/by-status
     */
    @GetMapping("/statistics/by-status")
    public ResponseEntity<Map<PaymentStatus, Long>> getCountByStatus() {
        return ResponseEntity.ok(processor.getPaymentCountByStatus());
    }

    /**
     * Get total amount by currency.
     * GET /api/payments/statistics/by-currency
     */
    @GetMapping("/statistics/by-currency")
    public ResponseEntity<Map<String, BigDecimal>> getAmountByCurrency() {
        return ResponseEntity.ok(processor.getTotalAmountByCurrency());
    }

    /**
     * Validate a payment.
     * POST /api/payments/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<String> validatePayment(@RequestBody Payment payment) {
        String validation = payment.validate();
        return ResponseEntity.ok(validation);
    }

    /**
     * Clear all payments.
     * DELETE /api/payments
     */
    @DeleteMapping
    public ResponseEntity<Void> clearPayments() {
        processor.clear();
        return ResponseEntity.noContent().build();
    }

    /**
     * Get total payment count.
     * GET /api/payments/count
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> getPaymentCount() {
        return ResponseEntity.ok(processor.getTotalPaymentCount());
    }
}
