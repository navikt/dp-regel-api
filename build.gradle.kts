import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("application")
    kotlin("jvm") version "1.3.10"
    id("com.diffplug.gradle.spotless") version "3.13.0"
}

apply {
    plugin("com.diffplug.gradle.spotless")
}

repositories {
    jcenter()
}

application {
    applicationName = "dagpenger-regel-api"
    mainClassName = "no.nav.dagpenger.regel.Api"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val ktorVersion = "1.1.1"
val swagger_version = "3.1.7"
val kotlinLoggingVersion = "1.4.9"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")

    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

    implementation("de.nielsfalk.ktor:ktor-swagger:0.4.0")

    testImplementation("junit:junit:4.12")
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts", "additionalScripts/*.gradle.kts")
        ktlint()
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}