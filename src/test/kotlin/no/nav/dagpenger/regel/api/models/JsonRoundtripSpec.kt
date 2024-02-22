package no.nav.dagpenger.regel.api.models

import de.huxhorn.sulky.ulid.ULID
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.regel.api.db.JsonAdapter
import java.time.LocalDate

class JsonRoundtripSpec : StringSpec() {
    init {

        "Alle InternBehov skal kunne gjøre JSON roundtrips" {
            checkAll(internBehovGenerator) { behov: InternBehov ->
                val parsedBehov = InternBehov.fromJson(behov.toJson())
                behov shouldBe parsedBehov
            }
        }
        "Alle Subsumsjoner skal kunne gjøre JSON roundtrips" {
            checkAll(subsumsjonGenerator) { subsumsjon: Subsumsjon ->
                val parsedSubsumsjon = JsonAdapter.fromJson(subsumsjon.toJson())
                subsumsjon shouldBe parsedSubsumsjon
            }
        }
    }
}

private val subsumsjonGenerator =
    arbitrary {
        val stringArb = Arb.string(10, 10)
        Subsumsjon(
            behovId = BehovId(ULID().nextULID()),
            faktum =
                Faktum(
                    aktorId = stringArb.next(it),
                    regelkontekst = RegelKontekst(stringArb.next(it), Kontekst.vedtak),
                    beregningsdato =
                        Arb.localDate(LocalDate.of(2010, 1, 1), LocalDate.of(LocalDate.now().year, 1, 1))
                            .next(it),
                ),
            grunnlagResultat = Arb.map(stringArb, stringArb).next(it),
            periodeResultat = Arb.map(stringArb, stringArb).next(it),
            minsteinntektResultat = Arb.map(stringArb, stringArb).next(it),
            satsResultat = Arb.map(stringArb, stringArb).next(it),
            problem = Problem(title = stringArb.next(it)),
        )
    }

private val internBehovGenerator =
    arbitrary {
        val stringArb = Arb.string(10, 10)
        InternBehov(
            aktørId = stringArb.next(it),
            behandlingsId =
                BehandlingsId.nyBehandlingsIdFraEksternId(
                    RegelKontekst(
                        Arb.string().next(it),
                        Kontekst.vedtak,
                    ),
                ),
            harAvtjentVerneplikt = Arb.boolean().next(it),
            oppfyllerKravTilFangstOgFisk = Arb.boolean().next(it),
            manueltGrunnlag = Arb.int(0, 1000).next(it),
            antallBarn = Arb.int(0, 10).next(it),
            beregningsDato = LocalDate.now(),
        )
    }
