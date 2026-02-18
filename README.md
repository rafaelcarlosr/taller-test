# Payment Statistics Calculator

A production-ready **Spring Boot 3.5** REST API for payment processing and statistics, built with **Java 25** featuring:
- 💰 **BigDecimal** for financial precision
- 🗄️ **PostgreSQL** database with JPA
- ⚡ **Redis** caching for performance
- 📦 **JPA Entity** with timestamps
- 🎯 **Pattern Matching** with guards (Java 25)
- 🧵 **Virtual Threads** for concurrency
- 🏗️ **Kotlin DSL** for Gradle
- 🐳 **Devcontainer** for local development
- 🧪 **Testcontainers** support

## Quick Start

### Option 1: With Devcontainer (PostgreSQL + Redis)

```bash
# Open in VS Code with Dev Containers extension
# Services (PostgreSQL & Redis) will start automatically

./gradlew bootRun
```

### Option 2: Without Docker (H2 in-memory)

```bash
# Uses H2 database for development
./gradlew bootRun --args='--spring.profiles.active=test'
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

# Get payments sorted by amount (cached)
curl http://localhost:8080/api/payments/sorted
```

### Statistics (Cached with Redis)
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
├── main/java/com/taller/test/
│   ├── PaymentStatisticsApplication.java  # Spring Boot main
│   ├── controller/
│   │   └── PaymentController.java         # REST endpoints
│   ├── config/
│   │   └── CacheConfig.java               # Redis cache config
│   ├── model/
│   │   ├── Payment.java                   # JPA entity with BigDecimal
│   │   └── PaymentStatus.java             # Enum
│   ├── processor/
│   │   └── PaymentProcessor.java          # Business logic with transactions
│   └── repository/
│       └── PaymentRepository.java         # Spring Data JPA repository
└── test/java/com/taller/test/
    ├── TestcontainersConfiguration.java   # Testcontainers setup
    ├── e2e/PaymentApiE2ETest.java         # E2E integration tests
    ├── model/PaymentTest.java             # Unit tests
    └── processor/PaymentProcessorTest.java # Integration tests
```

## Database Configuration

### PostgreSQL (Production)

Configured in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payment_stats
    username: payment_user
    password: payment_pass

  jpa:
    hibernate:
      ddl-auto: update
```

### H2 (Testing)

Configured in `application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
```

## Redis Caching

Cache configuration with custom TTL:

- **paymentsByStatus**: 5 minutes
- **paymentsSorted**: 5 minutes
- **paymentStatistics**: 2 minutes

Cache is automatically invalidated on data modifications (create, update, delete).

## Devcontainer Setup

The `.devcontainer` directory includes:

- **PostgreSQL 16** (port 5432)
- **Redis 7** (port 6379)
- **Java 25** runtime
- Persistent volumes for data

```bash
# Services defined in docker-compose.yml
docker compose up -d
```

## Build & Test

```bash
# Build (includes fast tests)
./gradlew build

# Run fast tests only (H2 in-memory - default)
./gradlew test
./gradlew testFast

# Run PostgreSQL E2E tests (requires Docker)
./gradlew testPostgreSQL

# Run ALL tests including PostgreSQL
./gradlew testAll

# Run application with PostgreSQL (requires Docker)
./gradlew bootRun

# Run with H2 for development (no Docker needed)
./gradlew bootRun --args='--spring.profiles.active=test'

# Run with custom port
./gradlew bootRun --args='--server.port=9090'
```

## Technologies

- **Spring Boot** 3.5.10 (with Java 25 support)
- **Java** 25
- **PostgreSQL** 16
- **Redis** 7
- **H2** 2.x (for testing)
- **Gradle** 8.12 with Kotlin DSL
- **JUnit** 5
- **Testcontainers** 1.20.4

## Key Features

### BigDecimal for Money
```java
// Exact decimal arithmetic
BigDecimal amount = new BigDecimal("0.1");
BigDecimal result = amount.add(new BigDecimal("0.2"));
// = 0.3 exactly (no floating-point errors)
```

### JPA Entity with Timestamps
```java
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    private String id;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### Pattern Matching (Java 25)
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

### Redis Caching
```java
@Cacheable(value = "paymentsByStatus", key = "#status")
List<Payment> findByStatus(PaymentStatus status);

@CacheEvict(value = {"paymentsByStatus", "paymentsSorted"}, allEntries = true)
<S extends Payment> S save(S entity);
```

## Testing

**Three-tier test strategy:**

### 1. Fast Tests (H2 - Default) ⚡
**34 tests, ~40 seconds**

- **13 unit tests** - Payment model validation
- **15 integration tests** - PaymentProcessor with H2
- **6 E2E tests** - Full REST API workflow
- **No Docker required** - runs anywhere

```bash
# Run fast tests (default)
./gradlew test
./gradlew testFast
./gradlew build  # Includes fast tests
```

### 2. PostgreSQL Tests (DB-Specific) 🐘
**6 PostgreSQL E2E tests, ~60 seconds, requires Docker**

Tests PostgreSQL-specific features:
- `NUMERIC(19,2)` decimal precision
- Aggregate functions (`SUM`, `COUNT`, `GROUP BY`)
- `ORDER BY` behavior & transaction rollback
- `TIMESTAMP` handling & batch inserts

```bash
./gradlew testPostgreSQL  # Requires Docker
```

### 3. Full Stack Tests (PostgreSQL + Redis) 🚀
**6 full stack tests, ~80 seconds, requires Docker**

Tests production-like environment with **caching**:
- ✅ Redis cache hit/miss behavior
- ✅ Cache invalidation on create/delete
- ✅ Performance comparison (cached vs uncached)
- ✅ Complete PostgreSQL + Redis integration

```bash
./gradlew testFullStack  # Requires Docker
./gradlew testAll        # All 46 tests
```

### Test Comparison

| Tier | Tests | Duration | Database | Cache | Docker | Use Case |
|------|-------|----------|----------|-------|--------|----------|
| **Fast** | 34 | ~40s | H2 | ❌ | ❌ | CI/CD, dev iteration |
| **PostgreSQL** | 6 | ~60s | PostgreSQL | ❌ | ✅ | DB-specific bugs |
| **Full Stack** | 6 | ~80s | PostgreSQL | Redis | ✅ | Production validation |

**Total: 46 tests**

### Run Specific Tests

```bash
./gradlew test --tests PaymentTest                 # Unit
./gradlew test --tests PaymentProcessorTest         # Integration
./gradlew test --tests PaymentApiE2ETest            # Fast E2E
./gradlew test --tests PaymentApiPostgreSQLTest     # PostgreSQL (Docker)
./gradlew test --tests PaymentApiFullStackTest      # Full Stack (Docker)
```

## Configuration Profiles

### Default (Development)
- PostgreSQL database
- Redis caching enabled
- SQL logging: INFO level

### Test
- H2 in-memory database
- Redis disabled
- SQL logging: DEBUG level

```bash
# Run with test profile
./gradlew bootRun --args='--spring.profiles.active=test'
```

## Performance Features

1. **Redis Caching**
   - Frequently accessed queries cached
   - Automatic cache invalidation on updates
   - Configurable TTL per cache

2. **JPA Optimizations**
   - Lazy loading for relationships
   - Connection pooling with HikariCP
   - Database indexes on frequently queried fields

3. **Virtual Threads**
   - Lightweight concurrency for async operations
   - No thread pool limits
   - Better resource utilization

## Docker Compose

For local development without devcontainer:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: payment_stats
      POSTGRES_USER: payment_user
      POSTGRES_PASSWORD: payment_pass

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

## License

MIT
