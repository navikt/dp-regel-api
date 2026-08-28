package no.nav.dagpenger.regel.api.serder

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.models.Ulid
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.math.BigDecimal

internal val jacksonObjectMapper =
    jacksonMapperBuilder()
        .addModule(
            SimpleModule().also { module ->
                module.addSerializer(Ulid::class.java, UlidSerializer())
                module.addDeserializer(Ulid::class.java, UlidDeserializer())
            },
        )
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultPropertyInclusion {
            it.withValueInclusion(JsonInclude.Include.NON_NULL)
                .withContentInclusion(JsonInclude.Include.NON_NULL)
        }
        .addMixIn(Problem::class.java, ProblemJacksonMixIn::class.java)
        .build()

internal class EksternIdDeserializer : ValueDeserializer<Long>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): Long {
        return BigDecimal(p.getString()).toLong()
    }
}

@JsonIgnoreProperties(value = ["toJson"])
private class ProblemJacksonMixIn

private class UlidSerializer : ValueSerializer<Ulid>() {
    override fun serialize(
        value: Ulid,
        gen: JsonGenerator,
        serializers: SerializationContext,
    ) {
        gen.writeString(value.id)
    }
}

private class UlidDeserializer : ValueDeserializer<Ulid>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): Ulid = Ulid(p.getString())
}
