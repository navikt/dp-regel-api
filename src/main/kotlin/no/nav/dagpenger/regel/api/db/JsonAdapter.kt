package no.nav.dagpenger.regel.api.db

import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.InntektsPeriode
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonSerDerException
import no.nav.dagpenger.regel.api.serder.jacksonObjectMapper
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

internal object JsonAdapter {
    fun fromJson(jsonString: String): Subsumsjon {
        try {
            val node: tools.jackson.databind.JsonNode = jacksonObjectMapper.readTree(jsonString)
            return Subsumsjon(
                behovId = BehovId(node["behovId"].asString()),
                faktum = getFaktum(node),
                grunnlagResultat = node.getOrNull("grunnlagResultat")?.asMap(),
                minsteinntektResultat = node.getOrNull("minsteinntektResultat")?.asMap(),
                satsResultat = node.getOrNull("satsResultat")?.asMap(),
                periodeResultat = node.getOrNull("periodeResultat")?.asMap(),
                problem = node.getOrNull("problem")?.asProblem(),
            )
        } catch (e: Exception) {
            throw SubsumsjonSerDerException("Unable to deserialize: $jsonString", e)
        }
    }

    private fun JsonNode.asLocalDate(): LocalDate = LocalDate.parse(this.asString(), DateTimeFormatter.ISO_LOCAL_DATE)

    private fun JsonNode.asYearMonth(): YearMonth = YearMonth.parse(this.asString())

    private fun JsonNode.asMap(): Map<String, Any> = jacksonObjectMapper.convertValue(this, object : TypeReference<Map<String, Any>>() {})

    private fun JsonNode.asProblem(): Problem = jacksonObjectMapper.convertValue(this, Problem::class.java)

    private fun JsonNode.asInntektsPeriode() =
        InntektsPeriode(
            førsteMåned = this["førsteMåned"].asYearMonth(),
            sisteMåned = this["sisteMåned"].asYearMonth(),
        )

    private fun getFaktum(json: JsonNode): Faktum {
        val faktum = json["faktum"]
        val regelkontekst =
            if (faktum.has("vedtakId")) {
                RegelKontekst(faktum["vedtakId"].asString(), Kontekst.vedtak)
            } else {
                faktum["regelkontekst"].let {
                    RegelKontekst(it["id"].asString(), Kontekst.valueOf(it["type"].asString()))
                }
            }

        return Faktum(
            aktorId = faktum["aktorId"].asString(),
            regelkontekst = regelkontekst,
            beregningsdato = faktum["beregningsdato"].asLocalDate(),
            inntektsId = faktum.getOrNull("inntektsId")?.textValue(),
            inntektAvvik = faktum.getOrNull("inntektAvvik")?.asBoolean(),
            inntektManueltRedigert = faktum.getOrNull("inntektManueltRedigert")?.asBoolean(),
            harAvtjentVerneplikt = faktum.getOrNull("harAvtjentVerneplikt")?.asBoolean(),
            oppfyllerKravTilFangstOgFisk = faktum.getOrNull("oppfyllerKravTilFangstOgFisk")?.asBoolean(),
            antallBarn = faktum.getOrNull("antallBarn")?.asInt(),
            manueltGrunnlag = faktum.getOrNull("manueltGrunnlag")?.asInt(),
            forrigeGrunnlag = faktum.getOrNull("forrigeGrunnlag")?.asInt(),
            lærling = faktum.getOrNull("lærling")?.asBoolean(),
            bruktInntektsPeriode = faktum.getOrNull("bruktInntektsPeriode")?.asInntektsPeriode(),
            regelverksdato = faktum.getOrNull("regelverksdato")?.asLocalDate(),
        )
    }

    fun JsonNode.getOrNull(nullableField: String): JsonNode? {
        val nullableNode = this.get(nullableField)
        return if (nullableNode == null || nullableNode.isNull) {
            null
        } else {
            nullableNode
        }
    }
}
