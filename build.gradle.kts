import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.toolchain.JavaLanguageVersion
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
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

// Spring Boot 4.1.0's BOM selects a Tomcat patch with known CRITICAL fixes
// available upstream. Force the fixed patch consistently across runtime and
// test classpaths until the BOM is upgraded.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.tomcat.embed" &&
            requested.name in setOf("tomcat-embed-core", "tomcat-embed-el", "tomcat-embed-websocket")) {
            useVersion("11.0.25")
            because("security fixes for the embedded servlet container")
        }
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
        // Keep the embedded servlet container on the security-fixed patch line
        // until the Spring Boot BOM carries the same Tomcat update.
        implementation("org.apache.tomcat.embed:tomcat-embed-core:11.0.25")
        implementation("org.apache.tomcat.embed:tomcat-embed-el:11.0.25")
        implementation("org.apache.tomcat.embed:tomcat-embed-websocket:11.0.25")
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
    // OpenApiContractTest reads the spec by path, so editing it must re-run the tests.
    inputs.dir("api/openapi").withPropertyName("openApiSpec").optional()
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}

val testSourceSet = sourceSets.named("test")
// Keep the default test task as the fast suite so existing local commands
// still work. Database-backed tests carry @Tag("integration") and run in
// integrationTest, so a new integration test is picked up without editing this file.
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

fun registerVerificationTestTask(name: String, descriptionText: String) {
    tasks.register<Test>(name) {
        group = "verification"
        description = descriptionText
        testClassesDirs = testSourceSet.get().output.classesDirs
        classpath = testSourceSet.get().runtimeClasspath
        useJUnitPlatform()
    }
}

registerVerificationTestTask("integrationTest", "Runs integration tests.")
registerVerificationTestTask("modulithTest", "Runs Spring Modulith architecture tests.")
registerVerificationTestTask("migrationTest", "Runs database migration tests.")
registerVerificationTestTask("performanceTest", "Runs controlled performance tests.")
registerVerificationTestTask("parityInventoryTest", "Runs legacy page and API inventory tests.")
tasks.named<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
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

tasks.register("releaseCheck") {
    group = "verification"
    description = "Runs the fast unit suite and every integration test once."
    dependsOn("test", "integrationTest")
}

tasks.named("check") {
    dependsOn(tasks.named("integrationTest"))
}
