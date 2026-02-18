package com.taller.test.processor;

import com.taller.test.model.Payment;
import com.taller.test.model.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Processes and manages payment transactions with statistics calculation.
 * Uses Java 25 features: Virtual Threads, Enhanced Pattern Matching
 * Uses BigDecimal for accurate financial calculations.
 */
public class PaymentProcessor {
    private final Map<String, Payment> payments;
    private final ExecutorService virtualThreadExecutor;

    public PaymentProcessor() {
        this.payments = new ConcurrentHashMap<>();
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Adds a new payment to the processor.
     *
     * @param payment the payment to add
     * @throws IllegalArgumentException if a payment with the same ID already exists
     */
    public void addPayment(Payment payment) {
        Objects.requireNonNull(payment, "Payment cannot be null");

        if (payments.containsKey(payment.id())) {
            throw new IllegalArgumentException(
                String.format("Payment with ID %s already exists", payment.id())
            );
        }

        payments.put(payment.id(), payment);
    }

    /**
     * Retrieves all payments.
     *
     * @return an unmodifiable list of all payments
     */
    public List<Payment> getAllPayments() {
        return List.copyOf(payments.values());
    }

    /**
     * Retrieves payments filtered by status.
     *
     * @param status the status to filter by
     * @return a list of payments with the specified status
     */
    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        Objects.requireNonNull(status, "Status cannot be null");

        return payments.values().stream()
                .filter(payment -> payment.status() == status)
                .toList();
    }

    /**
     * Gets the total number of payments.
     *
     * @return the total count of payments
     */
    public int getTotalPaymentCount() {
        return payments.size();
    }

