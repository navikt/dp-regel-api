import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("common")
    application
    alias(libs.plugins.shadow.jar)
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
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.navikt:dp-inntekt-kontrakter:2_20251211.17f9d7")
    implementation("com.github.navikt:dagpenger-events:20250226.cb02d9")
    implementation(libs.bundles.jackson)

    val kafkaVersion = "7.9.1-ce"
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")

    val ktorServerVersion = libs.versions.ktor.get()
    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation("io.ktor:ktor-server-netty:$ktorServerVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorServerVersion")

    implementation(libs.bundles.ktor.client)

    implementation("io.micrometer:micrometer-registry-prometheus:1.16.2")

    implementation("io.prometheus:client_java:1.4.3")
    implementation("io.prometheus:prometheus-metrics-core:1.4.3")
    implementation("io.prometheus:prometheus-metrics-instrumentation-jvm:1.4.3")

    implementation("ch.qos.logback:logback-classic:1.5.26")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    implementation(libs.kotlin.logging)

    implementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:8.3.0")

    implementation(libs.bundles.postgres)
    implementation(libs.konfig)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotest.assertions.core)
    testImplementation("io.kotest:kotest-property-jvm:${libs.versions.kotest.get()}")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:${libs.versions.kotest.get()}")
    testImplementation(libs.testcontainer.postgresql)
    testImplementation("org.testcontainers:kafka:1.21.4")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:7.9.1-ce")
    testImplementation("no.nav.security:mock-oauth2-server:3.0.1")

    testImplementation(libs.mockk)

    val junitVersion = libs.versions.junit.get()
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}
