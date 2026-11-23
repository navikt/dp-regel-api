package no.nav.dagpenger.events

import java.net.URI

data class Problem(
    val type: URI = URI.create("about:blank"),
    val title: String,
    val status: Int? = 500,
    val detail: String? = null,
    val instance: URI = URI.create("about:blank"),
) {
    companion object {
        private val adapter = moshiInstance.adapter<Problem>(Problem::class.java)

        fun fromJson(json: Map<String, Any>): Problem =
            Problem(
                URI.create(json["type"] as String),
                json["title"] as String,
                json["status"]?.toString()?.toDouble()?.toInt(),
                json["detail"] as String?,
                URI.create(json["instance"] as String),
            )
    }

    val toJson: String? get() = adapter.toJson(this)
}
