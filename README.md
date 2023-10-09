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

For utfyllende dokumentasjon se [Postgres i NAV](https://github.com/navikt/utvikling/blob/main/docs/teknisk/PostgreSQL.md)

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
Tilkobling via IntelliJ:
```
View -> ToolWindows -> Database
New -> Datasource -> PostgresSQL

Host: dev-pg.intern.nav.no eller prod-pg.intern.nav.no for hhv test og prod
Database: dp-regel-api
Bruk brukernavn og passord fra "vault read" kommandoen over

```


Tilkobling via kommandolinja:

```

psql -d $DATABASE_NAME -h $DATABASE_HOST -U $GENERERT_BRUKER_NAVN

```

###Spørringer:
Finne behovene og aktørId for en gitt subsumsjonsId:
```SQL
select
    *
from v2_subsumsjon
where data -> 'minsteinntektResultat' ->> 'subsumsjonsId' = 'subsumsjonsid'
OR data -> 'satsResultat' ->> 'subsumsjonsId' = 'subsumsjonsid'
OR data -> 'periodeResultat' ->> 'subsumsjonsId' = 'subsumsjonsid'
OR data -> 'grunnlagResultat' ->> 'subsumsjonsId' = 'subsumsjonsid';
```

Finne sats, grunnlag og hvilke grunnbeløp brukt gitt en aktør
```SQL
select
data -> 'satsResultat' ->> 'dagsats' as dagsats,
data -> 'faktum' ->> 'beregningsdato' as beregningsdato,
data -> 'faktum' ->> 'regelverksdato' as regelverksdato,
data -> 'faktum' ->> 'manueltGrunnlag' as manueltgrunnlag,
data -> 'grunnlagResultat' as grunnlag,
brukt
from v2_subsumsjon where behov_id in (
select id from v2_behov where aktor_id = 'aktørId');
```
