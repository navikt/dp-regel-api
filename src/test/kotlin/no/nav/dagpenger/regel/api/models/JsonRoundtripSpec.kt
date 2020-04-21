package no.nav.dagpenger.regel.api.models

import de.huxhorn.sulky.ulid.ULID
import io.kotlintest.properties.Gen
import io.kotlintest.properties.assertAll
import io.kotlintest.specs.StringSpec
import no.nav.dagpenger.events.Problem
import java.time.LocalDate

class JsonRoundtripSpec : StringSpec() {
    init {
        "Alle InternBehov skal kunne gjøre JSON roundtrips" {
            assertAll(gena = InternBehovGenerator()) { behov: InternBehov ->
                val parsedBehov = InternBehov.fromJson(behov.toJson())
                behov == parsedBehov
            }
        }

        "Alle Subsumsjoner skal kunne gjøre JSON roundtrips" {
            assertAll(gena = SubsumsjonGenerator()) { subsumsjon: Subsumsjon ->
                val parsedSubsumsjon = Subsumsjon.fromJson(subsumsjon.toJson())
                subsumsjon == parsedSubsumsjon
            }
        }
    }
}
class SubsumsjonGenerator : Gen<Subsumsjon> {
    override fun constants() = emptyList<Subsumsjon>()
    override fun random(): Sequence<Subsumsjon> = generateSequence {
        Subsumsjon(
            behovId = BehovId(ULID().nextULID()),
            faktum = Faktum(
                aktorId = Gen.string().random().first(),
                vedtakId = Gen.positiveIntegers().random().first(),
                beregningsdato = Gen.localDate(minYear = 2010, maxYear = LocalDate.now().year).random().first()
            ),
            grunnlagResultat = Gen.map(genK = Gen.string(), genV = Gen.string()).random().first(),
            periodeResultat = Gen.map(genK = Gen.string(), genV = Gen.string()).random().first(),
            minsteinntektResultat = Gen.map(genK = Gen.string(), genV = Gen.string()).random().first(),
            satsResultat = Gen.map(genK = Gen.string(), genV = Gen.string()).random().first(),
            problem = Problem(title = Gen.string().random().first())
        )
    }
}

class InternBehovGenerator : Gen<InternBehov> {
    override fun constants() = emptyList<InternBehov>()
    override fun random(): Sequence<InternBehov> = generateSequence {
        InternBehov(
            aktørId = Gen.string().random().first(),
            behandlingsId = BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst(Gen.string().random().first(), Kontekst.VEDTAK)),
            beregningsDato = LocalDate.now(),
            harAvtjentVerneplikt = Gen.bool().random().first(),
            oppfyllerKravTilFangstOgFisk = Gen.bool().random().first(),
            antallBarn = Gen.positiveIntegers().random().first(),
            manueltGrunnlag = Gen.positiveIntegers().random().first()
        )
    }
}