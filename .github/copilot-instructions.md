# GitHub Copilot Instructions — dp-regel-api

## Tjenestebeskrivelse

REST API for kjøring av dagpengeregler (minsteinntekt, periode, sats, grunnlag).
Mottar behov via REST, sender dem til Kafka, og lagrer subsumsjoner fra spesialiserte
regel-tjenester i PostgreSQL. Eneste konsument er `dp-regel-api-arena-adapter`.

## Språk og konvensjoner

- Skriv all kode i **Kotlin**
- Følg Kotlin coding conventions (offisiell stilguide)
- Bruk `internal` for klasser og funksjoner som ikke skal eksponeres utenfor modulen
- Unngå `!!` (not-null assertion) — bruk `?: error(...)` eller `requireNotNull(...)`

## Autentisering

Tjenesten bruker **Azure AD JWT** via Ktor sin `jwt`-plugin:

```kotlin
install(Authentication) {
    jwt(name = "jwt") {
        azureAdJWT(
            providerUrl = config.azureAppWellKnownUrl,
            realm = config.id,
            clientId = config.azureAppClientId,
        )
    }
}
```

- Alle forretningsruter skal ligge innenfor `authenticate("jwt") { ... }`
- `/isAlive`, `/isReady` og `/metrics` er **alltid åpne** (ingen auth)
- Ikke bruk TokenX eller ID-porten — kun Azure AD client_credentials

## Ktor-mønstre

Bruk `Application.module()` extension-funksjoner for feature-installasjon:

```kotlin
internal fun Application.api(
    subsumsjonStore: SubsumsjonStore,
    kafkaProducer: DagpengerBehovProducer,
    healthChecks: List<HealthCheck>,
    config: Configuration,
) {
    install(DefaultHeaders)
    install(Authentication) { ... }
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(jacksonObjectMapper)) }
    install(MicrometerMetrics) { registry = meterRegistry }
    install(StatusPages) { ... }

    routing {
        naischecks(healthChecks)
        metrics(meterRegistry)
        authenticate("jwt") {
            subsumsjon(subsumsjonStore)
            lovverk(subsumsjonStore, kafkaProducer)
            behov(subsumsjonStore, kafkaProducer)
        }
    }
}
```

Definer ruter som extension-funksjoner på `Route`:

```kotlin
internal fun Route.subsumsjon(store: SubsumsjonStore) {
    get("/subsumsjon/{id}") { ... }
}
```

## Konfigurasjon

Bruk `Configuration`-singletonen med Konfig-biblioteket:

```kotlin
internal object Configuration {
    val config: Configuration = systemProperties() overriding EnvironmentVariables overriding defaultProperties
    val azureAppClientId: String by lazy { config[Key("azure.app.client.id", stringType)] }
}
```

- Legg alltid til fornuftige standardverdier i `defaultProperties` for lokal kjøring
- Bruk `lazy` for verdier som leser fra miljøvariabler ved oppstart

## Database

Bruk `PostgresDataSourceBuilder` og Kotliquery. **Aldri** skriv rå JDBC.

```kotlin
using(sessionOf(dataSource)) { session ->
    session.run(
        queryOf("SELECT ... WHERE id = ?", id)
            .map { row -> row.string("kolonne") }
            .asSingle,
    )
}
```

- Bruk alltid parameteriserte spørringer — aldri strengkonkatenering i SQL
- Flyway-migrasjoner legges i `src/main/resources/db/migration/` med navn `V{n}__{beskrivelse}.sql`
- HikariCP er konfigurert med `maximumPoolSize = 10` — ikke endre uten god grunn

## Feilhåndtering i Ktor

Legg domenespesifikke exceptions i `StatusPages`:

```kotlin
install(StatusPages) {
    exception<SubsumsjonNotFoundException> { call, _ ->
        call.respond(HttpStatusCode.NotFound)
    }
    exception<IllegalUlidException> { call, _ ->
        call.respond(HttpStatusCode.BadRequest)
    }
}
```

Logg med `warn` for forventede feil, `error` for uventede.

## ID-format

Bruk **ULID** (ikke UUID) for alle entitets-IDer:

```kotlin
import de.huxhorn.sulky.ulid.ULID
val id = ULID().nextULID()
```

## Kafka

- Producer: `KafkaDagpengerBehovProducer` sender behov til `teamdagpenger.regel.v1`
- Consumer: `AivenKafkaSubsumsjonConsumer` leser subsumsjoner fra `teamdagpenger.regel.v1`
- Brukt-consumer: `KafkaSubsumsjonBruktConsumer` leser fra `teamdagpenger.subsumsjonbrukt.v1`
- Ikke bruk Rapids & Rivers-rammeverket — tjenesten bruker eget `SubsumsjonPond`-mønster

## Logging

Bruk `kotlin-logging` (wrapper rundt SLF4J/Logback):

```kotlin
private val logger = KotlinLogging.logger {}
logger.info { "Behandler behov $behovId" }
```

- **Aldri** logg `fnr`, `aktørId`, navn eller annen PII
- Logg heller `behovId`, `subsumsjonId` eller andre ikke-sensitive IDer

## Observabilitet

- Micrometer + Prometheus er allerede satt opp via `MicrometerMetrics`-plugin
- Metrikkendepunkt: `GET /metrics`
- Health: `GET /isAlive` og `GET /isReady`
- Ikke legg til CPU-limits i Nais-manifest — kun `requests`

## Sikkerhet

- Aldri commit secrets eller credentials
- Alle hemmeligheter hentes fra miljøvariabler (via Konfig) eller Nais secrets
- Valider alltid inndata — bruk `require(...)` og `requireNotNull(...)`
- Eneste tillatte inbound-klient er `dp-regel-api-arena-adapter`
