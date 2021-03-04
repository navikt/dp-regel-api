package no.nav.dagpenger.regel.api.db

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.Faktum
import no.nav.dagpenger.regel.api.models.InntektsPeriode
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.streams.ProblemStrategyTest
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter


class DeserializerTest {
    private fun JsonNode.asLocalDate(): LocalDate = LocalDate.parse(this.asText(), DateTimeFormatter.ISO_LOCAL_DATE)
    private fun JsonNode.nullableBoolean(field: String): Boolean? {
        return if (this.hasNonNull(field)) {
            this[field].asBoolean()
        } else {
            null
        }
    }

    private fun JsonNode.nullableInt(field: String): Int? {
        return if (this.hasNonNull(field)) {
            this[field].asInt()
        } else {
            null
        }
    }

    private fun JsonNode.asYearMont(): YearMonth = YearMonth.parse(this.asText())
    private fun JsonNode.nullableMap(field: String): Map<String, Any>? {
        return if (this.hasNonNull(field)) {
            jacksonObjectMapper().convertValue(this[field], object : TypeReference<Map<String, Any>>() {})
        } else {
            null
        }

    }


    @Test
    fun `Mappe fra json string til domene object`() {
        jacksonObjectMapper().readTree(json).let {
            Subsumsjon(
                behovId = BehovId(it["behovId"].asText()),
                faktum = getFaktum(it),
                grunnlagResultat = it.nullableMap("grunnlagResultat"),
                minsteinntektResultat = it.nullableMap("minsteinntektResultat"),
                satsResultat = it.nullableMap("satsResultat"),
                periodeResultat = it.nullableMap("periodeResultat"),
                problem =  jacksonObjectMapper().convertValue(it["problem"], Problem::class.java)
            )
        } shouldNotBe null
    }

    private fun getBruktInntektsPeriode(jsonNode: JsonNode): InntektsPeriode? {
        return if (jsonNode.hasNonNull("bruktInntektsPeriode")) {
            InntektsPeriode(
                førsteMåned = jsonNode["bruktInntektsPeriode"]["førsteMåned"].asYearMont(),
                sisteMåned = jsonNode["bruktInntektsPeriode"]["sisteMåned"].asYearMont(),
            )
        } else {
            null
        }
    }

    private fun getFaktum(json: JsonNode): Faktum {
        val faktum = json["faktum"]
        val regelkontekst = if (faktum.has("vedtakId")) {
            RegelKontekst(faktum["vedtakId"].asText(), Kontekst.VEDTAK)
        } else {
            json["regelkontekst"].let {
                RegelKontekst(it["id"].asText(), Kontekst.valueOf(it["type"].asText()))
            }
        }

        return Faktum(
            aktorId = faktum["aktorId"].asText(),
            regelkontekst = regelkontekst,
            vedtakId = regelkontekst.id.toInt(),
            beregningsdato = faktum["beregningsdato"].asLocalDate(),
            inntektsId = faktum["inntektsId"].textValue(),
            inntektAvvik = faktum.nullableBoolean("inntektAvvik"),
            inntektManueltRedigert = faktum.nullableBoolean("inntektManueltRedigert"),
            harAvtjentVerneplikt = faktum.nullableBoolean("harAvtjentVerneplikt"),
            oppfyllerKravTilFangstOgFisk = faktum.nullableBoolean("oppfyllerKravTilFangstOgFisk"),
            antallBarn = faktum.nullableInt("antallBarn"),
            manueltGrunnlag = faktum.nullableInt("manueltGrunnlag"),
            lærling = faktum.nullableBoolean("lærling"),
            bruktInntektsPeriode = getBruktInntektsPeriode(faktum)
        )


    }

    val json = """{
  "faktum": {
    "aktorId": "1000023072522",
    "lærling": false,
    "vedtakId": 36766521,
    "antallBarn": 0,
    "inntektsId": "01EZYFZP0RE5PT1PRKJ2MDJKZ1",
    "inntektAvvik": false,
    "regelkontekst": {
      "id": "36766521",
      "type": "vedtak"
    },
    "beregningsdato": "2021-03-03",
    "harAvtjentVerneplikt": false,
    "inntektManueltRedigert": false,
    "oppfyllerKravTilFangstOgFisk": true
  },
  "behovId": "01EZYG0JSCG597XJEK4YXJQACM",
  "satsResultat": {
    "dagsats": 1665.0,
    "ukesats": 8325.0,
    "sporingsId": "01EZYG0JYEH60AJWS96DFTQ7AC",
    "subsumsjonsId": "01EZYG0JYEADMQT59HM2V4NCZE",
    "beregningsregel": "KORONA",
    "regelIdentifikator": "Sats.v1",
    "benyttet90ProsentRegel": false
  },
  "periodeResultat": {
    "sporingsId": "01EZYG0JYZ0PJJ1JVJYA6VKFZ5",
    "subsumsjonsId": "01EZYG0JYZFHN8QM6SFG14N5KV",
    "periodeAntallUker": 104.0,
    "regelIdentifikator": "Periode.v1"
  },
  "grunnlagResultat": {
    "avkortet": "608106",
    "uavkortet": "625820",
    "sporingsId": "01EZYG0JY3R0VYGRN5WPBMXXVP",
    "harAvkortet": true,
    "subsumsjonsId": "01EZYG0JY36VJR8T7A9KSXVVM1",
    "beregningsregel": "ArbeidsinntektSiste12",
    "grunnbeløpBrukt": "101351",
    "regelIdentifikator": "Grunnlag.v1",
    "grunnlagInntektsPerioder": [
      {
        "inntekt": "643294.2300",
        "periode": 1.0,
        "inntektsPeriode": {
          "sisteMåned": "2021-01",
          "førsteMåned": "2020-02"
        },
        "inneholderFangstOgFisk": true
      },
      {
        "inntekt": "576071.5300",
        "periode": 2.0,
        "inntektsPeriode": {
          "sisteMåned": "2020-01",
          "førsteMåned": "2019-02"
        },
        "inneholderFangstOgFisk": false
      },
      {
        "inntekt": "932655.5700",
        "periode": 3.0,
        "inntektsPeriode": {
          "sisteMåned": "2019-01",
          "førsteMåned": "2018-02"
        },
        "inneholderFangstOgFisk": false
      }
    ]
  },
  "minsteinntektResultat": {
    "sporingsId": "01EZYG0JXQK8GXQ8TSASVGPX86",
    "subsumsjonsId": "01EZYG0JXQZ9T9D0WD57G361TR",
    "beregningsregel": "KORONA",
    "regelIdentifikator": "Minsteinntekt.v1",
    "oppfyllerMinsteinntekt": true,
    "minsteinntektInntektsPerioder": [
      {
        "andel": "643294.2300",
        "inntekt": "643294.2300",
        "periode": 1.0,
        "inntektsPeriode": {
          "sisteMåned": "2021-01",
          "førsteMåned": "2020-02"
        },
        "inneholderFangstOgFisk": true
      },
      {
        "andel": "549566.5300",
        "inntekt": "549566.5300",
        "periode": 2.0,
        "inntektsPeriode": {
          "sisteMåned": "2020-01",
          "førsteMåned": "2019-02"
        },
        "inneholderFangstOgFisk": false
      },
      {
        "andel": "895800.5700",
        "inntekt": "895800.5700",
        "periode": 3.0,
        "inntektsPeriode": {
          "sisteMåned": "2019-01",
          "førsteMåned": "2018-02"
        },
        "inneholderFangstOgFisk": false
      }
    ]
  }
}"""
}


