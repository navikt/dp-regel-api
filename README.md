# dp-regel-api

REST API for kjøring av dagpengeregler (minsteinntekt, periode, sats, grunnlag).
Mottatt behov publiseres til Kafka, regelberegnings-tjenester produserer subsumsjoner,
og resultatene lagres i PostgreSQL. Brukes av Arena via `dp-regel-api-arena-adapter`.

📖 **[Les full systemdokumentasjon i docs/README.md](docs/README.md)**

## Kom i gang

### Krav

- JDK 21+
- Docker (kreves for integrasjonstester med Testcontainers)

### Bygg og test

```bash
./gradlew build   # Kompiler og kjør alle tester
./gradlew test    # Kjør kun tester
```

### Lokal kjøring

Start avhengigheter med Docker Compose:

```bash
docker-compose up -d
```

Kjør applikasjonen:

```bash
./gradlew run
```

Tjenesten starter på port `8092`.

## Teknisk stack

| Komponent | Teknologi |
|-----------|-----------|
| Rammeverk | Ktor (Netty) |
| Autentisering | Azure AD JWT |
| Database | PostgreSQL via Kotliquery + Flyway |
| Meldingskø | Kafka (kafka-clients + kafka-streams) |
| Metrikker | Micrometer + Prometheus |
| Testing | JUnit 5, Kotest, MockK, Testcontainers, mock-oauth2-server |

## Prosjektstruktur

```
src/main/kotlin/no/nav/dagpenger/regel/api/
├── RegelApi.kt                  # Applikasjonsoppstart og Ktor-modulen
├── Configuration.kt             # Miljøkonfigurasjon (Konfig)
├── KafkaConfig.kt               # Kafka-oppsett
├── auth/
│   └── AzureAdJWT.kt            # JWT-validering mot Azure AD
├── db/
│   ├── PostgresDataSourceBuilder.kt  # HikariCP + Flyway
│   ├── PostgresSubsumsjonStore.kt    # Lese/skrive subsumsjoner
│   └── PostgresBruktSubsumsjonStore.kt
├── models/
│   ├── Behov.kt                 # Domeneobjekter for behov
│   ├── Subsumsjon.kt            # Domeneobjekter for subsumsjon
│   └── Faktum.kt                # Inndatafaktum
├── routing/
│   ├── BehovRoute.kt            # POST /behov, GET /behov/status/{id}
│   ├── SubsumsjonRoute.kt       # GET /subsumsjon/{id}
│   ├── LovverkRoute.kt          # POST /lovverk/vurdering/minsteinntekt
│   ├── NaisChecks.kt            # /isAlive, /isReady
│   └── Prometheus.kt            # /metrics
└── streams/
    ├── KafkaDagpengerBehovProducer.kt
    ├── AivenKafkaSubsumsjonConsumer.kt
    └── KafkaSubsumsjonBruktConsumer.kt

src/main/resources/db/migration/  # Flyway SQL-migrasjoner (V1–V18)
nais/dev/nais.yaml                # Nais-manifest for dev-gcp
nais/prod/nais.yaml               # Nais-manifest for prod-gcp
```

## Databasetilgang

Se [NAIS: Personlig databasetilgang](https://docs.nais.io/how-to-guides/persistence/postgres/#personal-database-access)

Nyttige SQL-spørringer finnes i [docs/README.md](docs/README.md#nyttige-sql-spørringer).
