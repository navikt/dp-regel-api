# AGENTS.md — navikt/dp-regel-api

## Repository Overview

API-tjeneste for kjøring av dagpengeregler (minsteinntekt, periode, sats, grunnlag).
Mottar behov via REST API, publiserer dem til Kafka der spesialiserte regel-tjenester
beregner subsumsjoner, og lagrer resultatene i Postgres. Brukes av Arena via
dp-regel-api-arena-adapter. Bygget med Ktor, sikret med Azure AD JWT.

## Tech Stack

- Kotlin
- Dockerfile

## Build & Test Commands

```bash
./gradlew build  # Build
./gradlew test   # Run tests
```

## Code Standards

- Follow Kotlin coding conventions
- Use sealed classes for environment configuration (Dev/Prod/Local)
- Use Kotliquery with HikariCP for database access
- ApplicationBuilder pattern for bootstrapping
- Write tests for all public APIs

## Ktor Patterns

- Use `Application.module()` extension functions for feature installation
- Route definitions via `routing { }` DSL
- Use `testApplication { }` for integration tests
- Flyway for database migrations
- Rapids & Rivers pattern for Kafka event handling

## Boundaries

### ✅ Always

- Follow existing code patterns
- Run tests before committing
- Use parameterized queries for database access
- Use sealed classes for environment configuration

### ⚠️ Ask First

- Changing authentication mechanisms
- Modifying production configurations
- Adding new dependencies

### 🚫 Never

- Commit secrets or credentials to git
- Skip input validation
- Bypass security controls
