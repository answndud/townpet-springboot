import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.toolchain.JavaLanguageVersion
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.0.2"
    id("net.ltgt.errorprone") version "4.1.0"
}

group = "com.townpet"
version = "0.1.0-SNAPSHOT"
description = "TownPet Spring Boot community platform"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
}

dependencyLocking {
    lockAllConfigurations()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:2.1.0")
    }
}

dependencies {
    constraints {
        implementation("org.bouncycastle:bcprov-jdk18on:1.81.1")
    }

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("io.minio:minio:8.6.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-session-jdbc")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-events-jdbc")
    implementation("org.springframework.modulith:spring-modulith-events-jackson")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql:42.7.12")
    developmentOnly("com.h2database:h2")

    errorprone("com.google.errorprone:error_prone_core:2.50.0")
    errorprone("com.uber.nullaway:nullaway:0.13.8")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    testRuntimeOnly("com.h2database:h2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        option("NullAway:AnnotatedPackages", "com.townpet")
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.errorprone.error("NullAway")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Testcontainers-backed suites must remain single-forked to avoid
    // starting duplicate PostgreSQL containers and exhausting local Docker RAM.
    maxParallelForks = 1
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.36.1")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

val testSourceSet = sourceSets.named("test")
fun registerVerificationTestTask(name: String, descriptionText: String) {
    tasks.register<Test>(name) {
        group = "verification"
        description = descriptionText
        testClassesDirs = testSourceSet.get().output.classesDirs
        classpath = testSourceSet.get().runtimeClasspath
        useJUnitPlatform()
        shouldRunAfter(tasks.named("test"))
    }
}

registerVerificationTestTask("integrationTest", "Runs integration tests.")
registerVerificationTestTask("modulithTest", "Runs Spring Modulith architecture tests.")
registerVerificationTestTask("migrationTest", "Runs database migration tests.")
registerVerificationTestTask("performanceTest", "Runs controlled performance tests.")
registerVerificationTestTask("parityInventoryTest", "Runs legacy page and API inventory tests.")
tasks.named<Test>("integrationTest") {
    filter {
        includeTestsMatching("com.townpet.*.*ControllerTest")
    }
}
tasks.named<Test>("modulithTest") {
    filter {
        includeTestsMatching("com.townpet.architecture.*")
    }
}
tasks.named<Test>("migrationTest") {
    filter {
        includeTestsMatching("com.townpet.platform.*")
    }
}
tasks.named<Test>("parityInventoryTest") {
    filter {
        includeTestsMatching("com.townpet.parity.*")
    }
}
tasks.named<Test>("performanceTest") {
    filter {
        includeTestsMatching("com.townpet.performance.*")
    }
}

tasks.named("check") {
    dependsOn(tasks.named("spotlessCheck"))
    dependsOn(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    finalizedBy(tasks.named("jacocoTestCoverageVerification"))
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.40".toBigDecimal()
            }
        }
    }
}
