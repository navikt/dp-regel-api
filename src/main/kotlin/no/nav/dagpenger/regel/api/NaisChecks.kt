package no.nav.dagpenger.regel.api

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respondText
import io.ktor.routing.Routing

@Location("/isAlive")
class IsAlive

@Location("/isReady")
class IsReady

fun Routing.naischecks() {
    get<IsAlive> {
        call.respondText("ALIVE", ContentType.Text.Plain)
    }
    get<IsReady> {
        call.respondText("READY", ContentType.Text.Plain)
    }
}
