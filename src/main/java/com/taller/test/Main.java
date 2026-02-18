package com.taller.test;

import com.taller.test.model.Payment;
import com.taller.test.model.PaymentStatus;
import com.taller.test.processor.PaymentProcessor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Main application demonstrating the Payment Statistics Calculator.
 * Uses Java 25 features: Enhanced Pattern Matching, Virtual Threads
 * Uses BigDecimal for accurate financial calculations
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Payment Statistics Calculator (Java 25 + BigDecimal) ===\n");

        PaymentProcessor processor = new PaymentProcessor();

        // Create sample payments using BigDecimal for amounts
        List<Payment> samplePayments = List.of(
                new Payment("PAY001", new BigDecimal("150.00"), "USD", PaymentStatus.SUCCESS),
                new Payment("PAY002", new BigDecimal("75.50"), "EUR", PaymentStatus.SUCCESS),
                new Payment("PAY003", new BigDecimal("200.00"), "USD", PaymentStatus.FAILED),
                new Payment("PAY004", new BigDecimal("325.75"), "GBP", PaymentStatus.SUCCESS),
                new Payment("PAY005", new BigDecimal("50.00"), "USD", PaymentStatus.PENDING),
                new Payment("PAY006", new BigDecimal("180.25"), "EUR", PaymentStatus.SUCCESS),
                new Payment("PAY007", new BigDecimal("95.00"), "USD", PaymentStatus.FAILED),
                new Payment("PAY008", new BigDecimal("420.50"), "GBP", PaymentStatus.SUCCESS),
                new Payment("PAY009", new BigDecimal("60.00"), "EUR", PaymentStatus.PENDING),
                new Payment("PAY010", new BigDecimal("275.00"), "USD", PaymentStatus.SUCCESS)
        );

        // Demonstration 1: Basic payment processing
        System.out.println("1. Adding payments sequentially...");
        samplePayments.forEach(processor::addPayment);
        System.out.printf("✓ Added %d payments%n%n", processor.getTotalPaymentCount());

        // Demonstration 2: Retrieve and display all payments with descriptions
        System.out.println("2. All Payments (with Java 25 Pattern Matching):");
        processor.getAllPayments().forEach(p -> System.out.printf("  %s%n", p.getDescription()));
        System.out.println();

        // Demonstration 3: Filter payments by status
        System.out.println("3. Payments by Status:");
        for (PaymentStatus status : PaymentStatus.values()) {
            List<Payment> filtered = processor.getPaymentsByStatus(status);
            System.out.printf("  %s: %d payments%n", status, filtered.size());
            filtered.forEach(p -> System.out.printf("    - %s%n", p));
        }
        System.out.println();

        // Demonstration 4: Calculate and display statistics
        System.out.println("4. Payment Statistics:");
        var stats = processor.calculateStatistics();
        System.out.println(stats);
        System.out.printf("  Summary: %s%n%n", stats.getSummary());

        // Demonstration 5: Payment count by status (using streams)
        System.out.println("5. Payment Count by Status (Streams):");
        Map<PaymentStatus, Long> countByStatus = processor.getPaymentCountByStatus();
        countByStatus.forEach((status, count) ->
                System.out.printf("  %s: %d%n", status, count));
        System.out.println();

        // Demonstration 6: Total amount by currency (using streams with BigDecimal)
        System.out.println("6. Total Amount by Currency (BigDecimal precision):");
        Map<String, BigDecimal> amountByCurrency = processor.getTotalAmountByCurrency();
        amountByCurrency.forEach((currency, total) ->
                System.out.printf("  %s: $%s%n", currency, total));
        System.out.println();

        // Demonstration 7: Sort payments by amount (descending)
        System.out.println("7. Payments Sorted by Amount (Highest to Lowest):");
        processor.getPaymentsSortedByAmount()
                .forEach(p -> System.out.printf("  %s - $%s%n", p.id(), p.amount()));
        System.out.println();

        // Demonstration 8: Virtual Threads for parallel processing
        System.out.println("8. Virtual Threads Demo (Java 21+):");
        PaymentProcessor asyncProcessor = new PaymentProcessor();

        List<Payment> asyncPayments = List.of(
                new Payment("ASYNC001", new BigDecimal("100.00"), "USD", PaymentStatus.SUCCESS),
                new Payment("ASYNC002", new BigDecimal("200.00"), "EUR", PaymentStatus.SUCCESS),
                new Payment("ASYNC003", new BigDecimal("300.00"), "GBP", PaymentStatus.SUCCESS)
        );

        System.out.println("  Processing payments with virtual threads...");
        asyncProcessor.processPaymentsAsync(asyncPayments).join();
        System.out.println("  ✓ Virtual thread processing complete!");
        System.out.printf("  Total payments processed: %d%n", asyncProcessor.getTotalPaymentCount());
        System.out.println();

        // Demonstration 9: Validation with enhanced pattern matching
        System.out.println("9. Parallel Validation (Pattern Matching + Virtual Threads):");
        List<String> validationResults = asyncProcessor.validatePaymentsAsync(
            List.of(
                new Payment("VAL001", new BigDecimal("500.00"), "USD", PaymentStatus.SUCCESS),
                new Payment("VAL002", new BigDecimal("15000.00"), "EUR", PaymentStatus.SUCCESS),
                new Payment("VAL003", new BigDecimal("750.00"), "GBP", PaymentStatus.SUCCESS),
                new Payment("VAL004", new BigDecimal("100.00"), "USD", PaymentStatus.FAILED)
            )
        ).join();
        validationResults.forEach(result -> System.out.printf("  %s%n", result));
        System.out.println();

        // Demonstration 10: Advanced analytics with BigDecimal
        System.out.println("10. Advanced Analytics (BigDecimal Precision):");
        BigDecimal totalSuccessful = processor.getTotalSuccessfulAmount();
        BigDecimal avgSuccessful = processor.getAverageSuccessfulAmount();

        System.out.printf("  Total Successful Amount: $%s%n", totalSuccessful);
        System.out.printf("  Average Successful Amount: $%s%n", avgSuccessful);

        // Find largest successful payment
        processor.getAllPayments().stream()
                .filter(p -> p.status() == PaymentStatus.SUCCESS)
                .max(Comparator.comparing(Payment::amount))
                .ifPresent(p -> System.out.printf("  Largest Successful Payment: %s - $%s%n",
                        p.id(), p.amount()));

        // Find smallest successful payment
        processor.getAllPayments().stream()
                .filter(p -> p.status() == PaymentStatus.SUCCESS)
                .min(Comparator.comparing(Payment::amount))
                .ifPresent(p -> System.out.printf("  Smallest Successful Payment: %s - $%s%n",
                        p.id(), p.amount()));

        // Demonstration 11: Payment validation using pattern matching
        System.out.println("\n11. Payment Validation (Pattern Matching):");
        processor.getAllPayments().stream()
                .limit(5)
                .forEach(p -> System.out.printf("  %s: %s%n", p.id(), p.validate()));

        // Demonstration 12: BigDecimal arithmetic precision
        System.out.println("\n12. BigDecimal Precision Demo:");
        BigDecimal amount1 = new BigDecimal("0.1");
        BigDecimal amount2 = new BigDecimal("0.2");
        BigDecimal sum = amount1.add(amount2);
        System.out.printf("  0.1 + 0.2 = %s (exact!)%n", sum);
        System.out.println("  Note: With double, 0.1 + 0.2 = 0.30000000000000004");

        // Cleanup
        processor.shutdown();
        asyncProcessor.shutdown();

        System.out.println("\n=== Demo Complete (Java 25 + BigDecimal Features Demonstrated) ===");
        System.out.println("Features used:");
        System.out.println("  ✓ Enhanced Pattern Matching with Guards");
        System.out.println("  ✓ Virtual Threads for Scalable Concurrency");
        System.out.println("  ✓ Records with Custom Methods");
        System.out.println("  ✓ Pattern Matching in Switch Expressions");
        System.out.println("  ✓ BigDecimal for Precise Financial Calculations");
        System.out.println("  ✓ Modern Stream Operations");
    }
}
