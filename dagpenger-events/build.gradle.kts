plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:8.3.0")
    implementation(libs.prometheus.simpleclient)

    val moshiVersion = "1.15.2"
    implementation("com.squareup.moshi:moshi:$moshiVersion")
    implementation("com.squareup.moshi:moshi-adapters:$moshiVersion")
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")

    testImplementation("org.json:json:20251224")
    testImplementation(libs.kotest.assertions.core)
    testImplementation("org.junit.jupiter:junit-jupiter-api:${libs.versions.junit.get()}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${libs.versions.junit.get()}")
}
