package no.nav.dagpenger.regel.api.routing

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotEndWith
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyAll
import no.nav.dagpenger.regel.api.db.BehovNotFoundException
import no.nav.dagpenger.regel.api.db.InternSubsumsjonBrukt
import no.nav.dagpenger.regel.api.db.SubsumsjonStore
import no.nav.dagpenger.regel.api.models.BehandlingsId
import no.nav.dagpenger.regel.api.models.BehovId
import no.nav.dagpenger.regel.api.models.InntektsPeriode
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.models.Kontekst
import no.nav.dagpenger.regel.api.models.RegelKontekst
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonId
import no.nav.dagpenger.regel.api.routing.TestApplication.autentisert
import no.nav.dagpenger.regel.api.routing.TestApplication.testApp
import no.nav.dagpenger.regel.api.streams.DagpengerBehovProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.concurrent.Future

class BehovRouteV1Test {
    @Test
    fun `401 on unauthorized requests`() {
        testApp(
            mockApi(),
        ) {
            client.get("v1/behov/status/id").status shouldBe HttpStatusCode.Unauthorized
            client.post("v1/behov").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Status when behov is done, pending or not found`() {
        val storeMock = mockk<SubsumsjonStore>(relaxed = false)
        every { storeMock.behovStatus(BehovId("01DSFG6P7969DP56BPW2EDS1RN")) } returns Status.Pending
        every { storeMock.behovStatus(BehovId("01DSFG798QNFAWXNFGZF0J2APX")) } returns Status.Done(BehovId("01DSFGCKM9TEZ94X872C7H4QB4"))
        every { storeMock.behovStatus(BehovId("01DSFG7JVZVVD2ZK7K7HG9SNVG")) } throws BehovNotFoundException("not found")

        testApp(
            mockApi(
                subsumsjonStore = storeMock,
            ),
        ) {
            with(autentisert("v1/behov/status/01DSFG6P7969DP56BPW2EDS1RN", HttpMethod.Get)) {
                status shouldBe HttpStatusCode.OK
                headers["Content-Type"] shouldBe
                    ContentType.Application.Json.withParameter("charset", "UTF-8")
                        .toString()
                bodyAsText() shouldBe """{"status":"PENDING"}"""
            }
//            with(autentisert("v1/behov/status/01DSFG798QNFAWXNFGZF0J2APX", HttpMethod.Get)) {
//                status shouldBe HttpStatusCode.OK
//                headers["Content-Type"] shouldBe
//                    ContentType.Application.Json.withParameter("charset", "UTF-8")
//                        .toString()
//                bodyAsText() shouldBe """{"status":"PENDING"}"""
//            }

            with(autentisert("v1/behov/status/01DSFG7JVZVVD2ZK7K7HG9SNVG", HttpMethod.Get)) {
                status shouldBe HttpStatusCode.NotFound
            }
        }

        verifyAll {
            storeMock.behovStatus(BehovId("01DSFG6P7969DP56BPW2EDS1RN"))
//            storeMock.behovStatus(BehovId("01DSFG798QNFAWXNFGZF0J2APX"))
            storeMock.behovStatus(BehovId("01DSFG7JVZVVD2ZK7K7HG9SNVG"))
        }
    }

    @Test
    fun `Valid json to behov endpoint should be accepted, saved and produce an event to Kafka`() {
        val subsumsjonStoreMock: SubsumsjonStore = mockedSubsumsjonStore()

        val produceSlot = slot<InternBehov>()
        val kafkaMock =
            mockk<DagpengerBehovProducer>(relaxed = true).apply {
                every { this@apply.produceEvent(behov = capture(produceSlot)) } returns mockk<Future<RecordMetadata>>()
            }

        testApp(
            mockApi(
                subsumsjonStoreMock,
                kafkaMock,
            ),
        ) {
            // language=JSON
            val body =
                """
                {
                    "aktorId": "1234",
                    "regelkontekst": {"id": "1", "type": "vedtak"},
                    "beregningsdato": "2019-01-08",
                    "manueltGrunnlag": 54200,
                    "harAvtjentVerneplikt": true,
                    "oppfyllerKravTilFangstOgFisk": true,
                    "bruktInntektsPeriode":{"førsteMåned":"2011-07","sisteMåned":"2011-07"},
                    "antallBarn": 1,
                    "lærling": false
                }
                """.trimIndent()
            val response = autentisert("v1/behov", HttpMethod.Post, body)
            response.status shouldBe HttpStatusCode.Accepted
            response.headers.contains(HttpHeaders.Location) shouldBe true
            response.headers[HttpHeaders.Location]?.let { location ->
                location shouldStartWith "/v1/behov/status/"
                withClue("Behov id should be present") { location shouldNotEndWith "v1/behov/status/" }
            }
        }

        with(produceSlot.captured) {
            behovId shouldNotBe null
            aktørId shouldBe "1234"
            behandlingsId shouldNotBe null
            behandlingsId.regelKontekst.type shouldBe Kontekst.vedtak
            behandlingsId.regelKontekst.id shouldBe "1"
            beregningsDato shouldBe LocalDate.of(2019, 1, 8)
            harAvtjentVerneplikt shouldBe true
            oppfyllerKravTilFangstOgFisk shouldBe true
            bruktInntektsPeriode shouldBe InntektsPeriode(YearMonth.of(2011, 7), YearMonth.of(2011, 7))
            manueltGrunnlag shouldBe 54200
            antallBarn shouldBe 1
            lærling shouldNotBe null
        }

        verifyAll {
            kafkaMock.produceEvent(any())
        }
    }

    @Test
    fun `Handle behov where regelkontekst id is not present`() {
        val subsumsjonStoreMock: SubsumsjonStore = mockedSubsumsjonStore()

        val produceSlot = slot<InternBehov>()
        val kafkaMock =
            mockk<DagpengerBehovProducer>(relaxed = true).apply {
                every { this@apply.produceEvent(behov = capture(produceSlot)) } returns mockk<Future<RecordMetadata>>()
            }

        testApp(
            mockApi(
                subsumsjonStoreMock,
                kafkaMock,
            ),
        ) {
            // language=JSON
            val body =
                """
                {
                    "regelkontekst" : { "type" : "vedtak"},
                    "aktorId": "1234",
                    "beregningsdato": "2019-01-08",
                    "manueltGrunnlag": 54200,
                    "harAvtjentVerneplikt": true,
                    "oppfyllerKravTilFangstOgFisk": true,
                    "bruktInntektsPeriode":{"førsteMåned":"2011-07","sisteMåned":"2011-07"},
                    "antallBarn": 1
                }
                """.trimIndent()
            val response = autentisert("v1/behov", HttpMethod.Post, body)
            response.status shouldBe HttpStatusCode.Accepted
            response.headers.contains(HttpHeaders.Location) shouldBe true
            response.headers[HttpHeaders.Location]?.let { location ->
                location shouldStartWith "/v1/behov/status/"
                withClue("Behov id should be present") { location shouldNotEndWith "v1/behov/status/" }
            }
        }

        with(produceSlot.captured) {
            behovId shouldNotBe null
            aktørId shouldBe "1234"
            behandlingsId shouldNotBe null
            behandlingsId.regelKontekst.type shouldBe Kontekst.vedtak
            behandlingsId.regelKontekst.id shouldBe "0"
            beregningsDato shouldBe LocalDate.of(2019, 1, 8)
            harAvtjentVerneplikt shouldBe true
            oppfyllerKravTilFangstOgFisk shouldBe true
            bruktInntektsPeriode shouldBe InntektsPeriode(YearMonth.of(2011, 7), YearMonth.of(2011, 7))
            manueltGrunnlag shouldBe 54200
            antallBarn shouldBe 1
        }
    }

    @Test
    fun `Valid json with regelkontekst to behov endpoint should be accepted, saved and produce an event to Kafka`() {
        val subsumsjonStoreMock: SubsumsjonStore = mockedSubsumsjonStore()

        val produceSlot = slot<InternBehov>()
        val kafkaMock =
            mockk<DagpengerBehovProducer>(relaxed = true).apply {
                every { this@apply.produceEvent(behov = capture(produceSlot)) } returns mockk<Future<RecordMetadata>>()
            }

        testApp(
            mockApi(
                subsumsjonStoreMock,
                kafkaMock,
            ),
        ) {
            // language=JSON
            val body =
                """
                {
                    "regelkontekst" : { "type" : "vedtak", "id" : "45678" },
                    "aktorId": "1234",
                    "beregningsdato": "2019-01-08",
                    "manueltGrunnlag": 54200,
                    "harAvtjentVerneplikt": true,
                    "oppfyllerKravTilFangstOgFisk": true,
                    "bruktInntektsPeriode":{"førsteMåned":"2011-07","sisteMåned":"2011-07"},
                    "antallBarn": 1
                }
                """.trimIndent()
            val response = autentisert("v1/behov", HttpMethod.Post, body)
            response.status shouldBe HttpStatusCode.Accepted

            response.headers.contains(HttpHeaders.Location) shouldBe true
            response.headers[HttpHeaders.Location]?.let { location ->
                location shouldStartWith "/v1/behov/status/"
                withClue("Behov id should be present") { location shouldNotEndWith "v1/behov/status/" }
            }
        }

        with(produceSlot.captured) {
            behovId shouldNotBe null
            aktørId shouldBe "1234"
            behandlingsId shouldNotBe null
            behandlingsId.regelKontekst.type shouldBe Kontekst.vedtak
            behandlingsId.regelKontekst.id shouldBe "45678"
            beregningsDato shouldBe LocalDate.of(2019, 1, 8)
            harAvtjentVerneplikt shouldBe true
            oppfyllerKravTilFangstOgFisk shouldBe true
            bruktInntektsPeriode shouldBe InntektsPeriode(YearMonth.of(2011, 7), YearMonth.of(2011, 7))
            manueltGrunnlag shouldBe 54200
            antallBarn shouldBe 1
        }

        verifyAll {
            kafkaMock.produceEvent(any())
        }
    }

    @Test
    fun `Regelverksdato`() {
        val subsumsjonStoreMock: SubsumsjonStore = mockedSubsumsjonStore()

        val produceSlot = slot<InternBehov>()
        val kafkaMock =
            mockk<DagpengerBehovProducer>(relaxed = true).apply {
                every { this@apply.produceEvent(behov = capture(produceSlot)) } returns mockk<Future<RecordMetadata>>()
            }

        testApp(
            mockApi(
                subsumsjonStoreMock,
                kafkaMock,
            ),
        ) {
            // language=JSON
            val body =
                """
                {
                    "regelkontekst" : { "type" : "vedtak", "id" : "45678" },
                    "aktorId": "1234",
                    "vedtakId": 1,
                    "beregningsdato": "2019-01-08",
                    "manueltGrunnlag": 54200,
                    "harAvtjentVerneplikt": true,
                    "oppfyllerKravTilFangstOgFisk": true,
                    "bruktInntektsPeriode":{"førsteMåned":"2011-07","sisteMåned":"2011-07"},
                    "antallBarn": 1,
                    "regelverksdato": "2020-02-09"
                }
                """.trimIndent()
            val response = autentisert("v1/behov", HttpMethod.Post, body)
            response.status shouldBe HttpStatusCode.Accepted

            response.headers.contains(HttpHeaders.Location) shouldBe true
            response.headers[HttpHeaders.Location]?.let { location ->
                location shouldStartWith "/v1/behov/status/"
                withClue("Behov id should be present") { location shouldNotEndWith "v1/behov/status/" }
            }
        }

        with(produceSlot.captured) {
            behovId shouldNotBe null
            aktørId shouldBe "1234"
            behandlingsId shouldNotBe null
            behandlingsId.regelKontekst.type shouldBe Kontekst.vedtak
            behandlingsId.regelKontekst.id shouldBe "45678"
            beregningsDato shouldBe LocalDate.of(2019, 1, 8)
            harAvtjentVerneplikt shouldBe true
            oppfyllerKravTilFangstOgFisk shouldBe true
            bruktInntektsPeriode shouldBe InntektsPeriode(YearMonth.of(2011, 7), YearMonth.of(2011, 7))
            manueltGrunnlag shouldBe 54200
            antallBarn shouldBe 1
            regelverksdato shouldBe LocalDate.of(2020, 2, 9)
        }

        verifyAll {
            kafkaMock.produceEvent(any())
        }
    }

    @Test
    fun `Forrige grunnlag`() {
        val subsumsjonStoreMock: SubsumsjonStore = mockedSubsumsjonStore()

        val produceSlot = slot<InternBehov>()
        val kafkaMock =
            mockk<DagpengerBehovProducer>(relaxed = true).apply {
                every { this@apply.produceEvent(behov = capture(produceSlot)) } returns mockk<Future<RecordMetadata>>()
            }

        testApp(
            mockApi(
                subsumsjonStoreMock,
                kafkaMock,
            ),
        ) {
            // language=JSON
            val body =
                """
                {
                    "regelkontekst" : { "type" : "vedtak", "id" : "45678" },
                    "aktorId": "1234",
                    "vedtakId": 1,
                    "beregningsdato": "2019-01-08",
                    "forrigeGrunnlag": 32200,
                    "harAvtjentVerneplikt": true,
                    "oppfyllerKravTilFangstOgFisk": true,
                    "bruktInntektsPeriode":{"førsteMåned":"2011-07","sisteMåned":"2011-07"},
                    "antallBarn": 1,
                    "regelverksdato": "2020-02-09"
                }
                """.trimIndent()
            val response = autentisert("v1/behov", HttpMethod.Post, body)
            response.status shouldBe HttpStatusCode.Accepted

            response.headers.contains(HttpHeaders.Location) shouldBe true
            response.headers[HttpHeaders.Location]?.let { location ->
                location shouldStartWith "/v1/behov/status/"
                withClue("Behov id should be present") { location shouldNotEndWith "v1/behov/status/" }
            }
        }

        with(produceSlot.captured) {
            behovId shouldNotBe null
            aktørId shouldBe "1234"
            behandlingsId shouldNotBe null
            behandlingsId.regelKontekst.type shouldBe Kontekst.vedtak
            behandlingsId.regelKontekst.id shouldBe "45678"
            beregningsDato shouldBe LocalDate.of(2019, 1, 8)
            harAvtjentVerneplikt shouldBe true
            oppfyllerKravTilFangstOgFisk shouldBe true
            bruktInntektsPeriode shouldBe InntektsPeriode(YearMonth.of(2011, 7), YearMonth.of(2011, 7))
            forrigeGrunnlag shouldBe 32200
            antallBarn shouldBe 1
            regelverksdato shouldBe LocalDate.of(2020, 2, 9)
        }

        verifyAll {
            kafkaMock.produceEvent(any())
        }
    }

    private fun mockedSubsumsjonStore(): SubsumsjonStore {
        return object : SubsumsjonStore {
            override fun insertSubsumsjon(
                subsumsjon: Subsumsjon,
                created: ZonedDateTime,
            ): Int {
                TODO("not implemented")
            }

            override fun delete(subsumsjon: Subsumsjon) {
                TODO("not implemented")
            }

            override fun markerSomBrukt(internSubsumsjonBrukt: InternSubsumsjonBrukt) {
                TODO("not implemented")
            }

            override fun getBehov(behovId: BehovId): InternBehov {
                TODO("not implemented")
            }

            override fun insertBehov(behov: InternBehov): Int {
                return 1
            }

            override fun hentKoblingTilRegelKontekst(regelKontekst: RegelKontekst): BehandlingsId {
                return BehandlingsId.nyBehandlingsIdFraEksternId(regelKontekst)
            }

            override fun opprettKoblingTilRegelkontekst(regelKontekst: RegelKontekst): BehandlingsId {
                TODO("not implemented")
            }

            override fun behovStatus(behovId: BehovId): Status {
                TODO("not implemented")
            }

            override fun getSubsumsjon(behovId: BehovId): Subsumsjon {
                TODO("not implemented")
            }

            override fun getSubsumsjonByResult(subsumsjonId: SubsumsjonId): Subsumsjon {
                TODO("not implemented")
            }

            override fun getSubsumsjonerByResults(subsumsjonIder: List<SubsumsjonId>): List<Subsumsjon> {
                TODO("Not yet implemented")
            }
        }
    }
}

internal class BehovRequestMappingTest {
    @Test
    fun `Default values for fields not present in request`() {
        val behov =
            mapRequestToBehov(
                BehovRequest(
                    aktorId = "aktorId",
                    regelkontekst = BehovRequest.RegelKontekst("1", Kontekst.vedtak),
                    beregningsdato = LocalDate.of(2019, 11, 7),
                    harAvtjentVerneplikt = null,
                    oppfyllerKravTilFangstOgFisk = null,
                    bruktInntektsPeriode = null,
                    manueltGrunnlag = null,
                    lærling = null,
                    antallBarn = null,
                ),
            )
        behov.regelverksdato shouldBe null
        behov.inntektsId shouldBe null
        behov.antallBarn shouldBe 0
        behov.forrigeGrunnlag shouldBe null
    }
}
