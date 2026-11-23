package no.nav.dagpenger.events

import io.prometheus.client.Summary

val packetPayloadByteSize =
    Summary
        .build()
        .name("payload_size_bytes")
        .help("Packet payload size in bytes.")
        .register()
