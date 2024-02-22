import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    alias(libs.plugins.kotlin)
    alias(libs.plugins.spotless)
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

application {
    applicationName = "dp-regel-api"
    mainClass.set("no.nav.dagpenger.regel.api.RegelApiKt")
}

dependencies {
    constraints {
        testRuntimeOnly("org.xerial.snappy:snappy-java:1.1.8.2") {
            because("Required on M1 cpus")
        }
    }
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.navikt:dagpenger-streams:20231220.ec218b")
    implementation("com.github.navikt:dp-inntekt-kontrakter:1_20231220.55a8a9")
    implementation("com.github.navikt:dagpenger-events:20231220.3050bf")

    implementation(Jackson.core)
    implementation(Jackson.kotlin)
    implementation(Jackson.jsr310)
    implementation(Kafka.clients)
    implementation(Kafka.streams)

    implementation(Ktor2.Server.library("netty"))
    implementation(Ktor2.Server.library("default-headers"))
    implementation(Ktor2.Server.library("call-logging"))
    implementation(Ktor2.Server.library("status-pages"))
    implementation(Ktor2.Server.library("auth"))
    implementation(Ktor2.Server.library("auth-jwt"))
    implementation(Ktor2.Server.library("locations"))
    implementation(Ktor2.Server.library("metrics-micrometer"))
    implementation(Ktor2.Server.library("content-negotiation"))
    implementation("io.ktor:ktor-serialization-jackson:${Ktor2.version}")

    implementation(Ktor2.Client.library("core"))
    implementation(Ktor2.Client.library("cio"))
    implementation(Ktor2.Client.library("apache"))

    implementation(Micrometer.prometheusRegistry)

    implementation(Log4j2.api)
    implementation(Log4j2.core)
    implementation(Log4j2.slf4j)
    implementation(Log4j2.library("layout-template-json"))
    implementation(Kotlin.Logging.kotlinLogging)

    implementation(Slf4j.api)

    implementation(Ulid.ulid)

    implementation(Database.Flyway)
    implementation(Database.HikariCP)
    implementation(Database.Postgres)
    implementation(Database.Kotlinquery)
    implementation(Konfig.konfig)

    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Prometheus.log4j2)

    testImplementation(kotlin("test"))
    testImplementation(Ktor2.Server.library("test-host"))
    testImplementation(Junit5.api)
    testImplementation(KoTest.assertions)
    testImplementation(KoTest.property)
    testImplementation(KoTest.runner)
    testImplementation(TestContainers.postgresql)
    testImplementation(TestContainers.kafka)
    testImplementation(Kafka.streamTestUtils)
    testImplementation("no.nav.security:mock-oauth2-server:0.5.7")

    testImplementation(Mockk.mockk)

    testRuntimeOnly(Junit5.engine)
    testRuntimeOnly(Junit5.vintageEngine)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

configurations {
    this.all {
        exclude(group = "ch.qos.logback")
    }
}

tasks.withType<Jar>().configureEach {
    dependsOn("test")
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        ktlint()
    }

    kotlinGradle {
        ktlint()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "7.3.3"
}

tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}

tasks.withType<ShadowJar> {
    transform(Log4j2PluginsCacheFileTransformer::class.java)
    mergeServiceFiles()
}
