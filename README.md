# dagpenger-regel-api


API for å kjøre dagpengeregler

## Utvikling av applikasjonen

For å kjøre enkelte av testene kreves det at Docker kjører.

[Docker Desktop](https://www.docker.com/products/docker-desktop)


### Starte applikasjonen lokalt

Applikasjonen har avhengigheter til Kafka og Postgres som kan kjøres
opp lokalt vha Docker Compose(som følger med Docker Desktop)


Starte Kafka og Postgres:
```

docker-compose -f docker-compose.yml up

```
Etter at containerene er startet kan man starte applikasjonen ved å kjøre main metoden.


Stoppe Kafka og Postgres:

```
ctrl-c og docker-compose -f docker-compose.yml down

```

### Tilgang til Postgres databasen

For utfyllende dokumentasjon se [Postgres i NAV](https://github.com/navikt/utvikling/blob/master/PostgreSQL.md)

#### Tldr

Applikasjonen benytter seg av dynamisk genererte bruker/passord til database.
For å koble seg til databasen må man genere bruker/passord(som varer i en time)
på følgende måte:

Installere [Vault](https://www.vaultproject.io/downloads.html)

Generere bruker/passord:

```

export VAULT_ADDR=https://vault.adeo.no USER=NAV_IDENT
vault login -method=oidc


```

Preprod credentials:

```
vault read postgresql/preprod-fss/creds/dp-regel-api-preprod-admin

```

Prod credentials:

```
vault read postgresql/prod-fss/creds/dp-regel-api-admin

```

Bruker/passord kombinasjonen kan brukes til å koble seg til de aktuelle databasene(Fra utvikler image...)
F.eks

```

psql -d $DATABASE_NAME -h $DATABASE_HOST -U $GENERERT_BRUKER_NAVN

```

## Hvordan jobbe med API spesifikasjonen

### Install

1. Install [Node JS](https://nodejs.org/)
2. Clone repo and run `npm install` in the repo root

### Usage

#### `npm start`
Starts the development server.

#### `npm run build`
Bundles the spec and prepares web_deploy folder with static assets.

#### `npm test`
Validates the spec.

#### `npm run gh-pages`
Deploys docs to GitHub Pages. You don't need to run it manually if you have Travis CI configured.
