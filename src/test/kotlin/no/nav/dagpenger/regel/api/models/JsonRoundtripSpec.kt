package no.nav.dagpenger.regel.api.models

import de.huxhorn.sulky.ulid.ULID
import io.kotest.core.spec.style.StringSpec
import io.kotest.properties.assertAll
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.arb
import io.kotest.property.arbitrary.bool
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.single
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll
import java.time.LocalDate
import no.nav.dagpenger.events.Problem

class JsonRoundtripSpec : StringSpec() {
    init {
        "Alle InternBehov skal kunne gjøre JSON roundtrips" {
            forAll(gena = internBehovGenerator) { behov: InternBehov ->
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

private val internBehovGenerator = arb {
    generateSequence {
        InternBehov(
            aktørId = Arb.string().single(it),
            behandlingsId = BehandlingsId.nyBehandlingsIdFraEksternId(RegelKontekst(Arb.string().single(it), Kontekst.VEDTAK)),
            harAvtjentVerneplikt = Arb.bool().single(it),
            oppfyllerKravTilFangstOgFisk = Arb.bool().single(it),
            manueltGrunnlag = Arb.int(0, 1000).single(it),
            antallBarn = Arb.int(0, 10).single(it),
            beregningsDato = LocalDate.now()
        )
    }
}
