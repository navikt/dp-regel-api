package no.nav.dagpenger.regel.api.routes

import io.ktor.application.Application
import io.mockk.mockk
import no.nav.dagpenger.regel.api.DagpengerBehovProducer
import no.nav.dagpenger.regel.api.api
import no.nav.dagpenger.regel.api.db.SubsumsjonStore

fun MockApi(
    subsumsjonStore: SubsumsjonStore = mockk(),
    kafkaDagpengerBehovProducer: DagpengerBehovProducer = mockk()

): Application.() -> Unit {
    return fun Application.() {
        api(
            subsumsjonStore,
            kafkaDagpengerBehovProducer
        )
    }
}
