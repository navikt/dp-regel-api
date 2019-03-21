package no.nav.dagpenger.regel.api

import mu.KotlinLogging
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.regel.api.models.common.InntektResponse
import no.nav.dagpenger.regel.api.models.common.InntektsPeriode
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagFaktum
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagResultat
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagSubsumsjon
import no.nav.dagpenger.regel.api.grunnlag.GrunnlagSubsumsjoner
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektFaktum
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektResultat
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektSubsumsjon
import no.nav.dagpenger.regel.api.minsteinntekt.MinsteinntektSubsumsjoner
import no.nav.dagpenger.regel.api.periode.PeriodeFaktum
import no.nav.dagpenger.regel.api.periode.PeriodeResultat
import no.nav.dagpenger.regel.api.periode.PeriodeSubsumsjon
import no.nav.dagpenger.regel.api.periode.PeriodeSubsumsjoner
import no.nav.dagpenger.regel.api.sats.SatsFaktum
import no.nav.dagpenger.regel.api.sats.SatsResultat
import no.nav.dagpenger.regel.api.sats.SatsSubsumsjon
import no.nav.dagpenger.regel.api.sats.SatsSubsumsjoner
import no.nav.dagpenger.regel.api.tasks.TaskStatus
import no.nav.dagpenger.regel.api.tasks.Tasks
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Topics
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Properties
import java.util.concurrent.TimeUnit

private val LOGGER = KotlinLogging.logger {}

