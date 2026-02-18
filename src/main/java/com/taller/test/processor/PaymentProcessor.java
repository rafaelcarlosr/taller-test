package com.taller.test.processor;

import com.taller.test.model.Payment;
import com.taller.test.model.PaymentStatus;
import com.taller.test.repository.PaymentRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Processes and manages payment transactions with statistics calculation.
 * Uses Java 25 features: Virtual Threads, Enhanced Pattern Matching
 * Uses BigDecimal for accurate financial calculations.
 * Integrated with PostgreSQL via JPA and Redis caching.
 */
@Service
@Transactional
public class PaymentProcessor {
    private final PaymentRepository repository;
    private final ExecutorService virtualThreadExecutor;

    public PaymentProcessor(PaymentRepository repository) {
        this.repository = repository;
        // Java 21+ Virtual Threads - lightweight, scalable concurrency
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Adds a new payment to the database.
     *
     * @param payment the payment to add
     * @throws IllegalArgumentException if a payment with the same ID already exists
     */
    public Payment addPayment(Payment payment) {
        Objects.requireNonNull(payment, "Payment cannot be null");

        if (repository.existsById(payment.getId())) {
            throw new IllegalArgumentException(
                String.format("Payment with ID %s already exists", payment.getId())
            );
        }

        return repository.save(payment);
    }

    /**
     * Retrieves a payment by ID.
     *
     * @param id the payment ID
     * @return Optional containing the payment if found
     */
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentById(String id) {
        return repository.findById(id);
    }

    /**
     * Retrieves all payments.
     *
     * @return a list of all payments
     */
    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return repository.findAll();
    }

    /**
     * Retrieves payments filtered by status.
     * Results are cached in Redis.
     *
     * @param status the status to filter by
     * @return a list of payments with the specified status
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByStatus(PaymentStatus status) {
        Objects.requireNonNull(status, "Status cannot be null");
        return repository.findByStatus(status);
    }

    /**
     * Gets the total number of payments.
     *
     * @return the total count of payments
     */
    @Transactional(readOnly = true)
    public long getTotalPaymentCount() {
        return repository.count();
    }

    /**
     * Calculates the total amount of successful payments.
     *
     * @return the sum of all successful payment amounts
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalSuccessfulAmount() {
        BigDecimal total = repository.sumAmountByStatus(PaymentStatus.SUCCESS);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Calculates the average amount of successful payments.
     *
     * @return the average amount, or BigDecimal.ZERO if no successful payments exist
     */
    @Transactional(readOnly = true)
    public BigDecimal getAverageSuccessfulAmount() {
        long successCount = repository.countByStatus(PaymentStatus.SUCCESS);

        if (successCount == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = getTotalSuccessfulAmount();
        return total.divide(
            BigDecimal.valueOf(successCount),
            2,
            RoundingMode.HALF_UP
        );
    }

    /**
     * Retrieves payments sorted by amount in descending order.
     * Results are cached in Redis.
     *
     * @return a list of payments sorted by amount (highest first)
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsSortedByAmount() {
        return repository.findAllByOrderByAmountDesc();
    }

    /**
     * Gets payment statistics grouped by status.
     *
     * @return a map of status to count of payments
     */
    @Transactional(readOnly = true)
    public Map<PaymentStatus, Long> getPaymentCountByStatus() {
        Map<PaymentStatus, Long> result = new HashMap<>();
        for (PaymentStatus status : PaymentStatus.values()) {
            result.put(status, repository.countByStatus(status));
        }
        return result;
    }

    /**
     * Gets payment statistics grouped by currency.
     *
     * @return a map of currency to total amount
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getTotalAmountByCurrency() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Payment::getCurrency,
                        Collectors.reducing(
                            BigDecimal.ZERO,
                            Payment::getAmount,
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
                        System.out.println(String.format("Processed: %s", payment.getId()));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(
                            String.format("Payment processing interrupted for %s", payment.getId()), e
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
                        case Payment p when p.getAmount().compareTo(new BigDecimal("10000")) > 0 ->
                            String.format("⚠ High-value payment: %s", p.getId());
                        case Payment p when p.getStatus() == PaymentStatus.FAILED ->
                            String.format("✗ Failed payment: %s", p.getId());
                        case Payment p when p.getStatus() == PaymentStatus.PENDING ->
                            String.format("⏳ Pending payment: %s", p.getId());
                        default ->
                            String.format("✓ Valid: %s", payment.getId());
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
     * Results are cached in Redis.
     *
     * @return a PaymentStatistics object containing all calculated statistics
     */
    @Transactional(readOnly = true)
    @Cacheable("paymentStatistics")
    public PaymentStatistics calculateStatistics() {
        long successCount = repository.countByStatus(PaymentStatus.SUCCESS);
        long failedCount = repository.countByStatus(PaymentStatus.FAILED);
        long pendingCount = repository.countByStatus(PaymentStatus.PENDING);

        return new PaymentStatistics(
                (int) repository.count(),
                successCount,
                failedCount,
                pendingCount,
                getTotalSuccessfulAmount(),
                getAverageSuccessfulAmount()
        );
    }

    /**
     * Clears all payments from the database.
     */
    public void clear() {
        repository.deleteAll();
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
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
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