    /**
     * Calculates the total amount of successful payments.
     *
     * @return the sum of all successful payment amounts
     */
    public BigDecimal getTotalSuccessfulAmount() {
        return payments.values().stream()
                .filter(payment -> payment.status() == PaymentStatus.SUCCESS)
                .map(Payment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the average amount of successful payments.
     *
     * @return the average amount, or BigDecimal.ZERO if no successful payments exist
     */
    public BigDecimal getAverageSuccessfulAmount() {
        List<Payment> successfulPayments = payments.values().stream()
                .filter(payment -> payment.status() == PaymentStatus.SUCCESS)
                .toList();

        if (successfulPayments.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = successfulPayments.stream()
                .map(Payment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(
            BigDecimal.valueOf(successfulPayments.size()),
            2,
            RoundingMode.HALF_UP
        );
    }

    /**
     * Retrieves payments sorted by amount in descending order.
     *
     * @return a list of payments sorted by amount (highest first)
     */
    public List<Payment> getPaymentsSortedByAmount() {
        return payments.values().stream()
                .sorted(Comparator.comparing(Payment::amount).reversed())
                .toList();
    }

    /**
     * Gets payment statistics grouped by status.
     *
     * @return a map of status to count of payments
     */
    public Map<PaymentStatus, Long> getPaymentCountByStatus() {
        return payments.values().stream()
                .collect(Collectors.groupingBy(
                        Payment::status,
                        Collectors.counting()
                ));
    }

    /**
     * Gets payment statistics grouped by currency.
     *
     * @return a map of currency to total amount
     */
    public Map<String, BigDecimal> getTotalAmountByCurrency() {
        return payments.values().stream()
                .collect(Collectors.groupingBy(
                        Payment::currency,
                        Collectors.reducing(
                            BigDecimal.ZERO,
                            Payment::amount,
                            BigDecimal::add
                        )
                ));
    }

    /**
     * Processes multiple payments in parallel using Virtual Threads (Java 21+).
     * Virtual threads provide lightweight, scalable concurrency.
     *
     * @param paymentsToProcess the list of payments to process
     * @return a CompletableFuture that completes when all payments are processed
     */
    public CompletableFuture<Void> processPaymentsAsync(List<Payment> paymentsToProcess) {
        List<CompletableFuture<Void>> futures = paymentsToProcess.stream()
                .map(payment -> CompletableFuture.runAsync(() -> {
                    try {
                        // Simulate processing delay
                        Thread.sleep(100);
                        addPayment(payment);
                        System.out.println(String.format("Processed: %s", payment.id()));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(
                            String.format("Payment processing interrupted for %s", payment.id()), e
                        );
                    }
                }, virtualThreadExecutor))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Validates payments in parallel using Virtual Threads.
     * Java 25 Feature: Enhanced pattern matching for validation logic
     */
    public CompletableFuture<List<String>> validatePaymentsAsync(List<Payment> paymentsToValidate) {
        List<CompletableFuture<String>> futures = paymentsToValidate.stream()
                .map(payment -> CompletableFuture.supplyAsync(() -> {
                    // Use enhanced pattern matching for validation
                    return switch (payment) {
                        case Payment p when p.amount().compareTo(new BigDecimal("10000")) > 0 ->
                            String.format("⚠ High-value payment: %s", p.id());
                        case Payment p when p.status() == PaymentStatus.FAILED ->
                            String.format("✗ Failed payment: %s", p.id());
                        case Payment p when p.status() == PaymentStatus.PENDING ->
                            String.format("⏳ Pending payment: %s", p.id());
                        default ->
                            String.format("✓ Valid: %s", payment.id());
                    };
                }, virtualThreadExecutor))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    /**
     * Calculates comprehensive payment statistics.
     *
     * @return a PaymentStatistics object containing all calculated statistics
     */
    public PaymentStatistics calculateStatistics() {
        long successCount = payments.values().stream()
                .filter(p -> p.status() == PaymentStatus.SUCCESS)
                .count();

        long failedCount = payments.values().stream()
                .filter(p -> p.status() == PaymentStatus.FAILED)
                .count();

        long pendingCount = payments.values().stream()
                .filter(p -> p.status() == PaymentStatus.PENDING)
                .count();

        return new PaymentStatistics(
                getTotalPaymentCount(),
                successCount,
                failedCount,
                pendingCount,
                getTotalSuccessfulAmount(),
                getAverageSuccessfulAmount()
        );
    }

    /**
     * Clears all payments from the processor.
     */
    public void clear() {
        payments.clear();
    }

    /**
     * Shuts down the virtual thread executor.
     */
    public void shutdown() {
        virtualThreadExecutor.shutdown();
    }

    /**
     * Record class containing payment statistics.
     * Uses modern Java record features with custom methods.
     * Uses BigDecimal for accurate financial calculations.
     */
    public record PaymentStatistics(
            int totalPayments,
            long successfulPayments,
            long failedPayments,
            long pendingPayments,
            BigDecimal totalSuccessfulAmount,
            BigDecimal averageSuccessfulAmount
    ) {
        @Override
        public String toString() {
            return String.format("""
                    Payment Statistics:
                    ------------------
                    Total Payments: %d
                    Successful: %d
                    Failed: %d
                    Pending: %d
                    Total Successful Amount: $%s
                    Average Successful Amount: $%s
                    """,
                    totalPayments,
                    successfulPayments,
                    failedPayments,
                    pendingPayments,
                    totalSuccessfulAmount,
                    averageSuccessfulAmount
            );
        }

        /**
         * Returns a concise summary using pattern matching.
         * Java 25 Feature: Enhanced switch with pattern matching
         */
        public String getSummary() {
            return switch (totalPayments) {
                case 0 -> "No payments processed";
                case 1 -> String.format("1 payment: %s", getStatusDescription());
                default -> String.format("%d payments: %s", totalPayments, getStatusDescription());
            };
        }

        private String getStatusDescription() {
            double successRate = totalPayments > 0
                ? (successfulPayments * 100.0 / totalPayments)
                : 0.0;
            return String.format("%.1f%% success rate", successRate);
        }
    }
}
