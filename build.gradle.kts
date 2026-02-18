plugins {
    java
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.payment"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    runtimeOnly("org.postgresql:postgresql")

    // H2 for in-memory testing
    testRuntimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
}

// Configure ALL Test tasks
tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

// Configure only the default "test" task to exclude slow tests
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("postgresql", "fullstack", "slow")
    }
}

// Task for running only fast tests (H2 database)
tasks.register<Test>("testFast") {
    group = "verification"
    description = "Run fast tests with H2 in-memory database"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        excludeTags("postgresql", "fullstack", "slow")
    }
}

// Task for running PostgreSQL tests (requires Docker)
tasks.register<Test>("testPostgreSQL") {
    group = "verification"
    description = "Run E2E tests with PostgreSQL via Testcontainers (requires Docker)"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        includeTags("postgresql")
    }

    shouldRunAfter("test")

    doFirst {
        println("\n")
        println("=".repeat(60))
        println("Starting PostgreSQL E2E Tests")
        println("Testcontainers PostgreSQL")
        println("=".repeat(60))
        println("\n")
    }
}

// Task for running full stack tests (PostgreSQL + Redis)
tasks.register<Test>("testFullStack") {
    group = "verification"
    description = "Run full stack E2E tests with PostgreSQL + Redis (requires Docker)"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        includeTags("fullstack")
    }

    shouldRunAfter("test")

    doFirst {
        println("\n")
        println("=".repeat(60))
        println("Starting Full Stack E2E Tests")
        println("PostgreSQL + Redis via Testcontainers")
        println("=".repeat(60))
        println("\n")
    }

    doLast {
        println("\n")
        println("=".repeat(60))
        println("Full Stack Tests Complete!")
        println("=".repeat(60))
        println("\n")
    }
}

// Task for running all tests including PostgreSQL and Full Stack
tasks.register<Test>("testAll") {
    group = "verification"
    description = "Run all tests including PostgreSQL and Full Stack tests (requires Docker)"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        // No tag filtering - run everything
    }
}
