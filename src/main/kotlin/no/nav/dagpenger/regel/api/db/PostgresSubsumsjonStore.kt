package no.nav.dagpenger.regel.api.db

import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.dagpenger.regel.api.models.BehandlingsId
import no.nav.dagpenger.regel.api.models.EksternId
import no.nav.dagpenger.regel.api.models.InternBehov
import no.nav.dagpenger.regel.api.models.Status
import no.nav.dagpenger.regel.api.models.Subsumsjon
import no.nav.dagpenger.regel.api.models.SubsumsjonSerDerException
import no.nav.dagpenger.regel.api.monitoring.HealthCheck
import no.nav.dagpenger.regel.api.monitoring.HealthStatus
import org.postgresql.util.PGobject
import org.postgresql.util.PSQLException

private val LOGGER = KotlinLogging.logger {}

internal class PostgresSubsumsjonStore(private val dataSource: HikariDataSource) : SubsumsjonStore, HealthCheck {

    override fun hentKoblingTilEkstern(eksternId: EksternId): BehandlingsId {
        val id: String? = using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT id FROM v1_behov_behandling_mapping WHERE kontekst = :kontekst AND ekstern_id = :ekstern_id",
                    mapOf("kontekst" to eksternId.kontekst.name, "ekstern_id" to eksternId.id)
                ).map { row ->
                    row.string("id")
                }.asSingle
            )
        }
        return id?.let { BehandlingsId(it, eksternId) } ?: opprettKoblingTilEkstern(eksternId)
    }

    private fun opprettKoblingTilEkstern(eksternId: EksternId): BehandlingsId {
        val behandlingsId = BehandlingsId.nyBehandlingsIdFraEksternId(eksternId)
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO v1_behov_behandling_mapping(id, ekstern_id, kontekst) VALUES (:id, :ekstern_id, :kontekst)",
                    mapOf(
                        "id" to behandlingsId.id,
                        "ekstern_id" to behandlingsId.eksternId.id,
                        "kontekst" to behandlingsId.eksternId.kontekst.name
                    )
                ).asUpdate
            )
        }
        return behandlingsId
    }

    override fun insertBehov(behov: InternBehov): Int {
        return try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.run(
                        queryOf(
                            """INSERT INTO v2_behov(id, behandlings_id, aktor_id, beregnings_dato, oppfyller_krav_til_fangst_og_fisk, 
                    |                                       avtjent_verne_plikt, brukt_opptjening_forste_maned, brukt_opptjening_siste_maned, antall_barn, manuelt_grunnlag, inntekts_id, data) 
                    |                  VALUES (:id, :behandlings_id, :aktor, :beregning, :fisk, :verneplikt, :forste, :siste, :barn, :grunnlag, :inntekt, :data)""".trimMargin(),
                            mapOf(
                                "id" to behov.behovId,
                                "behandlings_id" to behov.behandlingsId.id,
                                "aktor" to behov.aktørId,
                                "beregning" to behov.beregningsDato,
                                "fisk" to behov.oppfyllerKravTilFangstOgFisk,
                                "verneplikt" to behov.harAvtjentVerneplikt,
                                "forste" to behov.bruktInntektsPeriode?.førsteMåned?.atDay(1),
                                "siste" to behov.bruktInntektsPeriode?.sisteMåned?.atDay(1),
                                "barn" to behov.antallBarn,
                                "grunnlag" to behov.manueltGrunnlag,
                                "inntekt" to behov.inntektsId,
                                "data" to PGobject().apply {
                                    this.type = "jsonb"
                                    this.value = behov.toJson()
                                }
                            )
                        ).asUpdate
                    )
                }
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    override fun behovStatus(behovId: String): Status {
        return when (behovExists(behovId)) {
            true -> getSubsumsjonIdBy(behovId)?.let { Status.Done(it) } ?: Status.Pending
            false -> throw BehovNotFoundException("BehovId: $behovId")
        }
    }

    override fun insertSubsumsjon(subsumsjon: Subsumsjon): Int {
        return try {
            using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        """ INSERT INTO v2_subsumsjon VALUES (?, (to_json(?::json))) ON CONFLICT ON CONSTRAINT v2_subsumsjon_pkey DO NOTHING """,
                        subsumsjon.behovId, subsumsjon.toJson()
                    ).asUpdate
                )
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    override fun getSubsumsjon(behovId: String): Subsumsjon {
        val json = using(sessionOf(dataSource)) { session ->
            session.run(queryOf(""" SELECT data FROM v2_subsumsjon WHERE behov_id = ? """, behovId)
                .map { row -> row.string("data") }
                .asSingle)
        } ?: throw SubsumsjonNotFoundException("Could not find subsumsjon with behov id $behovId")

        return Subsumsjon.fromJson(json) ?: throw SubsumsjonSerDerException("Unable to deserialize: $json")
    }

    override fun status(): HealthStatus {
        return try {
            using(sessionOf(dataSource)) { session -> session.run(queryOf(""" SELECT 1""").asExecute) }.let { HealthStatus.UP }
        } catch (p: PSQLException) {
            LOGGER.error("Failed health check", p)
            return HealthStatus.DOWN
        }
    }

    override fun getSubsumsjonByResult(subsumsjonId: SubsumsjonId): Subsumsjon {
        return try {
            getSubsumByResultV2(subsumsjonId)
        } catch (err: SubsumsjonNotFoundException) {
            getSubsumByResultV1(subsumsjonId)
        }
    }

    private fun getSubsumByResultV1(subsumsjonId: SubsumsjonId): Subsumsjon {
        return getSubsumByResult("v1", subsumsjonId)
    }

    private fun getSubsumByResultV2(subsumsjonId: SubsumsjonId): Subsumsjon {
        return getSubsumByResult("v2", subsumsjonId)
    }

    private fun getSubsumByResult(version: String, subsumsjonId: SubsumsjonId): Subsumsjon {
        val json = using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """ select
                                                  data
                                            from ${version}_subsumsjon
                                            where data -> 'satsResultat' ->> 'subsumsjonsId'::text = :id
                                               OR data -> 'minsteinntektResultat' ->> 'subsumsjonsId'::text = :id
                                               OR data -> 'periodeResultat' ->> 'subsumsjonsId'::text = :id
                                               OR data -> 'grunnlagResultat' ->> 'subsumsjonsId'::text = :id """,
                    mapOf("id" to subsumsjonId.id)
                )
                    .map { row -> row.string("data") }.asSingle
            )
        } ?: throw SubsumsjonNotFoundException("Could not find subsumsjon with subsumsjonId $subsumsjonId")

        return Subsumsjon.fromJson(json) ?: throw SubsumsjonSerDerException("Unable to deserialize: $json")
    }

    private fun behovExists(behovId: String): Boolean {
        try {
            return using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        """ SELECT EXISTS (SELECT 1 FROM v2_behov WHERE id = ? ) AS "exists" """,
                        behovId
                    ).map { row -> row.boolean("exists") }.asSingle
                )!!
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }

    private fun getSubsumsjonIdBy(behovId: String): String? {
        try {
            return using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        """ SELECT behov_id FROM v2_subsumsjon WHERE behov_id = ? """,
                        behovId
                    ).map { row -> row.stringOrNull("behov_id") }.asSingle
                )
            }
        } catch (p: PSQLException) {
            throw StoreException(p.message!!)
        }
    }
}
