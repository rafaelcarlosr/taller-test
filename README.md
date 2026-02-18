# Payment Statistics Calculator

A Java 25 application that processes payment transactions and calculates comprehensive statistics using modern Java features including String Templates, Structured Concurrency, and Enhanced Pattern Matching.

## Features

- **Payment Management**: Add, retrieve, and filter payments by status
- **Statistics Calculation**:
  - Total payment count
  - Total and average amounts for successful payments
  - Payment counts by status
  - Total amounts by currency
- **Advanced Operations**:
  - Sort payments by amount
  - Filter using Java Streams
  - Parallel processing with CompletableFuture
  - Thread-safe operations with ConcurrentHashMap

## Project Structure

```
src/main/java/com/payment/
├── model/
│   ├── Payment.java          # Payment entity with proper encapsulation
│   └── PaymentStatus.java    # Enum for payment states (PENDING, SUCCESS, FAILED)
├── processor/
│   └── PaymentProcessor.java # Core processing logic and statistics
└── Main.java                 # Demo application
```

## Requirements

- **Java 25** (or compatible JDK with preview features enabled)
- Gradle 8.12+

### Java 25 Features Used
- ✨ **String Templates (STR)**: Cleaner string formatting
- ✨ **Structured Concurrency**: Better async processing with `StructuredTaskScope`
- ✨ **Enhanced Pattern Matching**: Advanced switch expressions
- ✨ **Unnamed Variables**: More explicit intent in code

## Building the Project

```bash
./gradlew build
```

## Running the Application

```bash
./gradlew run
```

## Running Tests

```bash
./gradlew test
```

## Key Components

### Payment Class
- Immutable payment entity with validation
- Fields: `id`, `amount`, `currency`, `status`
- Proper encapsulation with getters
- Overridden `toString()`, `equals()`, and `hashCode()`

### PaymentProcessor Class
- **addPayment()**: Add new payments
- **getAllPayments()**: Retrieve all payments
- **getPaymentsByStatus()**: Filter by status
- **getTotalPaymentCount()**: Count all payments
- **getTotalSuccessfulAmount()**: Sum successful payment amounts
- **getAverageSuccessfulAmount()**: Calculate average successful amount
- **getPaymentsSortedByAmount()**: Sort by amount (descending)
- **processPaymentsAsync()**: Parallel processing with Structured Concurrency (Java 25)
- **validatePaymentsAsync()**: Parallel validation with return values
- **calculateStatistics()**: Get comprehensive statistics

### Bonus Features Implemented
✅ Sorting by amount (descending)
✅ Java Streams for filtering and statistics
✅ Structured Concurrency (Java 25)
✅ Thread-safe operations
✅ Record class for statistics
✅ Comprehensive error handling

## Example Output

```
=== Payment Statistics Calculator ===

1. Adding payments sequentially...
✓ Added 10 payments

2. All Payments:
Payment{id='PAY001', amount=150.00, currency='USD', status=SUCCESS}
...

3. Payments by Status:
  SUCCESS: 6 payments
  FAILED: 2 payments
  PENDING: 2 payments

4. Payment Statistics:
Total Payments: 10
Successful: 6
Failed: 2
Pending: 2
Total Successful Amount: $1427.00
Average Successful Amount: $237.83
```

## License

MIT License
