# GitHub Copilot Review Instructions — dp-regel-api

Bruk disse sjekkpunktene ved gjennomgang av pull requests i `dp-regel-api`.

## Sikkerhet og personvern

- [ ] Logger koden PII (fnr, aktørId, navn, adresse)? → **Avvis** — logg kun IDer som `behovId`
- [ ] Finnes ikke-parameteriserte SQL-spørringer (strengkonkatenering)? → **Avvis** — SQL-injeksjon-risiko
- [ ] Inneholder koden hardkodede secrets, passord eller API-nøkler? → **Avvis**
- [ ] Valideres all inndata fra HTTP-requester med `require(...)` eller tilsvarende?
- [ ] Er nye endepunkter lagt innenfor `authenticate("jwt") { ... }`?
  - Unntak: `/isAlive`, `/isReady`, `/metrics` skal **ikke** ha auth

## Autentisering

- [ ] Bruker koden Azure AD JWT slik det er satt opp i `AzureAdJWT.kt`?
- [ ] Er det lagt til nye auth-mekanismer (TokenX, ID-porten) uten avklaring med teamet?

## Databasetilgang

- [ ] Brukes Kotliquery med `using(sessionOf(dataSource)) { ... }`?
- [ ] Er alle SQL-spørringer parameteriserte?
- [ ] Er nye Flyway-migrasjoner lagt i `src/main/resources/db/migration/` med riktig navneformat (`V{n}__{beskrivelse}.sql`)?
- [ ] Er migrasjoner bakoverkompatible (ingen DROP uten vurdering)?

## Kafka

- [ ] Sendes behov kun via `DagpengerBehovProducer`-interfacet?
- [ ] Endres topic-navn uten avklaring? (`teamdagpenger.regel.v1`, `teamdagpenger.subsumsjonbrukt.v1`)
- [ ] Håndteres deserialiserings-feil i consumers gracefully?

## Ktor-mønstre

- [ ] Følger nye ruter mønsteret med extension-funksjoner på `Route`?
- [ ] Er feilhåndtering lagt til `StatusPages` fremfor try/catch i rute-handlere?
- [ ] Brukes `jacksonObjectMapper` fra `serder`-pakken konsekvent?

## Testdekning

- [ ] Har nye endepunkter tester med `TestApplication.testApp { }` og `autentisert(...)`?
- [ ] Testes 401-responsen for alle nye beskyttede endepunkter?
- [ ] Brukes `MockK` for mocking og `Kotest`-assertions (`shouldBe`, `shouldContain`)?
- [ ] Er database-integrasjonstester basert på `PostgresTestSetup` med Testcontainers?

## Kodekvalitet

- [ ] Brukes `!!` (not-null assertion) uten god grunn? → Forklar eller endre til `?: error(...)`
- [ ] Er nye klasser og funksjoner som ikke skal eksponeres merket `internal`?
- [ ] Brukes ULID (ikke UUID) for nye entitets-IDer?
- [ ] Er koden konsistent med eksisterende mønstre i `routing/`, `db/` og `streams/`?

## Observabilitet

- [ ] Finnes det meningsfulle log-meldinger for viktige tilstandsendringer?
- [ ] Er nye `catch`-blokker uteglemt logging?

## Nais-konfigurasjon (ved endringer i `nais/`)

- [ ] Er `accessPolicy.inbound` oppdatert hvis nye konsumenter legges til?
- [ ] Er det lagt til CPU-limits? → **Fjern** — kun `requests`, ikke `limits` for CPU
- [ ] Er ressurser (`memory`, `cpu`) innenfor rimelige grenser for tjenestens størrelse?
