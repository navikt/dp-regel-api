import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    kotlin("jvm") version "1.3.21"
    id("com.diffplug.gradle.spotless") version "3.13.0"
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

apply {
    plugin("com.diffplug.gradle.spotless")
}

repositories {
    mavenCentral()
    jcenter()
    maven("http://packages.confluent.io/maven/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

application {
    applicationName = "dp-regel-api"
    mainClassName = "no.nav.dagpenger.regel.api.RegelApiKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
val ktorVersion = "1.2.0"
val kotlinLoggingVersion = "1.6.22"
val log4j2Version = "2.11.1"
val jupiterVersion = "5.3.2"
val kafkaVersion = "2.0.1"
val confluentVersion = "5.0.2"
val moshiVersion = "1.8.0"
val ktorMoshiVersion = "1.0.1"
val testcontainers_version = "1.11.2"
val flywayVersion = "6.0.0-beta"
val hikariVersion = "3.3.1"
val postgresVersion = "42.2.5"
val vaultJdbcVersion = "1.3.1"
val kotliqueryVersion = "1.3.0"
val vaultJavaDriverVersion = "3.1.0"
val konfigVersion = "1.6.10.0"
val mockkVersion = "1.9.3"
val prometheusVersion = "0.6.0"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.navikt:dagpenger-events:2019.05.28-13.44.ab5008b3ee50")
    implementation("com.github.navikt:dagpenger-streams:2019.06.03-21.15.cf80ca7a33d5")
    implementation("io.ktor:ktor-server:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("com.squareup.moshi:moshi-adapters:$moshiVersion")
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    implementation("com.squareup.moshi:moshi:$moshiVersion")
    implementation("com.ryanharter.ktor:ktor-moshi:$ktorMoshiVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    implementation("com.vlkan.log4j2:log4j2-logstash-layout-fatjar:0.15")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:8.2.0")
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    implementation("io.confluent:kafka-streams-avro-serde:$confluentVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    implementation("com.natpryce:konfig:$konfigVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion") {
        exclude(module = "slf4j-simple")
        exclude(module = "slf4j-api")
    }
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_log4j2:$prometheusVersion")

    runtime("com.bettercloud:vault-java-driver:$vaultJavaDriverVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainers_version")
    testImplementation("org.testcontainers:kafka:$testcontainers_version")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$jupiterVersion")
}

spotless {
    kotlin {
        ktlint("0.31.0")
    }
    kotlinGradle {
        target("*.gradle.kts", "additionalScripts/*.gradle.kts")
        ktlint("0.31.0")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xuse-experimental=io.ktor.locations.KtorExperimentalLocationsAPI")
    kotlinOptions.jvmTarget = "1.8"
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
    gradleVersion = "5.0"
}