class KafkaDagpengerBehovConsumer(
    val env: Environment,
    val tasks: Tasks,
    val minsteinntektSubsumsjoner: MinsteinntektSubsumsjoner,
    val periodeSubsumsjoner: PeriodeSubsumsjoner,
    val grunnlagSubsumsjoner: GrunnlagSubsumsjoner,
    val satsSubsumsjoner: SatsSubsumsjoner
) {

    val SERVICE_APP_ID = "dp-regel-api"
    private lateinit var streams: KafkaStreams
    fun start() {
        LOGGER.info { "Starting up $SERVICE_APP_ID kafca consumer" }
        streams = KafkaStreams(buildTopology(), this.getConfig())
        streams.setUncaughtExceptionHandler { t, e -> System.exit(0) }
        streams.start()
    }

    fun stop() {
        LOGGER.info { "Shutting down $SERVICE_APP_ID kafka consumer" }
        streams.close(3, TimeUnit.SECONDS)
        streams.cleanUp()
    }

    internal fun buildTopology(): Topology {
        val builder = StreamsBuilder()

        val stream = builder.stream(
            Topics.DAGPENGER_BEHOV_EVENT.name,
            Consumed.with(Serdes.StringSerde(), Serdes.serdeFrom(JsonSerializer(), JsonDeserializer()))
        )

        stream
            .peek { key, value -> LOGGER.info("Consuming behov with id $value") }
            .foreach { _, behov -> storeResult(behov) }

        return builder.build()
    }

    fun getConfig(): Properties {
        val props = streamConfig(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = env.bootstrapServersUrl,
            credential = KafkaCredential(env.username, env.password))
        return props
    }

    fun hasNeededMinsteinntektResultat(behov: SubsumsjonsBehov): Boolean {
        return behov.minsteinntektResultat != null && hasPendingTask(Regel.MINSTEINNTEKT, behov.behovId)
    }

    fun hasNeededPeriodeResultat(behov: SubsumsjonsBehov): Boolean {
        return behov.periodeResultat != null && hasPendingTask(Regel.PERIODE, behov.behovId)
    }

    fun hasNeededGrunnlagResultat(behov: SubsumsjonsBehov): Boolean {
        return behov.grunnlagResultat != null && hasPendingTask(Regel.GRUNNLAG, behov.behovId)
    }

    fun hasNeededSatsResultat(behov: SubsumsjonsBehov): Boolean {
        return behov.satsResultat != null && hasPendingTask(Regel.SATS, behov.behovId)
    }

    fun hasPendingTask(regel: Regel, behovId: String): Boolean {
        val task = tasks.getTask(regel, behovId)
        return task?.status == TaskStatus.PENDING
    }

    fun storeResult(behov: SubsumsjonsBehov) {
        when {
            hasNeededMinsteinntektResultat(behov) -> storeMinsteinntektSubsumsjon(behov)
            hasNeededPeriodeResultat(behov) -> storePeriodeSubsumsjon(behov)
            hasNeededGrunnlagResultat(behov) -> storeGrunnlagSubsumsjon(behov)
            hasNeededSatsResultat(behov) -> storeSatsSubsumsjon(behov)
            else -> LOGGER.info("Ignoring behov with id ${behov.behovId}")
        }
    }

    fun storeMinsteinntektSubsumsjon(behov: SubsumsjonsBehov) {
        val minsteinntektSubsumsjon = mapToMinsteinntektSubsumsjon(behov)

        minsteinntektSubsumsjoner.insertMinsteinntektSubsumsjon(minsteinntektSubsumsjon)
        tasks.updateTask(Regel.MINSTEINNTEKT, behov.behovId, minsteinntektSubsumsjon.subsumsjonsId)
    }

    fun storePeriodeSubsumsjon(behov: SubsumsjonsBehov) {
        val periodeSubsumsjon = mapToPeriodeSubsumsjon(behov)

        periodeSubsumsjoner.insertPeriodeSubsumsjon(periodeSubsumsjon)
        tasks.updateTask(Regel.PERIODE, behov.behovId, periodeSubsumsjon.subsumsjonsId)
    }

    fun storeGrunnlagSubsumsjon(behov: SubsumsjonsBehov) {
        val grunnlagSubsumsjon = mapToGrunnlagSubsumsjon(behov)

        grunnlagSubsumsjoner.insertGrunnlagSubsumsjon(grunnlagSubsumsjon)
        tasks.updateTask(Regel.GRUNNLAG, behov.behovId, grunnlagSubsumsjon.subsumsjonsId)
    }

    fun storeSatsSubsumsjon(behov: SubsumsjonsBehov) {
        val satsSubsumsjon = mapToSatsSubsumsjon(behov)

        satsSubsumsjoner.insertSatsSubsumsjon(satsSubsumsjon)
        tasks.updateTask(Regel.SATS, behov.behovId, satsSubsumsjon.subsumsjonsId)
    }

    fun mapToMinsteinntektSubsumsjon(behov: SubsumsjonsBehov): MinsteinntektSubsumsjon {
        val minsteinntektResultat = behov.minsteinntektResultat!!
        val inntektString = behov.inntektV1!!
        val inntekt = inntektAdapter.fromJson(inntektString) // TODO ADAPT TO PACKET
        return MinsteinntektSubsumsjon(
            minsteinntektResultat.subsumsjonsId,
            LocalDateTime.now(),
            LocalDateTime.now(),
            MinsteinntektFaktum(
                behov.aktørId,
                behov.vedtakId,
                behov.beregningsDato,
                inntekt?.inntektsId ?: "12345", // fixme
                behov.harAvtjentVerneplikt),
            MinsteinntektResultat(minsteinntektResultat.oppfyllerMinsteinntekt),
                behov.minsteinntektInntektsPerioder!!
        )
    }

    fun mapToPeriodeSubsumsjon(behov: SubsumsjonsBehov): PeriodeSubsumsjon {
        val periodeResultat = behov.periodeResultat!!
        val inntektString = behov.inntektV1!!
        val inntekt = inntektAdapter.fromJson(inntektString) // TODO ADAPT TO PACKET
        return PeriodeSubsumsjon(
            periodeResultat.subsumsjonsId,
            LocalDateTime.now(),
            LocalDateTime.now(),
            PeriodeFaktum(
                behov.aktørId,
                behov.vedtakId,
                behov.beregningsDato,
                inntekt?.inntektsId ?: "12345", // fixme
                behov.harAvtjentVerneplikt),
            PeriodeResultat(periodeResultat.periodeAntallUker)
        )
    }

    fun mapToGrunnlagSubsumsjon(behov: SubsumsjonsBehov): GrunnlagSubsumsjon {
        val grunnlagResultat = behov.grunnlagResultat!!
        val inntektString = behov.inntektV1!!
        val inntekt = inntektAdapter.fromJson(inntektString) // TODO ADAPT TO PACKET
        return GrunnlagSubsumsjon(
            grunnlagResultat.subsumsjonsId,
            LocalDateTime.now(),
            LocalDateTime.now(),
            GrunnlagFaktum(
                behov.aktørId,
                behov.vedtakId,
                behov.beregningsDato,
                inntekt?.inntektsId ?: "12345", // fixme
                behov.harAvtjentVerneplikt),
            GrunnlagResultat(grunnlagResultat.avkortet, grunnlagResultat.uavkortet),
            setOf(
                InntektResponse(
                    inntekt = BigDecimal.ZERO,
                    periode = 1,
                    inntektsPeriode = InntektsPeriode(YearMonth.of(2018, 2), YearMonth.of(2019, 1)),
                    inneholderFangstOgFisk = false,
                    andel = BigDecimal.ZERO
                ),
                InntektResponse(
                    inntekt = BigDecimal.ZERO,
                    periode = 2,
                    inntektsPeriode = InntektsPeriode(YearMonth.of(2017, 2), YearMonth.of(2018, 1)),
                    inneholderFangstOgFisk = false,
                    andel = BigDecimal.ZERO
                ),
                InntektResponse(
                    inntekt = BigDecimal.ZERO,
                    periode = 3,
                    inntektsPeriode = InntektsPeriode(YearMonth.of(2016, 2), YearMonth.of(2017, 1)),
                    inneholderFangstOgFisk = false,
                    andel = BigDecimal.ZERO
                )
            )
        )
    }

    val inntektAdapter = moshiInstance.adapter<Inntekt>(Inntekt::class.java)

    fun mapToSatsSubsumsjon(behov: SubsumsjonsBehov): SatsSubsumsjon {
        val satsResultat = behov.satsResultat!!
        val grunnlag = behov.grunnlag!!
        return SatsSubsumsjon(
            satsResultat.subsumsjonsId,
            LocalDateTime.now(),
            LocalDateTime.now(),
            SatsFaktum(behov.aktørId, behov.vedtakId, behov.beregningsDato, grunnlag, behov.antallBarn),
            SatsResultat(satsResultat.dagsats, satsResultat.ukesats)
        )
    }
}