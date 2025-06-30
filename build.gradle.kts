import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

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
    implementation("com.github.navikt:dp-inntekt-kontrakter:1_20231220.55a8a9")
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

    implementation("io.micrometer:micrometer-registry-prometheus:1.15.1")

    implementation("io.prometheus:client_java:1.3.8")
    implementation("io.prometheus:prometheus-metrics-core:1.3.8")
    implementation("io.prometheus:prometheus-metrics-instrumentation-jvm:1.3.8")

    val log4j2Version = "2.25.0"
    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-layout-template-json:$log4j2Version")
    implementation("org.slf4j:slf4j-api:2.0.17")

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
    testImplementation("org.testcontainers:kafka:1.21.3")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:7.9.1-ce")
    testImplementation("no.nav.security:mock-oauth2-server:2.2.1")

    testImplementation(libs.mockk)

    val junitVersion = libs.versions.junit.get()
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

configurations {
    this.all {
        exclude(group = "ch.qos.logback")
    }
}

tasks.withType<ShadowJar> {
    transform(Log4j2PluginsCacheFileTransformer::class.java)
    mergeServiceFiles()
}
