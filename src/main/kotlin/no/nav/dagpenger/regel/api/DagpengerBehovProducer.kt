package no.nav.dagpenger.regel.api

interface DagpengerBehovProducer {
    fun produceEvent(behov: SubsumsjonsBehov)
}