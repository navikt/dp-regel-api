package no.nav.dagpenger.regel.api.db

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate

class JsonAdapterTest {
    @Test
    fun `Mappe fra json string til domene object`() {
        JsonAdapter.fromJson(json) shouldNotBe null
        with(JsonAdapter.fromJson(json2)) {
            minsteinntektResultat shouldBe null
            faktum.inntektManueltRedigert shouldBe null
            faktum.regelverksdato shouldBe LocalDate.of(2020, 1, 1)
            faktum.forrigeGrunnlag shouldBe 12364
        }
    }

    @Test
    fun `Mapper gamle subsumsjoner med vedtakid`() {
        val vedtakId = "123"
        with(JsonAdapter.fromJson(json3(vedtakId))) {
            faktum.regelkontekst shouldBe RegelKontekst(vedtakId, Kontekst.vedtak)
        }
    }

    val json =
        """{
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

    @Language("JSON")
    val json2 =
        """{
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
    "inntektManueltRedigert": null,
    "regelverksdato": "2020-01-01",
    "forrigeGrunnlag": 12364
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
  }
}"""

    @Language("JSON")
    fun json3(vedtakId: String) =
        """{
  "faktum": {
    "aktorId": "1000023072522",
    "lærling": false,
    "vedtakId": $vedtakId,
    "antallBarn": 0,
    "inntektsId": "01EZYFZP0RE5PT1PRKJ2MDJKZ1",
    "inntektAvvik": false,
    "beregningsdato": "2021-03-03",
    "inntektManueltRedigert": null
  },
  "behovId": "01EZYG0JSCG597XJEK4YXJQACM"
}"""
}
