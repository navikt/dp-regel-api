package no.nav.dagpenger.events

import java.net.URI
import java.time.LocalDateTime

data class Breadcrumb(
    val appId: String,
    val dateTime: LocalDateTime,
    val instance: URI = URI.create("about:blank"),
) {
    companion object {
        fun fromJson(json: Map<String, Any>): Breadcrumb =
            Breadcrumb(
                json["appId"] as String,
                LocalDateTime.parse(json["dateTime"] as String),
                URI.create(json["instance"] as String),
            )
    }
}
