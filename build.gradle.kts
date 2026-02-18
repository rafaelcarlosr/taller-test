plugins {
    java
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.taller.test"
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
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation("org.mockito:mockito-inline:5.2.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}
