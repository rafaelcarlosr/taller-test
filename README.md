# Payment Statistics Calculator

A modern **Spring Boot 3.4** REST API for payment processing and statistics, built with **Java 21** featuring:
- 💰 **BigDecimal** for financial precision
- 📦 **Records** for immutable data
- 🎯 **Pattern Matching** with guards
- 🧵 **Virtual Threads** for concurrency
- 🏗️ **Kotlin DSL** for Gradle

## Quick Start

```bash
# Run the application
./gradlew bootRun

# The API will start on http://localhost:8080
```

## API Endpoints

### Payment Operations
```bash
# Create a payment
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "id": "PAY001",
    "amount": "150.00",
    "currency": "USD",
    "status": "SUCCESS"
  }'

# Get all payments
curl http://localhost:8080/api/payments

# Get payment by ID
curl http://localhost:8080/api/payments/PAY001

# Get payments by status
curl http://localhost:8080/api/payments/status/SUCCESS

# Get payments sorted by amount
curl http://localhost:8080/api/payments/sorted
```

### Statistics
```bash
# Get comprehensive statistics
curl http://localhost:8080/api/payments/statistics

# Get count by status
curl http://localhost:8080/api/payments/statistics/by-status

# Get total by currency
curl http://localhost:8080/api/payments/statistics/by-currency

# Get payment count
curl http://localhost:8080/api/payments/count
```

### Validation & Management
```bash
# Validate a payment
curl -X POST http://localhost:8080/api/payments/validate \
  -H "Content-Type: application/json" \
  -d '{"id":"TEST","amount":"500.00","currency":"USD","status":"SUCCESS"}'

# Clear all payments
curl -X DELETE http://localhost:8080/api/payments
```

## Project Structure

```
src/
├── main/java/com/payment/
│   ├── PaymentStatisticsApplication.java  # Spring Boot main
│   ├── controller/
│   │   └── PaymentController.java         # REST endpoints
│   ├── config/
│   │   └── PaymentProcessorConfig.java    # Bean configuration
│   ├── model/
│   │   ├── Payment.java                   # Record with BigDecimal
│   │   └── PaymentStatus.java             # Enum
│   └── processor/
│       └── PaymentProcessor.java          # Business logic
└── test/java/com/payment/
    ├── e2e/PaymentApiE2ETest.java         # E2E tests
    ├── model/PaymentTest.java             # Unit tests
    └── processor/PaymentProcessorTest.java
```

## Build & Test

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run with custom port
./gradlew bootRun --args='--server.port=9090'
```

## Technologies

- **Spring Boot** 3.4.2
- **Java** 21
- **Gradle** 8.12 with Kotlin DSL
- **JUnit** 5.11

## Key Features

### BigDecimal for Money
```java
// Exact decimal arithmetic
BigDecimal amount = new BigDecimal("0.1");
BigDecimal result = amount.add(new BigDecimal("0.2"));
// = 0.3 exactly (no floating-point errors)
```

### Records for Immutability
```java
public record Payment(
    String id,
    BigDecimal amount,
    String currency,
    PaymentStatus status
) {
    // Automatic equals, hashCode, toString
    // Compact constructor for validation
}
```

### Pattern Matching
```java
return switch (payment) {
    case Payment p when p.amount().compareTo(BigDecimal.ZERO) <= 0 ->
        "Invalid: amount must be positive";
    case Payment p when p.amount().compareTo(new BigDecimal("1000000")) > 0 ->
        "Warning: high amount";
    default -> "Valid payment";
};
```

### Virtual Threads
```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
// Lightweight, scalable concurrency
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

logging:
  level:
    com.payment: INFO
```

## License

MIT
